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
package ch.dermitza.securenio.socket.secure;

import ch.dermitza.securenio.AbstractSelector;
import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.socket.timeout.TimeoutListener;
import ch.dermitza.securenio.socket.timeout.worker.Timeout;
import ch.dermitza.securenio.socket.timeout.worker.TimeoutWorker;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import static javax.net.ssl.SSLEngineResult.Status.BUFFER_OVERFLOW;
import static javax.net.ssl.SSLEngineResult.Status.BUFFER_UNDERFLOW;
import static javax.net.ssl.SSLEngineResult.Status.CLOSED;
import static javax.net.ssl.SSLEngineResult.Status.OK;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * A secure socket implementation of {@link SocketIF}. This class implements all
 * logic required to process SSL/TLS handshaking and encrypt/decrypt data being
 * sent and received through the underlying {@link SocketChannel}. <p> Note that
 * this class is declared as final as it should NOT be extended.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public final class SecureSocket implements SocketIF {

    private final SocketChannel sc;
    private final SSLEngine engine;
    // BUFFERS
    private final ByteBuffer encryptedIn;
    private final ByteBuffer encryptedOut;
    private final ByteBuffer decryptedIn;
    private final ByteBuffer decryptedOut;
    private volatile boolean handshakePending = true;
    private volatile boolean taskPending = false;
    private boolean singleThreaded = false;
    private SSLEngineResult result = null;
    private final HandshakeListener hsListener;
    private final TimeoutListener toListener;
    private final TaskWorker taskWorker;
    private final TimeoutWorker toWorker;
    private final Timeout timeout;

    /**
     * Create a new instance of a {@link SecureSocket}. This instance has all
     * the necessary logic to perform an SSL/TLS handshake and to encrypt and
     * decrypt data being sent and received through the underlying
     * {@link SocketChannel}.
     *
     * @param channel The underlying SocketChannel
     * @param engine The underlying SSLEngine
     * @param singleThreaded Whether or not this socket should perform the
     * {@link SSLEngineResult#HandshakeStatus} NEED_TASK in the same thread
     * (true) or in the {@link TaskWorker} thread (false). If set to true, null
     * can be passed for the {@link TaskWorker} instance.
     * @param taskWorker The {@link TaskWorker} instance associated with this
     * SecureSocket. Can be null if this socket performs the
     * {@link SSLEngineResult#HandshakeStatus} NEED_TASK in the same thread
     * @param toWorker The {@link TimeoutWorker} instance associated with this
     * SecureSocket.
     * @param hsListener The {@link HandshakeListener} associated with this
     * SecureSocket. Only one handshake listener is associated per socket, which
     * is usually the {@link AbstractSelector} implementation.
     * @param toListener The {@link TimeoutListener} associated with this
     * SecureSocket. Only one timeout listener is associated per socket, which
     * is usually the {@link AbstractSelector} implementation.
     */
    public SecureSocket(SocketChannel channel, SSLEngine engine,
            boolean singleThreaded, TaskWorker taskWorker,
            TimeoutWorker toWorker, HandshakeListener hsListener,
            TimeoutListener toListener) {
        this.sc = channel;
        this.engine = engine;
        this.singleThreaded = singleThreaded;
        this.taskWorker = taskWorker;
        this.hsListener = hsListener;
        // Timeouts
        this.toWorker = toWorker;
        this.toListener = toListener;
        timeout = new Timeout(this, toListener, Timeout.TIMEOUT_MS);

        int appBufSize = engine.getSession().getApplicationBufferSize();
        int netBufSize = engine.getSession().getPacketBufferSize();

        decryptedIn = ByteBuffer.allocate(appBufSize);
        decryptedOut = ByteBuffer.allocate(appBufSize);
        encryptedIn = ByteBuffer.allocate(netBufSize);
        encryptedOut = ByteBuffer.allocate(netBufSize);
    }

    /**
     * Get the associated underlying {@link SSLEngine}. This method is called
     * from the {@link TaskWorker} when there are pending SSLEngine tasks to be
     * run.
     *
     * @return the underlying {@link SSLEngine} associated with this socket.
     */
    public SSLEngine getEngine() {
        return this.engine;
    }

    /**
     * Invalidate the current {@link SSLSession}. This method could be
     * periodically used via a {@link Timeout} to perform SSL/TLS session
     * rotation if needed.
     */
    @Override
    public void invalidateSession() {
        engine.getSession().invalidate();
        handshakePending = true;
        taskPending = false;
    }

    /**
     * Initialize SSL/TLS handshaking. This method is called from the
     * {@link AbstractSelector} thread in two cases: (1) when
     * {@link #finishConnect()} is called or (2) when the {@link SSLSession} has
     * been previously invalidated.
     *
     * @throws IOException propagated exceptions from
     * {@link #processHandshake()}
     * @throws SSLException if there is an SSL/TLS problem with the call to the
     * underlying {@link SSLEngine#beginHandshake()}.
     */
    @Override
    public void initHandshake() throws IOException {
        engine.beginHandshake();
        processHandshake();
    }

    /**
     * Returns the underlying {@link SocketChannel}. This is done in order to
     * register the current socket with a {@link Selector}, as only the
     * {@link SocketChannel} implementation is allowed to be associated with a
     * {@link Selector}.
     *
     * @return the underlying SocketChannel
     */
    @Override
    public SocketChannel getSocket() {
        return this.sc;
    }

    /**
     * Pass-through implementation of
     * {@link SocketChannel#connect(SocketAddress remote)}
     *
     * @param remote The remote address to which this channel is to be connected
     * @return true if a connection was established, false if this channel is in
     * non-blocking mode and the connection operation is in progress
     * @throws IOException Propagated exceptions from the underlying
     * {@link SocketChannel#connect(SocketAddress remote)} implementation.
     */
    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        return sc.connect(remote);
    }

    /**
     * Pass-through implementation of {@link SocketChannel#finishConnect()} In
     * the {@link SecureSocket} implementation, once the connection of the
     * underlying {@link SocketChannel} is finished, the SSL/TLS handshake is
     * manually initiated here. <p> Note that this is not strictly necessary but
     * rather a convenience, as subsequent handshakes can be initiated from the
     * {@link SecureSocket#read(ByteBuffer buffer)} and
     * {@link SecureSocket#write(ByteBuffer buffer)} methods.
     *
     * @return true if, and only if, this channel's socket is now connected
     * @throws IOException Propagated exceptions from the underlying
     * {@link SocketChannel#finishConnect()} implementation.
     */
    @Override
    public boolean finishConnect() throws IOException {
        boolean ret = sc.finishConnect();
        if (ret) {// sanity check
            initHandshake();
        }
        return ret;
    }

    /**
     * Pass-through implementation of
     * {@link SocketChannel#register(Selector sel, int ops)}
     *
     * @param sel The selector with which this channel is to be registered
     * @param ops The interest set for the resulting key
     * @return A key representing the registration of this channel with the
     * given selector
     * @throws ClosedChannelException Propagated exceptions from the underlying
     * {@link SocketChannel#register(Selector sel, int ops)} implementation.
     */
    @Override
    public SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return sc.register(sel, ops);
    }

    /**
     * Pass-through implementation of
     * {@link SocketChannel#configureBlocking(boolean block)}
     *
     * @param block If true then this channel will be placed in blocking mode;
     * if false then it will be placed in non-blocking mode
     * @return This selectable channel
     * @throws IOException Propagated exceptions from the underlying
     * {@link SocketChannel#configureBlocking(boolean block)} implementation.
     */
    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        return sc.configureBlocking(block);
    }

    /**
     * Performs SSL/TLS handshaking. Note that it can perform NEED_TASK either
     * on the same thread or on the {@link TaskWorker} thread, based on how the
     * {@link SecureSocket} has been initialized.
     *
     * @throws IOException If there is an underlying IOException while
     * performing the handshake
     * @throws SSLException If there is an exception thrown by the underlying
     * {@link SSLEngine} while performing the handshake
     */
    @Override
    public void processHandshake() throws IOException {
        //System.out.println("In thread " + Thread.currentThread());
        int count;
        SSLEngineResult.HandshakeStatus status;
        // At first call of processHandshake(), there is no SSLEngineResult
        // yet, use the handshakeStatus from the SSLEngine instead
        if (result == null) {
            status = engine.getHandshakeStatus();
        } else {
            status = result.getHandshakeStatus();
        }
        // process the handshake status
        switch (status) {
            case NEED_TASK:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() NEED_TASK");
                // Run the delegated SSL/TLS tasks
                runDelegatedTasks();
                if (!singleThreaded) {
                    // Return as handshaking cannot continue
                    return;
                }
            case NEED_UNWRAP:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() NEED_UNWRAP");
                // Don’t read if inbound is already closed
                count = engine.isInboundDone()
                        ? -1
                        : sc.read(encryptedIn);
                encryptedIn.flip();
                result = engine.unwrap(encryptedIn, decryptedIn);
                encryptedIn.compact();
                break;
            case NEED_WRAP:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() NEED_WRAP");
                decryptedOut.flip();
                result = engine.wrap(decryptedOut, encryptedOut);
                decryptedOut.compact();
                if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                    // RFC 2246 #7.2.1 requires us to respond to an
                    // incoming close_notify with an outgoing
                    // close_notify. The engine takes care of this, so we
                    // are now trying to send a close_notify, which can
                    // only happen if we have just received a
                    // close_notify.
                    // Try to flush the close_notify.
                    try {
                        count = flush();
                    } catch (SocketException exc) {
                        // failed to send the close_notify, this can happen if
                        // the peer has already sent its close_notify and then
                        // close the socket, which is permitted by RFC_2246.
                    }
                } else {
                    // flush without the try/catch,
                    // letting any exceptions propagate.
                    count = flush();
                }
                break;
            case FINISHED:
                handshakePending = false;
                // Indicate to the associated handshake listener that the
                // handshake is complete
                hsListener.handshakeComplete(this);
            //System.out.println(sc.socket().getLocalPort() + ":"
            //+ sc.socket().getPort() + " processHandshake() FINISHED");
            case NOT_HANDSHAKING:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() NOT_HANDSHAKING");
                // handshake has been completed at this point, no need to 
                // check the status of the SSLEngineResult;
                return;
        }

        // Check the result of the preceding wrap or unwrap.
        switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
                // Return as we do not have enough data to continue processing
                // the handshake
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() BUFFER_UNDERFLOW");
                return;
            case BUFFER_OVERFLOW:
                // Return as the encrypted buffer has not been cleared yet
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() BUFFER_OVERFLOW");
                return;
            case CLOSED:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() CLOSED");
                if (engine.isOutboundDone()) {
                    sc.socket().shutdownOutput();// stop sending
                }
                return;
            case OK:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " processHandshake() OK");
                // handshaking can continue.
                break;
        }
        processHandshake();
    }

    /**
     * Runs the {@link SSLEngine} delegated tasks. The tasks can either run in
     * the same thread (single threaded implementation) or via the
     * {@link TaskWorker} thread (multithreaded implementation). Note that in
     * the single threaded implementation, the {@link AbstractSelector} thread
     * will block until all tasks are completed. Additionally, in this case, the
     * taskPending variable is not needed.
     */
    private void runDelegatedTasks() {
        if (singleThreaded) {
            // Run the delegated tasks in the same thread 
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
                task.run();
            }
            // Update the SSLEngineResult
            updateResult();
        } else {
            // Run the delegated tasks in the TaskWorker thread
            // An existing task might already be pending for completion with
            // the TaskWorker, and we could have arrived here from a read or
            // write. If this is the case, we should NOT requeue the task
            // with the TaskWorker, otherwise there will be subsequent
            // problems. E.g. the TaskWorker finishes a (multi-queued) task
            // which by the second time of processing is now null. This can
            // then trigger processHandshake() to run on a potentially
            // closed socket.
            if (!taskPending) {
                taskWorker.addSocket(this);
                setTaskPending(true);
            }
        }
    }

    /**
     * Update the {@link SSLEngineResult} and {@link #setTaskPending(boolean)}
     * to false. <p> This method is called once the {@link SSLEngine} has no
     * more pending tasks. The updated result is fused from both the previous
     * {@link SSLEngineResult} and the {@link SSLEngine#getHandshakeStatus()}.
     * <p> This is done such that we can receive the FINISHED
     * {@link SSLEngineResult#handshakeStatus} that would otherwise not be
     * available by just updating the existing {@link SSLEngineResult}. <p> We
     * also {@link #setTaskPending(boolean)} to false as there are no more tasks
     * needed to be completed for the underlying {@link SSLEngine}.
     */
    @Override
    public void updateResult() {
        result = new SSLEngineResult(
                result.getStatus(),
                engine.getHandshakeStatus(),
                result.bytesProduced(),
                result.bytesConsumed());
        // SSLEngine task was completed, set its taskPending
        // to false, in case there are more tasks to be run
        // in the future. TODO: does this ever happen?
        setTaskPending(false);
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer. This
     * is a pass-through implementation of the underlying
     * {@link SecureSocket#read(ByteBuffer buffer)}, with additional logic to
     * handle the SSL/TLS encrypted stream. <p> An attempt is made to read up to
     * r bytes from the encrypted channel, where r is the number of bytes
     * remaining in the buffer. If handshaking has not been previously
     * completed, handshaking also occurs at this stage. This method will also
     * respond appropriately with returning -1 (EOF) when the underlying
     * {@link SSLEngine#isInboundDone()}, or when reading from the encrypted
     * channel returns -1 (EOF);
     *
     *
     * @param buffer The buffer into which bytes are to be transferred
     * @return The number of bytes read, possibly zero, or -1 if the channel has
     * reached end-of-stream
     * @throws IOException Propagated exceptions from the underlying
     * {@link SocketChannel#read(ByteBuffer buffer)} implementation.
     * @throws BufferOverflowException If the underlying {@link SSLEngineResult}
     * has a status of BUFFER_OVERFLOW. As this should not happen in this
     * implementation, it can be considered serious and should be handled
     * @throws BufferUnderflowException If the underlying
     * {@link SSLEngineResult} has a status of BUFFER_UNDERFLOW. As this should
     * not happen in this implementation, it can be considered serious and
     * should be handled
     */
    @Override
    public int read(ByteBuffer buffer) throws IOException {
        if (engine.isInboundDone()) {
            // We can skip the read operation as the SSLEngine is closed,
            // instead, propagate EOF one level up
            return -1;
        }

        decryptedIn.clear();

        int pos = decryptedIn.position();
        // Read from the channel
        int count = sc.read(encryptedIn);
        //System.out.println(sc.socket().getLocalPort() + ":"
        //+ sc.socket().getPort() + " Read " + count + " bytes encrypted");
        if (count == -1) {
            // We have reached EOF, propagate one level up
            // At this point, we may not have received close_notify from peer,
            // and as such we cannot SSLEngine.closeInbound(), this will happen
            // in a subsequent step
            //engine.closeInbound();
            return count;
        }
        // Unwrap the data just read
        encryptedIn.flip();
        result = engine.unwrap(encryptedIn, decryptedIn);
        encryptedIn.compact();
        // Process the engineResult.Status
        switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
                // nothing was read. This is the entry point for initiating a
                // short timeout in the underlying socket if we are still in the
                // handshaking phase. The reason is to rule out DDOS attacks
                // where a high number of idle connections are created with the
                // sole purpose of either exhausting the ports of the host
                // machine or depleting the host machine's memory.
                // Schedule a new timeout
                toWorker.insert(timeout);
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " read() BUFFER_UNDERFLOW");
                return 0;
            case BUFFER_OVERFLOW:
                if (!timeout.hasExpired()) {
                    // cancel any previous timeout
                    toWorker.cancel(timeout);
                }
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " read() BUFFER_OVERFLOW");
                // This should never happen (ideally). If this happens, the
                // thread responsible for emptying the decryptedIn buffer has 
                // not done so in time. Throw an exception to be handled at the
                // application layer.
                throw new BufferOverflowException();
            case CLOSED:
                //System.out.println(sc.socket().getLocalPort() + ":"
                //+ sc.socket().getPort() + " read() CLOSED");
                // The SSLEngine was inbound closed, there is and will be no
                // more input from the engine, so setup the socket appropriately
                // too. An outbound close_notify will be send by the SSLEngine
                sc.socket().shutdownInput();
                break;
            case OK:
                if (!timeout.hasExpired()) {
                    // cancel any previous timeout
                    toWorker.cancel(timeout);
                }
                //System.out.println(sc.socket().getLocalPort()
                //+ ":" + sc.socket().getPort() + " read() OK");
                break;
        }
        // process any handshaking now required
        processHandshake();

        // put the data to the buffer given to us
        for (int i = pos; i < decryptedIn.position(); i++) {
            buffer.put(decryptedIn.get(i));
        }

        // return count of application data read
        count = decryptedIn.position() - pos;
        return count;
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        // Make shallow copy, assumes the entire buffer needs to be written
        //System.out.println("buffer position" + buffer.position());
        //System.out.println("buffer capacity " + buffer.capacity());
        while (buffer.hasRemaining()) {
            decryptedOut.put(buffer.get());
        }
        //System.out.println("decryptedOut position " + decryptedOut.position());
        //System.out.println("decryptedOut capacity " + decryptedOut.capacity());

        int pos = decryptedOut.position();
        encryptedOut.clear();
        // Wrap the data to be written
        decryptedOut.flip();
        result = engine.wrap(decryptedOut, encryptedOut);
        decryptedOut.compact();
        // Process the engineResult.Status
        switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
                //System.out.println(sc.socket().getLocalPort()
                //+ ":" + sc.socket().getPort() + " write() BUFFER_UNDERFLOW");
                // This shouldn't happen as we only call write() when there is
                // data to be written, throw an exception that will be handled
                // in the application layer.
                throw new BufferUnderflowException();
            case BUFFER_OVERFLOW:
                //System.out.println(sc.socket().getLocalPort()
                //+ ":" + sc.socket().getPort() + " write() BUFFER_OVERFLOW");
                // This shouldn't happen if we flush data that has been wrapped
                // as we do in this implementation, throw an exception that will
                // be handled in the application layer.
                throw new BufferOverflowException();
            case CLOSED:
                //System.out.println(sc.socket().getLocalPort()
                //+ ":" + sc.socket().getPort() + " write() CLOSED");
                // Trying to write on a closed SSLEngine, throw an exception
                // that will be handled in the application layer.
                throw new SSLException("SSLEngine is CLOSED");
            case OK:
                //System.out.println(sc.socket().getLocalPort()
                //+ ":" + sc.socket().getPort() + " write() OK");
                // Everything is good, everything is fine.
                break;
        }
        // Process any pending handshake
        processHandshake();
        // Flush any pending data to the network
        flush();
        // return count of application bytes written.
        return pos - decryptedOut.position();
    }

    /**
     * Flush encrypted output data to the underlying {@link SocketChannel}.
     *
     * An attempt is made to write all encrypted bytes to the channel, but it is
     * not guaranteed.
     *
     * @return The number of bytes written, possibly zero
     * @throws IOException Propagated exceptions from the underlying
     * {@link SocketChannel#write(ByteBuffer buffer)} implementation.
     */
    private int flush() throws IOException {
        encryptedOut.flip();
        int count = sc.write(encryptedOut);
        encryptedOut.compact();
        //System.out.println(sc.socket().getLocalPort()
        //+ ":" + sc.socket().getPort() + " Flushed " + count + " bytes");
        return count;
    }

    /**
     * Pass-through implementation of {@link SocketChannel#close()}. <p> Before
     * closing the underlying {@link SocketChannel}, attempts are made to
     * {@link #flush} any encrypted data remaining in the encrypted buffer and
     * cleanly close the associated {@link SSLEngine}. <p> Finally, we process
     * what we can via {@link #processHandshake()} before closing the underlying
     * {@link SocketChannel}.
     *
     * @throws IOException Propagated exceptions from the underlying
     * SocketChannel.close() implementation.
     * @throws SSLException If we have not received a close_notify when
     * {@link SSLEngine#closeInbound()} is invoked.
     */
    @Override
    public void close() throws IOException {
        //if (timeout.hasExpired()) {
        // This causes a threadlock, WHY? TODO
        // cancel any previous timeout
        //    toWorker.cancel(timeout);
        //}
        try {
            // Flush any pending encrypted output data
            flush();
            if (!engine.isOutboundDone()) {
                engine.closeOutbound();
                processHandshake();
                /*
                 * RFC 2246 #7.2.1: if we are initiating this
                 * close, we may send the close_notify without
                 * waiting for an incoming close_notify.
                 * If we weren't the initiator we would have already
                 * received the inbound close_notify in read(),
                 * and therefore already have done closeOutbound(),
                 * so, we are initiating the close,
                 * so we can skip the closeInbound().
                 */
            } else if (!engine.isInboundDone()) {
                // Closing inbound will throw an SSLException if we have not
                // received a close_notify.
                engine.closeInbound();
                // Process what we can before we close the channel.
                processHandshake();
            }
        } finally {
            // Clear all buffers TODO
            // Close the channel.
            sc.close();
        }
    }

    /**
     * Used to identify whether the handshaking performed from the underlying
     * {@link SSLEngine} is still pending. <p> This is called from the
     * application layer that invokes
     * {@link AbstractSelector#send(SocketIF, ByteBuffer)} to correctly queue
     * data to be written.
     *
     * @return whether the SSL/TLS handshaking is still pending
     */
    @Override
    public boolean handshakePending() {
        return this.handshakePending;
    }

    /**
     * Sets whether or not there is an {@link SSLEngine} task pending, during an
     * SSL/TLS handshake. <p> If the current socket implementation is processing
     * the pending tasks in the same thread, this method has no effect.
     * Otherwise in a multi-threaded implementation, this allows the
     * {@link AbstractSelector} thread to know the status of the task, and
     * subsequently correctly process incoming requests (e.g. queueing data to
     * be written).
     *
     * @param taskPending Set whether or not there is a {@link SSLEngine} task
     * pending for the {@link SSLEngine} associated with the underlying
     * {@link SocketChannel}. Has no effect in a single threaded implementation.
     */
    @Override
    public void setTaskPending(boolean taskPending) {
        this.taskPending = taskPending;
    }
}
