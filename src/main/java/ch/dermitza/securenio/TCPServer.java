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

import ch.dermitza.securenio.packet.PacketIF;
import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.socket.PlainSocket;
import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.socket.secure.SecureSocket;
import ch.dermitza.securenio.util.PropertiesReader;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * A TCP Server implementation of {@link AbstractSelector}. This implementation
 * is purely non-blocking and can handle both {@link PlainSocket}s and
 * {@link SecureSocket}s.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class TCPServer extends AbstractSelector {

    private ServerSocketChannel ssc;

    /**
     * Create a TCPServer instance.
     *
     * @param address The address to bind to
     * @param port The port to listen on
     * @param packetWorker The instance of packet worker to use
     * @param usingSSL Whether we are using SSL/TLS
     * @param needClientAuth Whether we need clients to also authenticate
     */
    public TCPServer(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean needClientAuth) {
        super(address, port, packetWorker, usingSSL, false,
                needClientAuth);
    }

    /**
     * Send an {@link PacketIF} over the specified {@link SocketIF}.
     *
     * @param sc The SocketIF to send the packet through.
     * @param packet The PacketIF to send through the associated SocketIF.
     *
     * @see AbstractSelector#send(ch.dermitza.securenio.socket.SocketIF,
     * java.nio.ByteBuffer)
     */
    public void send(SocketIF sc, PacketIF packet) {
        send(sc, packet.toBytes());
    }

    /**
     * Initialize a server connection. This method initializes a
     * {@link ServerSocketChannel}, configures it to non-blocking, binds it to
     * the specified (if any) host and port, sets the specified backlog and
     * registers it with the underlying {@link java.nio.channels.Selector}
     * instance with an OP_ACCEPT {@link SelectionKey}.
     *
     * @throws IOException Propagates all underlying IOExceptions as thrown, to
     * be handled by the application layer.
     *
     * @see AbstractSelector#run()
     */
    @Override
    protected void initConnection() throws IOException {

        // Create a new non-blocking server socket channel
        logger.config("Creating NB ServerSocketChannel");
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        // Bind the server socket to the specified address and port
        logger.log(Level.CONFIG, "Binding ServerSocket to *:{0}", port);
        InetSocketAddress isa = new InetSocketAddress(address, port);
        //ssc.socket().setReuseAddress(true);
        ssc.socket().bind(isa, PropertiesReader.getBacklog());

        // Register the server socket channel, indicating an interest in 
        // accepting new connections
        logger.finest("Registering ServerChannel to selector");
        ssc.register(selector, SelectionKey.OP_ACCEPT);

    }

    /**
     * Accepts incoming connections and binds new non-blocking {@link SocketIF}
     * instances to them. If this server implementation is using SSL/TLS, it
     * also sets up the {@link SSLEngine}, to be used.
     *
     * @param key The selection key with the underlying {@link SocketChannel} to
     * be accepted
     *
     * @see AbstractSelector#run()
     */
    @Override
    protected void accept(SelectionKey key) {
        SocketChannel socketChannel = null;
        SocketIF socket = null;
        String peerHost = null;
        int peerPort = 0;
        try {
            // Accept the connection and make it non-blocking
            socketChannel = ssc.accept();
            socketChannel.configureBlocking(false);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, PropertiesReader.getTCPNoDelay());
            socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, PropertiesReader.getSoSndBuf());
            socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, PropertiesReader.getSoRcvBuf());
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, PropertiesReader.getKeepAlive());
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, PropertiesReader.getReuseAddress());
            socketChannel.setOption(StandardSocketOptions.IP_TOS, PropertiesReader.getIPTos());

            // Register the new SocketChannel with our Selector, indicating
            // we'd like to be notified when there's data waiting to be read
            socketChannel.register(selector, SelectionKey.OP_READ);

            // Now wrap it in our container
            if (usingSSL) {
                peerHost = socketChannel.socket().getInetAddress().getHostAddress();
                peerPort = socketChannel.socket().getPort();
                SSLEngine engine = setupEngine(peerHost, peerPort);

                socket = new SecureSocket(socketChannel, engine, singleThreaded,
                        taskWorker, toWorker, this, this);
            } else {
                socket = new PlainSocket(socketChannel);
            }
        } catch (IOException ioe) {
            logger.log(Level.INFO, "IOE accepting the connection", ioe);
            // If accepting the connection failed, close the socket and remove
            // any references to it
            if (socket != null) {
                closeSocket(socket);
            }
            return;
        }
        // Finally, add the socket to our socket container
        container.addSocket(socketChannel, socket);
        logger.log(Level.CONFIG, "{0}:{1} connected", new Object[]{peerHost, peerPort});
    }

    /**
     * As this is the server implementation, it is NOT allowed to call this
     * method which is only useful for client implementations. This
     * implementation will throw a {@link NoSuchMethodError} if it is called and
     * do nothing else.
     *
     * @param key The selection key to be used for connecting.
     *
     * @see AbstractSelector#run()
     */
    @Override
    protected void connect(SelectionKey key) {
        throw new NoSuchMethodError("connect() is never called in client");
    }
}
