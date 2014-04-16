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
package ch.dermitza.securenio.socket;

import java.nio.channels.SelectableChannel;
import java.util.HashMap;

/**
 * A container implementation containing
 * {@link SelectableChannel}, {@link SocketIF} pairs. This container is used by
 * the {@link ch.dermitza.securenio.AbstractSelector} to keep track of valid
 * sockets. While it is used from a client implementation, it is most useful to
 * a server implementation that deals with multiple connected sockets (clients).
 * <p> This implementation is synchronized and thread-safe.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class SocketContainer {

    /**
     * The underlying HashMap containing the pairs of {@link SelectableChannel}
     * and {@link SocketIF}.
     */
    private final HashMap<SelectableChannel, SocketIF> sockets = new HashMap<>();

    /**
     * Get the {@link SocketIF} that is paired to the given
     * {@link SelectableChannel} key.
     *
     * @param key the {@link SelectableChannel} key whose associated value is to
     * be returned
     * @return the {@link SocketIF} to which the specified
     * {@link SelectableChannel} is mapped, or null if this map contains no
     * mapping for the {@link SelectableChannel}.
     */
    public synchronized SocketIF getSocket(SelectableChannel key) {
        return sockets.get(key);
    }

    /**
     * Returns true if this map contains a mapping for the specified
     * {@link SelectableChannel} key.
     *
     * @param key {@link SelectableChannel} key whose presence in this map is to
     * be tested
     * @return true if this map contains a {@link SocketIF} mapping for the
     * specified {@link SelectableChannel} key
     */
    public synchronized boolean containsKey(SelectableChannel key) {
        return sockets.containsKey(key);
    }

    /**
     * Associates the specified {@link SocketIF} with the specified
     * {@link SelectableChannel} in this map. If the map previously contained a
     * mapping for the {@link SelectableChannel}, the old {@link SocketIF} is
     * replaced by the specified {@link SocketIF}.
     *
     * @param key the {@link SelectableChannel} key with which the specified
     * value is to be associated
     * @param socket {@link SocketIF} to be associated with the specified key
     */
    public synchronized void addSocket(SelectableChannel key, SocketIF socket) {
        sockets.put(key, socket);
        //System.out.println("Sockets: " + size());
    }

    /**
     * Remove the {@link SocketIF} that is paired to the given
     * {@link SelectableChannel} key.
     *
     * @param key the {@link SelectableChannel} key whose associated value is to
     * be removed from the map
     * @return the previous {@link SocketIF} associated with
     * {@link SelectableChannel} is mapped, or null if there was no mapping for
     * {@link SelectableChannel}.
     */
    public synchronized SocketIF removeSocket(SelectableChannel key) {
        //System.out.println("Sockets: " + (size() - 1));
        return sockets.remove(key);
    }

    /**
     * Removes all {@link SelectableChannel}, {@link SocketIF} mappings from
     * this map. The map will be empty after this call returns.
     */
    public synchronized void clear() {
        sockets.clear();
    }

    /**
     * Returns the number of {@link SelectableChannel}, {@link SocketIF}
     * mappings in this map. If the map contains more than Integer.MAX_VALUE
     * elements, returns Integer.MAX_VALUE.
     *
     * @return the number of {@link SelectableChannel}, {@link SocketIF}
     * mappings in this map
     */
    public synchronized int size() {
        return sockets.size();
    }
}
