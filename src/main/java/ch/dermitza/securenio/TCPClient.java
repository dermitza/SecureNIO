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
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * A TCP Client implementation of {@link AbstractSelector}. This implementation
 * is purely non-blocking and can handle both {@link PlainSocket}s and
 * {@link SecureSocket}s.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class TCPClient extends AbstractSelector {

    private SocketIF sc;
    private boolean connected = false;

    /**
     * Create a TCPClient instance
     *
     * @param address The address to connect to
     * @param port The port to connect to
     * @param packetWorker The instance of packet worker to use
     * @param usingSSL Whether we are using SSL/TLS
     * @param needClientAuth Whether this client should also authenticate with
     * the server.
     */
    public TCPClient(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean needClientAuth) {
        super(address, port, packetWorker, usingSSL, true,
                needClientAuth);
    }

    /**
     * Send an {@link PacketIF} over the this client's {@link SocketIF}. Since
     * the client only has one socket, no socket parameter is necessary.
     *
     * @param packet The PacketIF to send through the associated SocketIF.
     *
     * @see AbstractSelector#send(ch.dermitza.securenio.socket.SocketIF,
     * java.nio.ByteBuffer)
     */
    public void send(PacketIF packet) {
        // Sometimes during testing send is called before the socket is
        // even initialized. Does this happen on actual single client code?
        if (sc != null) {
            send(sc, packet.toBytes());
        }
    }

    /**
     * Invalidate the SSL/TLS session (if any) on the underlying
     * {@link SocketIF}. As this client implementation only has a single socket,
     * no parameter is needed.
     *
     * @see AbstractSelector#invalidateSession(SocketIF)
     */
    public void invalidateSession() {
        if (sc != null) {
            invalidateSession(sc);
        }
    }

    /**
     * Initialize a client connection. This method initializes a
     * {@link SocketChannel}, configures it to non-blocking, and registers it
     * with the underlying {@link java.nio.channels.Selector} instance with an
     * OP_CONNECT {@link SelectionKey}. If this client implementation is using
     * SSL/TLS, it also sets up the {@link SSLEngine}, to be used.
     *
     * @throws IOException Propagates all underlying IOExceptions as thrown, to
     * be handled by the application layer.
     *
     * @see AbstractSelector#run()
     */
    @Override
    protected void initConnection() throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(address, port));

        // As part of the registration we'll register
        // an interest in connection events. These are raised when a channel
        // is ready to complete connection establishment.
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.setOption(StandardSocketOptions.SO_SNDBUF, PropertiesReader.getSoSndBuf());
        channel.setOption(StandardSocketOptions.SO_RCVBUF, PropertiesReader.getSoRcvBuf());
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, PropertiesReader.getKeepAlive());
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, PropertiesReader.getReuseAddress());
        channel.setOption(StandardSocketOptions.IP_TOS, PropertiesReader.getIPTos());

        // now wrap the channel
        if (usingSSL) {
            String peerHost = channel.socket().getInetAddress().getHostAddress();
            int peerPort = channel.socket().getPort();
            SSLEngine engine = setupEngine(peerHost, peerPort);

            sc = new SecureSocket(channel, engine, singleThreaded, taskWorker,
                    toWorker, this, this);
        } else {
            sc = new PlainSocket(channel);
        }
        // add the socket to the container
        container.addSocket(sc.getSocket(), sc);
    }

    /**
     * As this is the client implementation, it is NOT allowed to call this
     * method which is only useful for server implementations. This
     * implementation will throw a {@link NoSuchMethodError} if it is called and
     * do nothing else.
     *
     * @param key The selection key with the underlying {@link SocketChannel} to
     * be accepted
     *
     * @see AbstractSelector#run()
     */
    @Override
    protected void accept(SelectionKey key) {
        // This is severe if it happens
        throw new NoSuchMethodError("accept() is never called in client");
    }

    /**
     * Finish the connection to the server. This method also instantiates an
     * SSLEngine handshake if the underlying {@link SocketIF} is a secure
     * socket. Finally, after the connection has been established, the socket is
     * registered to the underlying {@link java.nio.channels.Selector}, with a
     * {@link SelectionKey} of OP_READ, signalling it is ready to read data.
     *
     * @param key The selection key with the underlying {@link SocketChannel}
     * that needs a connection finalization.
     */
    @Override
    protected void connect(SelectionKey key) {
        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            // TCP_NODELAY should be called after we are ready to connect,
            // otherwise the socket does not recognize the option.
            sc.getSocket().setOption(StandardSocketOptions.TCP_NODELAY, PropertiesReader.getTCPNoDelay());
            sc.finishConnect();
        } catch (IOException ioe) {
            // Cancel the channel's registration with our selector
            // since it faled to connect. At this point, there is no
            // reason to keep the client running. Perhaps we can issue
            // a reconnection attempt at a later stage, TODO.
            setRunning(false);
            logger.log(Level.SEVERE, "IOE at finishConnect(), shutting down", ioe);
            key.cancel();
            return;
        }

        // We are connected at this point
        connected = true;
        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_READ);
    }

    /**
     * Returns whether or not this client is connected, if and only if it is
     * running (returns false otherwise).
     *
     * @return whether or not this client is connected, if and only if it is
     * running (returns false otherwise)
     */
    public boolean isConnected() {
        return isRunning() ? this.connected : false;
    }

    /**
     * This method overrides the default
     * {@link AbstractSelector#closeSocket(SocketIF)} method, to also stop this
     * client from running, as this client implementation only has one
     * associated {@link SocketIF}.
     *
     * @param socket The SocketIF to be closed
     *
     * @see AbstractSelector#closeSocket(SocketIF)
     */
    @Override
    protected void closeSocket(SocketIF socket) {
        // This method is only called from AbstractTCPSelector
        // If this method has been called in a client implementation,
        // as a client, we must shutdown cleanly. Stop running
        setRunning(false);
        // and close the socket
        super.closeSocket(socket);
    }
}
