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
package ch.dermitza.securenio.socket.timeout.threadpool;

import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.socket.timeout.TimeoutListener;

/**
 * A {@link Runnable} implementation of a timeout to be used with a
 * {@link java.util.concurrent.ExecutorService}. Timeouts are associated with a
 * {@link SocketIF} and a {@link TimeoutListener}. When a timeout is expired, it
 * notifies the listener of the expiration, at which time the listener should
 * act on the timeout (e.g. closing the socket, performing an SSL/TLS
 * re-handshake and so on).
 *
 * @deprecated since the number of threads allocated is too high for a low to
 * medium client size (anything above 10 clients really).
 * @author K. Dermitzakis
 * @version 0.18
 *
 * @see TimeoutExecutor
 */
public class TimeoutRunnable implements Runnable {

    private final SocketIF socket;
    private final TimeoutListener listener;
    private final long timeout;

    /**
     *
     * @param listener
     * @param socket
     * @param timeout
     */
    public TimeoutRunnable(TimeoutListener listener, SocketIF socket, long timeout) {
        this.listener = listener;
        this.socket = socket;
        this.timeout = timeout;
    }

    /**
     *
     */
    @Override
    public void run() {
        // sleep for timeout
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            // The timeout was cancelled, exit the run() method.
            return;
        }
        // If the task has not been cancelled or reset at this point, the
        // timeout period has expired and we need to close the socket on
        // the selector thread.
        if (listener != null) {
            listener.timeoutExpired(socket);
        }
    }
}
