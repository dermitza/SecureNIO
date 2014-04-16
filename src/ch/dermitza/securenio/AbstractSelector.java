/**
 * This file is part of SecureNIO. Copyright (C) 2014 K. Dermitzakis
 * <dermitza@gmail.com>
 *
 * SecureNIO is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SecureNIO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SecureNIO. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.dermitza.securenio;

import ch.dermitza.securenio.packet.PacketListener;
import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.socket.SocketContainer;
import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.socket.secure.HandshakeListener;
import ch.dermitza.securenio.socket.secure.TaskListener;
import ch.dermitza.securenio.socket.secure.TaskWorker;
import ch.dermitza.securenio.socket.timeout.TimeoutListener;
import ch.dermitza.securenio.socket.timeout.worker.TimeoutWorker;
import ch.dermitza.securenio.util.PropertiesReader;
import ch.dermitza.securenio.util.logging.LoggerHandler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * An abstract selector implementation that can be the basis (can be extended)
 * of an NIO TCP server or client, supporting both plain and SSL/TLS encrypted
 * sockets.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public abstract class AbstractSelector implements Runnable, TaskListener, HandshakeListener, TimeoutListener {

    /**
     *
     */
    protected static final Logger logger = LoggerHandler.getLogger(AbstractSelector.class.getName());
    /**
     * The address to listen at or connect
     */
    protected InetAddress address;
    /**
     * The port to listen on or connect to
     */
    protected int port;
    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final ArrayDeque<ChangeRequest> pendingChanges = new ArrayDeque<>();
    /**
     * Maps a SocketChannel to a list of ByteBuffer instances
     */
    protected final HashMap<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();
    /**
     * The selector we'll be monitoring
     */
    protected Selector selector;
    private boolean running = false;
    private boolean processAll = true;
    /**
     * Whether we are using SSL/TLS
     */
    protected final boolean usingSSL;
    /**
     * The associated SSLContext
     */
    protected SSLContext context = null;
    /**
     * A SocketContainer to hold active socket instances
     */
    protected final SocketContainer container = new SocketContainer();
    private AbstractPacketWorker packetWorker;
    /**
     * The underlying TaskWorker
     */
    protected final TaskWorker taskWorker;
    /**
     * The underlying TimeoutWorker
     */
    protected final TimeoutWorker toWorker;
    /**
     *
     */
    protected String[] protocols;
    /**
     *
     */
    protected String[] cipherSuits;
    /**
     * Whether this AbstractSelector is a client
     */
    protected final boolean isClient;
    /**
     * Whether this AbstractSelector needs clientAuth
     */
    protected final boolean needClientAuth;
    /**
     * Whether the SSLEngine tasks required run in the AbstractSelector thread
     */
    protected final boolean singleThreaded;

    /**
     * Create a new AbstractSelector instance.
     *
     * @param address The address this selector will use
     * @param port The port this selector will use
     * @param packetWorker The instance of packet worker to use
     * @param usingSSL Whether we are using SSL/TLS
     * {@link ch.dermitza.securenio.socket.secure.TaskWorker} thread.
     * @param isClient If the current Selector implementation is a client
     * implementation (false indicates it is a server implementation).
     * @param needClientAuth If the current implementation is a server
     * implementation, whether the client should also verify its authenticity
     * (i.e. sets up SSLEngine.setNeedClientAuth(true)).
     */
    public AbstractSelector(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean isClient, boolean needClientAuth) {
        this.address = address;
        this.port = port;
        this.singleThreaded = PropertiesReader.getSelectorSingleThreaded();
        this.processAll = PropertiesReader.getSelectorProcessAll();
        this.usingSSL = usingSSL;
        this.isClient = isClient;
        this.needClientAuth = needClientAuth;
        this.packetWorker = packetWorker;
        this.taskWorker = (singleThreaded)?null:new TaskWorker(this);
        //this.taskWorker = new TaskWorker(this);
        this.toWorker = new TimeoutWorker();
        logger.log(Level.FINE, "Using ssl: {0}", usingSSL);
    }

    /**
     * If the server/client has been initialized to use SSL/TLS, this method is
     * used to setup and initialize the SSL/TLS required parameters.
     *
     * @param trustStoreLoc The location of the trustStore on the disk. The
     * trustore is *ALWAYS* required for a client implementation. For a server
     * implementation it is only required if needClientAuth is true, i.e. IFF
     * the client should also verify its authenticity; it can be null otherwise.
     * @param keyStoreLoc The location of the keyStore on the disk. The keystore
     * is *ALWAYS* required for a server implementation. For a client
     * implementation it is only required if needClientAuth is true, i.e. IFF
     * the client should also verify its authenticity; it can be null otherwise.
     * @param tsPassPhrase The passphrase to use with the trustStore or null if
     * the truststore is not required.
     * @param ksPassPhrase The passphrase to use with the keyStore or null if
     * the keystore is not required.
     * @param protocolsLoc The location of the file containing the SSL/TLS
     * protocols to be used by the client/server. WARNING: If null is passed, or
     * the file is not found, the default SSL/TLS protocols will be used
     * instead, as per SSLEngine.getEnabledProtocols().
     * @param cipherSuitesLoc The location of the file containing the SSL/TLS
     * cipher suites to be used by the client/server. WARNING: If null is
     * passed, or the file is not found, the default SSL/TLS cipher suites will
     * be used instead, as per SSLEngine.getEnabledCipherSuites().
     */
    public void setupSSL(String trustStoreLoc, String keyStoreLoc,
            char[] tsPassPhrase, char[] ksPassPhrase, String protocolsLoc,
            String cipherSuitesLoc) {
        if (!usingSSL) {
            logger.log(Level.WARNING, "Trying to set SSL parameters with a "
                    + "non-SSL/TLS {0}" + ". SSL/TLS was NOT set or initialized.",
                    (isClient ? "client" : "server"));
            return;
        }

        protocols = PropertiesReader.getProtocols();
        cipherSuits = PropertiesReader.getCipherSuites();
                
        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;
        KeyStore ks = null;
        FileInputStream fis = null;

        if (isClient || (!isClient && needClientAuth)) {
            // Need to initialize truststores
            try {
                tmf = TrustManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS");
                fis = new FileInputStream(trustStoreLoc);
                ks.load(fis, tsPassPhrase);
                tmf.init(ks);
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
                // tmf
            } catch (KeyStoreException kse) {
                kse.printStackTrace();
                // ks, tmf.init()
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                // fis
            } catch (IOException ioe) {
                ioe.printStackTrace();
                // ks.load()
            } catch (CertificateException ce) {
                ce.printStackTrace();
                // ks.load()
            } finally {
                try {
                    fis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    // fis.close();
                }
            }
        }

        if (!isClient || (isClient && needClientAuth)) {
            // Need to initialize keystores
            try {
                kmf = KeyManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS");
                fis = new FileInputStream(keyStoreLoc);
                ks.load(fis, ksPassPhrase);
                kmf.init(ks, ksPassPhrase);
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
                // kmf
            } catch (KeyStoreException kse) {
                kse.printStackTrace();
                // kmf.init()
            } catch (UnrecoverableKeyException uke) {
                uke.printStackTrace();
                // kmf.init()
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                // fis, fis.close()
            } catch (IOException ioe) {
                ioe.printStackTrace();
                // ks.load()
            } catch (CertificateException ce) {
                ce.printStackTrace();
                // ks.load()
            } finally {
                try {
                    fis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    // fis.close();
                }
            }
        }
        // Finally, initialize the context
        try {
            context = SSLContext.getInstance("TLS");
            if (kmf == null) {
                context.init(null, tmf.getTrustManagers(), null);
            } else if (tmf == null) {
                context.init(kmf.getKeyManagers(), null, null);
            } else {
                context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            }
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            // context
        } catch (KeyManagementException kme) {
            kme.printStackTrace();
            // context.init()
        }
    }

    /**
     * Sets up the underlying {@link SSLEngine} to be used with a
     * {@link ch.dermitza.securenio.socket.secure.SecureSocket} implementation.
     * The SSLEngine is initialized based on whether this instance is a server
     * or a client, whether we need clientAuth or not, and based on the provided
     * protocols and cipher suites provided in {@link
     * #setupSSL(String trustStoreLoc, String keyStoreLoc, char[] tsPassPhrase,
     * char[] ksPassPhrase, String protocolsLoc, String cipherSuitesLoc)}. The
     * peerHost and peerPort parameters are passed as hints to the
     * {@link SSLEngine} for engine re-usage purposes but can also be null.
     *
     * @param peerHost The peer host of the socket
     * @param peerPort The peer port of the socket
     * @return An initialized and configured SSLEngine ready to be used
     */
    protected SSLEngine setupEngine(String peerHost, int peerPort) {
        SSLEngine engine = context.createSSLEngine(peerHost, peerPort);
        engine.setUseClientMode(isClient);
        engine.setNeedClientAuth(needClientAuth);
        // Setup protocols and suites
        try {
            engine.setEnabledProtocols(protocols);
        } catch (IllegalArgumentException iae) {
            logger.log(Level.WARNING, "Provided protocols invalid, using default", iae);
        }

        try {
            engine.setEnabledCipherSuites(cipherSuits);
        } catch (IllegalArgumentException iae) {
            logger.log(Level.WARNING, "Provided cipher suites invalid, using default", iae);
        }
        return engine;
    }

    /**
     * Initializes the selector, the worker threads, and initiates a select
     * procedure that can be either server or client based. The select operation
     * can be interrupted when there are pending tasks to be completed, which
     * happens via the {@link #processChanges()} method. Upon shutdown, a
     * best-effort attempt is made to shutdown cleanly via the
     * {@link #shutdown()} method.
     *
     * @see #processChanges()
     * @see #shutdown()
     */
    @Override
    public void run() {
        try {
            // Initialize the selector
            selector = SelectorProvider.provider().openSelector();
            // Now init the connection
            // this is implementation specific (server or client wise)
            initConnection();
            new Thread(packetWorker, "PacketWorkerThread").start();
            new Thread(taskWorker, "TaskWorkerThread").start();
            new Thread(toWorker, "TimeoutWorkerThread").start();
            running = true;
        } catch (IOException ioe) {
            running = false;
            logger.log(Level.SEVERE, "Could not initialize selector, shutting down", ioe);
        }
        
        int keyNo;
        while (running) {
            try {
                // Process any pending changes
                processChanges();
                // Wait for an event on one of the registered channels
                keyNo = (processAll) ? selector.select() : selector.select(PropertiesReader.getSelectorTimeoutMS());

                if (keyNo > 0) {
                    // Iterate over the set of keys for which events are available
                    Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey key = selectedKeys.next();
                        selectedKeys.remove();

                        // Key could have been invalidated, if so
                        // just ignore it and go back to selecting
                        if (!key.isValid()) {
                            continue;
                        }

                        // Check what event is available and deal with it
                        // IMPORTANT: key can be A COMBINATION OF all states
                        // e.g. acceptable and connectable and readable etc.
                        // Elseifs would ignore the multistatus, use separate
                        // ifs instead
                        if (key.isAcceptable()) {
                            // Accept, we are inside a server
                            accept(key);
                        }
                        if (key.isConnectable()) {
                            // Connect, we are inside a client
                            connect(key);
                        }
                        if (key.isValid() && key.isReadable()) {
                            // Ready to read stuff
                            read(key);
                        }
                        if (key.isValid() && key.isWritable()) {
                            // Ready to write stuff
                            // however, application data will be consumed (destroyed)
                            // if we are still handshaking. In this case, we need
                            // to unregister the key for being writable until the
                            // handshake is complete
                            //if(container.getSocket(key.channel()).handshakePending()){
                                // need to flush
                            //    try{
                           //         container.getSocket(key.channel()).flush();
                           //     } catch(IOException ioe){
                           //         System.out.println("IOE while flushing");
                           //         closeSocket(container.getSocket(key.channel()));
                           //     }
                           // }else{
                                write(key);
                           // }
                        }
                    }
                }
            } catch (IOException ioe) {
                // Selector should NOT throw an IOException during select(),
                // if it does, something is really wrong, shutdown
                running = false;
                logger.log(Level.SEVERE, "IOException in select(), shutting down", ioe);
            }
        }
        shutdown();
    }

    /**
     * Invalidate the {@link javax.net.ssl.SSLSession} associated with the
     * provided {@link SocketIF}. The invalidation is queued as a pending change
     * where it is executed in a FIFO fashion along with any other pending
     * changes. This method could be periodically used via a
     * {@link ch.dermitza.securenio.socket.timeout.worker.Timeout} to perform
     * SSL/TLS session rotation if needed.
     *
     * @param socket The socket whose underlying
     * {@link javax.net.ssl.SSLSession} should be invalidates
     *
     * @see #processChanges()
     */
    protected void invalidateSession(SocketIF socket) {
        synchronized (this.pendingChanges) {
            socket.invalidateSession();
            pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_SESSION, 0));
            this.selector.wakeup();
        }
    }

    /**
     * Send an {@link ByteBuffer} over the specified {@link SocketIF}. This
     * method does not directly send the packets, but rather queues them for
     * sending as soon as possible. <p> Data are subsequently written by setting
     * the associated {@link SelectionKey} to SelectionKey.OP_WRITE. In case of
     * an SSL/TLS implementation and where the handshaking is not completed, the
     * SelectionKey is not changed until the handshake has finished.
     *
     * @param socket The SocketIF to send the packet through
     * @param data The ByteBuffer to send through the associated SocketIF
     *
     * @see #processChanges()
     */
    protected void send(SocketIF socket, ByteBuffer data) {
        synchronized (this.pendingChanges) {
            // If the handshake has been completed, indicate that we want the
            // interest ops changed to OP_WRITE. If the handshake is still
            // pending, a PendingChange will be issued when the handshake has
            // been completed.
            if (!socket.handshakePending()) {
                pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_OPS, SelectionKey.OP_WRITE));
            }

            // Queue the data we want written to the remote end. If a queue does
            // not yet exist, a new one will be created and the data will be 
            // added to it.
            synchronized (this.pendingData) {
                List<ByteBuffer> queue = this.pendingData.get(socket.getSocket());
                if (queue == null) {
                    queue = new ArrayList<>();
                    this.pendingData.put(socket.getSocket(), queue);
                }
                queue.add(data);
            }
        }

        // If the handshake has been previously completed, wake up our selecting
        // thread so it can make the required changes. If the handshake is still
        // pending, there are no changes to be made, so we can leave the selector
        // in the select() state until an actual change happens.
        if (!socket.handshakePending()) {
            this.selector.wakeup();
        }
    }

    /**
     * Any pending {@link ChangeRequest}s are processed via this method, in the
     * {@link AbstractSelector} thread. There is an option to process everything
     * at once, once this method is called, or to process a part of changes
     * queued. In the first case, care should be taken that the queued changes
     * are not so many that the main thread cannot return to the select() fast
     * enough to process a large number of connections or data reads. In the
     * second case, care should be taken that sufficiently many changes are
     * processed in time, such that the change queue does not grow too large
     * and/or remotely connected systems timeout (and potentially fail) due to
     * our inactivity to process changes fast enough.
     *
     * @param processAll Whether or not to process all changes in a single pass
     *
     * @see ChangeRequest
     */
    private void processChanges() {
        int changeCount = 0;
        synchronized (this.pendingChanges) {
            for (ChangeRequest change : this.pendingChanges) {
                SelectionKey key;
                switch (change.getType()) {
                    // The request concerns switching the interestOps of a key
                    // associated with a particular socket
                    case ChangeRequest.TYPE_OPS:
                        key = change.getChannel().getSocket().keyFor(selector);
                        // At this point we might get a CancelledKeyException if
                        // we are trying to set the interestOps on a key that has
                        // been previously been cancelled. Check if it is valid
                        // before changing the interestOps;
                        // It can also be the case that the the socket has already been
                        // unregistered with the selector, thus having a null key. In this
                        // case, we do not need to process anything regarding that socket
                        if (key != null && key.isValid()) {
                            key.interestOps(change.getOps());
                        }
                        break;
                    // The request concerns an SSLEngineTask that has just
                    // finished running on the TaskWorker thread
                    case ChangeRequest.TYPE_TASK:
                        // At this point, we need to resume processing the
                        // SSL/TLS handshake on the associated socket.
                        try {
                            // First update the result of the finished task
                            change.getChannel().updateResult();
                            // Then continue processing the handshake
                            // TODO, processHandshake can be merged with
                            // inithandshake
                            change.getChannel().processHandshake();
                        } catch (IOException ioe) {
                            // At this point, the handshake is NOT completed.
                            // Drop the socket.
                            logger.log(Level.INFO, "IOE after task", ioe);
                            closeSocket(change.getChannel());
                        }
                        break;
                    case ChangeRequest.TYPE_TIMEOUT:
                        // The timeout has expired on the given socket.
                        // As such, the socket needs to be closed
                        logger.config("Timeout expired");
                        closeSocket(change.getChannel());
                        break;
                    case ChangeRequest.TYPE_SESSION:
                        // The SSL/TLS session has been invalidated, we need to
                        // re-initiate handshaking
                        try {
                            change.getChannel().initHandshake();
                        } catch (IOException ioe) {
                            // At this point, the handshake is NOT completed.
                            // Drop the socket.
                            logger.log(Level.INFO, "IOE while initializing handshake", ioe);
                            closeSocket(change.getChannel());
                        }
                }
                // Remove the change we just processed
                pendingChanges.removeFirst();
                changeCount++;
                if (!processAll && changeCount >= PropertiesReader.getMaxChanges()) {
                    // processed the changes we were asked to. Break from the 
                    // loop only clearing the changes we just processed. The
                    // rest of changes will be processed in a subsequent iteration.
                    return;
                }
            }
            // All pending changes have been processed at this point
            // we are free to clear the list. NOTE: if the pending changes to
            // be processed are too many, this can cause the selecting thread
            // to start refusing connections. It could in this case be better
            // to not process everything at once, but rather process them one at
            // a time
            this.pendingChanges.clear();
        }
    }

    /**
     * Initialize a connection. This method is up to the server or client
     * implementations to implement, but at the minimum it should set
     * implemented channels to non-blocking, and set-up any SSL/TLS
     * configuration if necessary.
     *
     * @throws IOException Propagates all underlying IOExceptions as thrown, to
     * be handled by the application layer.
     *
     * @see AbstractSelector#run()
     */
    protected abstract void initConnection() throws IOException;

    /**
     * Should accept incoming connections and bind new non-blocking
     * {@link SocketIF} instances to them. If the server implementation is using
     * SSL/TLS, it should also set up the {@link SSLEngine}, to be used.
     *
     * @param key The selection key with the underlying {@link SocketChannel} to
     * be accepted
     *
     * @see AbstractSelector#run()
     */
    protected abstract void accept(SelectionKey key);

    /**
     * Should finish the connection to the server. This method should also
     * instantiate an SSLEngine handshake if the underlying {@link SocketIF} is
     * a secure socket. Finally, after the connection has been established, the
     * socket should be registered to the underlying {@link Selector}, with a
     * {@link SelectionKey} of OP_READ, signalling it is ready to read data.
     *
     * @param key The selection key with the underlying {@link SocketChannel}
     * that needs a connection finalization.
     */
    protected abstract void connect(SelectionKey key);

    /**
     * Writes data to the socket associated with the given {@link SelectionKey}.
     * This method is ONLY called once we have set the {@link SelectionKey}
     * associated with the socket to OP_WRITE. It tries to write as much data as
     * possible before returning. Once there is no more data to be written on
     * this socket, it sets the {@link SelectionKey} to OP_READ, disallowing any
     * further calls to this method until more data is available.
     *
     * @param key The SelectionKey whose associated socket we should write on
     *
     * @see #processChanges()
     * @see #send(ch.dermitza.securenio.socket.SocketIF, java.nio.ByteBuffer)
     */
    protected void write(SelectionKey key) {
        SocketIF socketChannel = container.getSocket(key.channel());

        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socketChannel.getSocket());

            // Write until there's not more data ...
            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                try {
                    int written = socketChannel.write(buf);
                    logger.log(Level.FINEST, "Written {0} bytes", written);
                } catch (IOException ioe) {
                    // If a IOE happens when writing, something really bad
                    // happened (generally). Close the socket where the write
                    // was happening
                    logger.log(Level.INFO, "IOE while writing", ioe);
                    closeSocket(socketChannel);
                    return;
                }
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.remove(0);
            }

            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    /**
     * Reads data from the socket associated with the given
     * {@link SelectionKey}. This method is called as soon as data is ready to
     * be read. It tries to read as much data as possible, handing bytes read to
     * the underlying {@link AbstractPacketWorker} for reconstruction and
     * further processing. It also handles potential socket disconnections
     * and/or errors, upon which, it makes a best effort to close the socket
     * cleanly.
     *
     * @param key The SelectionKey whose associated socket we should read from
     *
     * @see AbstractPacketWorker#addData(SocketIF, ByteBuffer, int)
     * @see #closeSocket(ch.dermitza.securenio.socket.SocketIF)
     */
    protected void read(SelectionKey key) {
        SocketIF socketChannel = container.getSocket(key.channel());
        // Clear out our read buffer so it's ready for new data
        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
            //numRead = socketChannel.read(this.readBuffer);
            logger.log(Level.FINEST, "Read {0} bytes", numRead);
        } catch (IOException ioe) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            // Closing the channel automatically cancels the key
            // TODO, recover the IP here
            logger.log(Level.INFO, "Remote forcibly disconnected", ioe);
            closeSocket(socketChannel);
            return;
        } catch (BufferOverflowException boe) {
            // Can be thrown during read from a secure socket
            // We would need to handle this, TODO
            logger.log(Level.INFO, "BufferOverflowException while reading", boe);
            closeSocket(socketChannel);
            return;
        }


        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            // Closing the channel automatically cancels the key
            // TODO, recover the IP here
            logger.config("Remote disconnected");
            closeSocket(socketChannel);
            return;
        }

        if (numRead > 0) {
            // Here we have a bytebuffer with some data on a SocketChannel
            // We need to construct a packet and then fire the listener methods
            // this happens in the worker thread
            packetWorker.addData(socketChannel, readBuffer, numRead);
        }
    }

    /**
     * Closes the given {@link SocketIF}. In doing that, it also removes any
     * potentially queued {@link ChangeRequest}s not yet processed on that
     * socket, and also removes the socket from the underlying
     * {@link SocketContainer}.
     *
     * TODO: Only pending data is being removed. A correct implementation would
     * also remove all other pendingChanges instances. Alternatively, we should
     * check for closed sockets when processing changes
     * ({@link #processChanges()}) and not perform any changes if the
     * socket is closed.
     *
     * @param socket The socket to close
     *
     * @see #processChanges()
     *
     */
    protected void closeSocket(SocketIF socket) {
        try {
            logger.log(Level.INFO, "Disconnecting remote: {0}", socket.getSocket().getRemoteAddress().toString());
        } catch (IOException ioe) {
            logger.log(Level.INFO, "IOE while obtaining remote IP", ioe);
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ioe) {
                logger.log(Level.INFO, "Closing socket failed", ioe);
                // We need to at least try to cancel the key here
                SelectionKey key = socket.getSocket().keyFor(selector);
                if (key != null) {
                    logger.finest("Cancelling registered key");
                    key.cancel();
                }
            } finally {
                // remove all pending bytebuffers registered to be sent through this
                // socket in pendingData
                synchronized (this.pendingData) {
                    List<ByteBuffer> queue = this.pendingData.get(socket.getSocket());
                    // clear the queue if it is not empty
                    if (queue != null && !queue.isEmpty()) {
                        queue.clear();
                    }
                    // remove the socket reference from pendingData
                    pendingData.remove(socket.getSocket());
                }
                // remove the reference from the container
                container.removeSocket(socket.getSocket());
                //logger.log(Level.INFO, "Disconnecting remote: " + reason);
            }
        }
    }

    //----------------------- RUNNABLE METHODS -------------------------------//
    /**
     * Check whether the {@link AbstractSelector} is running.
     *
     * @return true if it is running, false otherwise
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Set the running status of the {@link AbstractSelector}. If the running
     * status of the selector is set to false, the selector is interrupted in
     * order to cleanly shutdown by invoking {@link Selector#wakeup()}.
     *
     * @param running Whether the AbstractSelector should run or not
     */
    public void setRunning(boolean running) {
        this.running = running;
        // If a selector is already blocked in select()
        // and someone asked us to shutdown,
        // we should interrupt it so that it shuts down
        // after processing all possible pending requests
        if (!running) {
            selector.wakeup();
        }
    }

    /**
     * Shutdown procedure. This method is called if the {@link AbstractSelector}
     * was asked to shutdown; it cleanly process the shutdown procedure, cleanly
     * shutting down all associated worker threads, closing all open sockets,
     * and clearing any pending {@link ChangeRequest}s.
     */
    private void shutdown() {
        logger.config("Shutting down..");
        // Close the packetworker
        if(packetWorker.isRunning()){
            packetWorker.setRunning(false);
        }
        // Close the taskWorker
        if(!singleThreaded){
            if(taskWorker.isRunning()){
                taskWorker.setRunning(false);
            }
        }
        // Cancel all pending timeouts
        if(toWorker.isRunning()){
            toWorker.setRunning(false);
        }
        // Close all channels registered with the selector
        // This automatically invalidates the keys, so we dont
        // need to invalidate them ourselves
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            if (key.channel().isOpen()) {
                closeSocket(container.getSocket(key.channel()));
            }
        }
        // After all sockets are closed, clear pending changes.
        // Pending data associated with the sockets has already been invalidated
        // by the closeSocket() method
        synchronized (this.pendingChanges) {
            pendingChanges.clear();
        }
        // Close the selector too
        try {
            selector.close();
        } catch (IOException ioe) {
            // Ignore
        }
    }
    //----------------------- CHANGES METHODS -------------------------------//

    /**
     * Unused future clean-up implementation TODO
     *
     * @param changeRequest
     */
    private void queueChangeRequest(ChangeRequest changeRequest) {
        // Queue the ChangeRequest
        synchronized (this.pendingChanges) {
            pendingChanges.add(changeRequest);
        }
        // And wake up the selecting thread so it can make the required changes
        this.selector.wakeup();
    }

    /**
     * Unused future clean-up implementation TODO
     *
     * @param changeRequest
     */
    private boolean dataExists(SocketIF socket) {
        // Check if data exists for the socket supplied to us
        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socket.getSocket());
            if (queue == null || queue.isEmpty()) {
                // There is no data to be written, we do not need to register
                // for writing, we can just return.
                return false;
            } else {
                return true;
            }
        }
    }

    //----------------------- LISTENER METHODS -------------------------------//
    /**
     * The timeout on this socket has expired. We need to close the socket. As
     * this is called from a different thread, we need to queue a request for
     * closing the socket
     *
     * @param socket The socket of which its timeout has expired
     */
    @Override
    public void timeoutExpired(SocketIF socket) {
        synchronized (this.pendingChanges) {
            pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_TIMEOUT, 0));
        }
        // Finally, wake up our selecting thread so it can make the required changes
        this.selector.wakeup();
    }

    /**
     * A SSLEngine Task for a particular socket was completed by the TaskWorker.
     * As it is completed, the handshake on this particular socket is ready to
     * continue immediately. Since this method is called from the TaskWorker
     * thread, we need to queue a request for continuing to process the
     * handshake
     *
     * @param socket The socket of which an SSLEngine task was completed
     */
    @Override
    public void taskComplete(SocketIF socket) {
        // A SSLEngine task for a particular socket was completed by the
        // TaskWorker. As it is completed, the handshake on this particular
        // socket is ready to continue immediately. Since this method is called
        // from the TaskWorker thread, we need to queue a request for continuing
        // to process the handshake
        synchronized (this.pendingChanges) {
            pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_TASK, 0));
        }
        // Finally, wake up our selecting thread so it can make the required changes
        this.selector.wakeup();
    }
    
   // @Override
   // public void crap(SocketIF socket){
   //     synchronized (this.pendingChanges){
   //         pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_OPS, SelectionKey.OP_WRITE));
   //     }
   //     this.selector.wakeup();
   // }

    /**
     * A handshake is completed on this socket, as such the socket is ready to
     * be used (reading and writing). We need to see if there is data already
     * queued for the particular socket and if there is, to set the associated
     * key as writable.
     *
     * @param socket the socket of which its handshake is completed
     */
    @Override
    public void handshakeComplete(SocketIF socket) {
        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socket.getSocket());
            if (queue == null || queue.isEmpty()) {
                // There is no data to be written, we do not need to register
                // for writing, we can just return.
                return;
            } else {
                // Data exists, we need to register for writing
                synchronized (this.pendingChanges) {
                    pendingChanges.add(new ChangeRequest(socket, ChangeRequest.TYPE_OPS, SelectionKey.OP_WRITE));
                }
            }
        }

        // Finally, wake up our selecting thread so it can make the required changes
        this.selector.wakeup();
    }

    /**
     * Pass-through method to allow registration of multiple
     * {@link PacketListener}s to the underlying {@link AbstractPacketWorker}.
     *
     * @param listener The listener to register to the underlying PacketWorker
     */
    public void addListener(PacketListener listener) {
        packetWorker.addListener(listener);
    }

    /**
     * Pass-through method to allow de-registration of multiple
     * {@link PacketListener}s from the underlying {@link AbstractPacketWorker}.
     *
     * @param listener The listener to unregister from the underlying
     * PacketWorker
     */
    public void removeListener(PacketListener listener) {
        packetWorker.removeListener(listener);
    }
}
