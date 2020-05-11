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
package ch.dermitza.securenio.util;

import ch.dermitza.securenio.util.logging.LoggerHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A static helper implementation for setting up static runtime options. The
 * options are located in a {@link Properties} file. For further information on
 * the options set, please look at the supplied setup.properties.
 *
 * @author K. Dermitzakis
 * @version 0.21
 * @since 0.19
 */
public class PropertiesReader {

    private static final Logger LOGGER = LoggerHandler.getLogger(PropertiesReader.class.getName());
    private static final String SETTINGS_LOC = "setup.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = PropertiesReader.class.getClassLoader()
                .getResourceAsStream(SETTINGS_LOC)) {
            PROPS.load(is);
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Properties file not found, exiting", ex);
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "IOException while reading properties, exiting", ex);
            System.exit(-1);
        }
    }

    /**
     * Return whether the selector should handle {@link javax.net.ssl.SSLEngine}
     * tasks in the same thread. If not, a
     * {@link ch.dermitza.securenio.socket.secure.TaskWorker} is initialized and
     * used for that purpose.
     *
     * @return whether the selector should handle
     * {@link javax.net.ssl.SSLEngine} tasks in the same thread.
     * @see ch.dermitza.securenio.socket.secure.TaskWorker
     */
    public static boolean getSelectorSingleThreaded() {
        return getPropAsBool("selector.single_threaded");
    }

    /**
     * Return whether the selector thread should process all
     * {@link ch.dermitza.securenio.ChangeRequest}s at each iteration. If not,
     * the values from {@link #getMaxChanges()} and
     * {@link #getSelectorTimeoutMS()} are used.
     *
     * @return whether the selector thread should process all
     * {@link ch.dermitza.securenio.ChangeRequest}s
     *
     * @see #getMaxChanges()
     * @see #getSelectorTimeoutMS()
     * @see ch.dermitza.securenio.AbstractSelector#processChanges()
     */
    public static boolean getSelectorProcessAll() {
        return getPropAsBool("selector.process_all_changes");
    }

    /**
     * If the selector thread should process all
     * {@link ch.dermitza.securenio.ChangeRequest}s at each iteration, this
     * method returns the maximum changes to be processed at each iteration.
     *
     * @return the maximum {@link ch.dermitza.securenio.ChangeRequest}s
     * processed at each iteration
     *
     * @see #getMaxChanges()
     * @see #getSelectorTimeoutMS()
     * @see ch.dermitza.securenio.AbstractSelector#processChanges()
     */
    public static int getMaxChanges() {
        int i = getPropAsInt("socket.max_changes");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.max_changes value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * If the selector thread should process all
     * {@link ch.dermitza.securenio.ChangeRequest}s at each iteration, this
     * method returns the maximum time (MS) the selector should wait on a
     * select() before returning to process the remaining changes.
     *
     * @return the maximum time (MS) the selector should wait on a select()
     * before returning to process the remaining changes.
     *
     * @see #getMaxChanges()
     * @see #getSelectorTimeoutMS()
     * @see ch.dermitza.securenio.AbstractSelector#processChanges()
     */
    public static long getSelectorTimeoutMS() {
        long l = getPropAsLong("socket.max_changes");

        if (l < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.max_changes value is invalid: {0}. Shutting down", l);
            System.exit(-1);
        }
        return l;
    }

    /**
     * Returns the timeout period (MS) for a
     * {@link ch.dermitza.securenio.socket.secure.SecureSocket} to wait on an
     * empty buffer during handshaking.
     *
     * @return the timeout period (MS) for a
     * {@link ch.dermitza.securenio.socket.secure.SecureSocket} to wait on an
     * empty buffer during handshaking.
     */
    public static long getTimeoutMS() {
        long l = getPropAsLong("timeout.period_ms");

        if (l < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.max_changes value is invalid: {0}. Shutting down", l);
            System.exit(-1);
        }
        return l;
    }

    /**
     * Returns the size of the backlog (socket number) of a
     * {@link java.nio.channels.ServerSocketChannel}.
     *
     * @return the size of the backlog (socket number) of a
     * {@link java.nio.channels.ServerSocketChannel}.
     */
    public static int getBacklog() {
        int i = getPropAsInt("socket.backlog");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.backlog value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Returns the default size (bytes) of the
     * {@link ch.dermitza.securenio.packet.worker.AbstractPacketWorker}. Note
     * that the size can grow if the data received overflows on a particular
     * socket.
     *
     * @return the default size (bytes) of the
     * {@link ch.dermitza.securenio.packet.worker.AbstractPacketWorker}.
     *
     * @see
     * ch.dermitza.securenio.packet.worker.AbstractPacketWorker#addData(ch.dermitza.securenio.socket.SocketIF,
     * java.nio.ByteBuffer, int)
     */
    public static int getPacketBufSize() {
        int i = getPropAsInt("packetworker.buffer_size");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "packetworker.buffer_size value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Returns the SO_SNDBUF size (bytes) to be set for each socket.
     *
     * @return the SO_SNDBUF size (bytes) to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#SO_SNDBUF
     */
    public static int getSoSndBuf() {
        int i = getPropAsInt("socket.so_sndbuf");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.so_sndbuf value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Returns the SO_RCVBUF size (bytes) to be set for each socket.
     *
     * @return the SO_RCVBUF size (bytes) to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#SO_RCVBUF
     */
    public static int getSoRcvBuf() {
        int i = getPropAsInt("socket.so_rcvbuf");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.so_rcvbuf value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Returns the IP_TOS to be set for each socket.
     *
     * @return the IP_TOS to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#IP_TOS
     */
    public static int getIPTos() {
        int i = getPropAsInt("socket.ip_tos");

        if (i < 0) {
            LOGGER.log(Level.SEVERE,
                    "socket.ip_tos value is invalid: {0}. Shutting down", i);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Get the enabled protocols to be used with a
     * {@link javax.net.ssl.SSLEngine}.
     *
     * @return the enabled protocols to be used with a
     * {@link javax.net.ssl.SSLEngine}.
     */
    public static String[] getProtocols() {
        String[] ret = getPropAsStrArr("secure.protocols");
        return ret;
    }

    /**
     * Get the enabled cipher suites to be used with a
     * {@link javax.net.ssl.SSLEngine}.
     *
     * @return the enabled cipher suites to be used with a
     * {@link javax.net.ssl.SSLEngine}.
     */
    public static String[] getCipherSuites() {
        String[] ret = getPropAsStrArr("secure.cipherSuites");

        return ret;
    }

    /**
     * Returns the TCP_NODELAY size (bytes) to be set for each socket.
     *
     * @return the TCP_NODELAY size (bytes) to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#TCP_NODELAY
     */
    public static boolean getTCPNoDelay() {
        return getPropAsBool("socket.tcp_nodelay");
    }

    /**
     * Returns the SO_KEEPALIVE size (bytes) to be set for each socket.
     *
     * @return the SO_KEEPALIVE size (bytes) to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#SO_KEEPALIVE
     */
    public static boolean getKeepAlive() {
        return getPropAsBool("socket.so_keepalive");
    }

    /**
     * Returns the SO_REUSEADDR size (bytes) to be set for each socket.
     *
     * @return the SO_REUSEADDR size (bytes) to be set for each socket.
     *
     * @see java.net.StandardSocketOptions#SO_REUSEADDR
     */
    public static boolean getReuseAddress() {
        return getPropAsBool("socket.so_reuseaddr");
    }

    /**
     * Return a property as a String array.
     *
     * @param key The key used to retrieve the property
     * @return the returned String array
     */
    private static String[] getPropAsStrArr(String key) {
        String str = getProp(key);
        String[] ret = str.split(" ");
        return ret;
    }

    /**
     * Return a property as a long. An error is thrown if the number is smaller
     * than zero.
     *
     * @param key The key used to retrieve the property
     * @return the returned long
     */
    private static long getPropAsLong(String key) {
        String str = getProp(key);
        long l = -1;
        try {
            l = Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            LOGGER.log(Level.SEVERE, key + " value is invalid, shutting down", nfe);
            System.exit(-1);
        }
        return l;
    }

    /**
     * Return a property as an int. An error is thrown if the number is smaller
     * than zero.
     *
     * @param key The key used to retrieve the property
     * @return the returned int
     */
    private static int getPropAsInt(String key) {
        String str = getProp(key);
        int i = -1;
        try {
            i = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            LOGGER.log(Level.SEVERE, key + " value is invalid, shutting down", nfe);
            System.exit(-1);
        }
        return i;
    }

    /**
     * Return a property as a boolean. An error is thrown if the values are
     * anything other than "true" or "false".
     *
     * @param key The key used to retrieve the property
     * @return the returned boolean
     */
    private static boolean getPropAsBool(String key) {
        String str = getProp(key);
        if (!str.equals("true") && !str.equals("false")) {
            LOGGER.log(Level.SEVERE, "{0} value is invalid: {1}. Shutting down",
                    new Object[]{key, str});
            System.exit(-1);
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * Get a property. An error is thrown if the value of the property is not
     * found or is empty.
     *
     * @param key The key used to retrieve the property
     * @return the associated property
     */
    private static String getProp(String key) {
        String str = PROPS.getProperty(key);
        if (str == null || str.isEmpty()) {
            // Fail, these are essential properties
            LOGGER.log(Level.SEVERE, "{0} value not found, shutting down.", key);
            System.exit(-1);
        }
        return str;
    }

    private PropertiesReader() {
        // Static class, disallow instantiation
    }
    //public static void main(String[] args) {
    //PropertiesReader.getSoSndBuf();
    //String[] s = PropertiesReader.getPropAsStrArr("secure.cipherSuites");
    //for (int i = 0; i < s.length; i++) {
    //    System.out.println(s[i]);
    //}
    //}
}
