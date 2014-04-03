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
package ch.dermitza.securenio.socket.timeout.worker;

import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.socket.timeout.TimeoutListener;

/**
 * A timeout implementation to be used with a {@link TimeoutWorker}. Timeouts
 * are associated with a {@link SocketIF} and a {@link TimeoutListener}. When a
 * timeout is expired, it notifies the listener of the expiration, at which time
 * the listener should act on the timeout (e.g. closing the socket, performing
 * an SSL/TLS re-handshake and so on).
 *
 * @author K. Dermitzakis
 * @version 0.18
 *
 * @see TimeoutWorker
 * @see TimeoutWorker
 */
public class Timeout implements Comparable<Timeout> {

    /**
     * Default timeout value (in ms)
     */
    public static final long TIMEOUT_MS = 10000;
    /**
     * Unused TODO
     */
    public static final int SSL_TIMEOUT = 0;
    /**
     * Unused TODO
     */
    public static final int GENERIC_TIMEOUT = 1;
    private final SocketIF socket;
    private final TimeoutListener listener;
    private final long timeout;
    private final long created;
    private final long delta;
    private boolean hasExpired = false;

    /**
     * Create a new timeout for the {@link SocketIF} given. If the timeout
     * expires, it will fire {@link TimeoutListener#timeoutExpired(SocketIF)} on
     * the associated listener.
     *
     * @param socket The socket the timeout is to be associated with
     * @param listener The listener listening for timeout expiration on this
     * socket
     * @param timeout The timeout period
     */
    public Timeout(SocketIF socket, TimeoutListener listener, long timeout) {
        this.socket = socket;
        this.listener = listener;
        this.timeout = timeout;
        this.created = System.currentTimeMillis();
        this.delta = created + timeout;
    }

    /**
     * Get the {@link SocketIF} this timeout is associated with
     *
     * @return the {@link SocketIF} this timeout is associated with
     */
    public SocketIF getSocket() {
        return this.socket;
    }

    /**
     * Get the timeout period
     *
     * @return the timeout period
     */
    public long getTimeout() {
        return this.timeout;
    }

    /**
     * Get the time this timeout was created
     *
     * @return the time this timeout was created
     */
    public long getCreated() {
        return this.created;
    }

    /**
     * Get the absolute expiration time of this timeout
     *
     * @return the absolute expiration time of this timeout
     */
    public long getDelta() {
        return this.delta;
    }

    /**
     * Sets this timeout to expired state, firing any attached listeners in the
     * process.
     */
    public void expired() {
        hasExpired = true;
        if (listener != null) {
            listener.timeoutExpired(socket);
        }
    }

    /**
     * Check whether or not this timeout is expired right now. If the timeout
     * was set to its expired state via {@link #expired()}, time checking is not
     * performed, returning true directly.
     *
     * @return whether or not this timeout is expired right now
     */
    public boolean isExpired() {
        return hasExpired ? hasExpired : (System.currentTimeMillis() - getDelta()) >= 0;
    }

    /**
     * Whether or not this timeout already had its associated {@link #expired()}
     * method called and had its {@link TimeoutListener} fired.
     *
     * @return Whether or not this timeout already had its associated
     * {@link #expired()} method called and had its {@link TimeoutListener}
     * fired.
     */
    public boolean hasExpired() {
        return hasExpired;
    }

    /**
     * Overrides the {@link Comparable} interface to allow timeouts to be
     * compared. Timeouts are compared based on their absolute expiration times.
     *
     * @param t The timeout to compare to
     * @return A negative value if the current timeout's expiration time is
     * earlier than the one of the given timeout, a positive value if it is
     * later, or 0 if both timeouts expire at the same time
     */
    @Override
    public int compareTo(Timeout t) {
        return (int) (getDelta() - t.getDelta());
    }
}
