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
package ch.dermitza.securenio.socket.timeout;

import ch.dermitza.securenio.socket.SocketIF;

/**
 * Timeout listeners listen for expired
 * {@link ch.dermitza.securenio.socket.timeout.worker.Timeout}s and act upon
 * them. Only one timeout listener is associated with each
 * {@link ch.dermitza.securenio.socket.timeout.worker.Timeout} instance.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public interface TimeoutListener {

    /**
     * This method is called from the
     * {@link ch.dermitza.securenio.socket.timeout.worker.Timeout#expired()}
     * method, notifying any Timeout Listeners that the Timeout has expired and
     * they should act upon it.
     *
     * @param socket The SocketIF that just had its Timeout expired
     */
    public void timeoutExpired(SocketIF socket);
}
