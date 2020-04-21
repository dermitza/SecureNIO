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

import ch.dermitza.securenio.socket.SocketIF;

/**
 * Task listeners listen for {@link javax.net.ssl.SSLEngine} task completions
 * and act upon them. As such, task listeners are only useful for
 * {@link SecureSocket} implementations. Only one task listener is associated
 * with each {@link SecureSocket} instance.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public interface TaskListener {

    /**
     * This method is called from the {@link TaskWorker} thread once it has
     * finished processing all {@link javax.net.ssl.SSLEngine} tasks associated
     * with a {@link SecureSocket} instance.
     *
     * @param socket The SocketIF that just had one (or more) SSLEngine task(s)
     * completed
     * @see SecureSocket
     * @see ch.dermitza.securenio.AbstractSelector
     */
    public void taskComplete(SocketIF socket);
}
