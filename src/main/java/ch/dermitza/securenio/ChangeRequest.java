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

import ch.dermitza.securenio.socket.SocketIF;

/**
 * A ChangeRequest defines a request for some operation that needs to be
 * executed on the selector thread.
 *
 * ChangeRequests are created and queued from threads that interact with the
 * selector thread and are necessary as the result of some particular operation
 * being completed (e.g. an SSLEngineTask having finished).
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public final class ChangeRequest {

    /**
     * This type concerns an {@link javax.net.ssl.SSLEngine} task that has just
     * finished running on the
     * {@link ch.dermitza.securenio.socket.secure.TaskWorker} thread. <p> This
     * type of task is used if and only if the
     * {@link ch.dermitza.securenio.socket.secure.SecureSocket} is processing
     * tasks via the {@link ch.dermitza.securenio.socket.secure.TaskWorker}
     * thread (multi-threaded implementation).
     */
    public static final int TYPE_TASK = 0;
    /**
     * This type concerns switching the interestOps of a key associated with a
     * particular socket.
     */
    public static final int TYPE_OPS = 1;
    /**
     * This type concerns a timeout that has expired on the given socket. As
     * such, the socket needs to be closed.
     */
    public static final int TYPE_TIMEOUT = 2;
    /**
     * This type concerns an {@link javax.net.ssl.SSLSession} that has been
     * invalidated. As such, we need to re-initiate handshaking.
     */
    public static final int TYPE_SESSION = 3;
    /**
     * The SocketIF associated with this ChangeRequest
     */
    private final SocketIF sc;
    /**
     * The type associated with this ChangeRequest
     */
    private final int type;
    /**
     * The interestOps associated with this ChangeRequest. If the type of this
     * ChangeRequest is anything other than TYPE_OPS, the interestOps can be set
     * to anything safely.
     */
    private final int interestOps;

    /**
     * A ChangeRequest defines a request for some operation that needs to be
     * executed on the selector thread.
     *
     * ChangeRequests are created and queued from threads that interact with the
     * selector thread and are necessary as the result of some particular
     * operation being completed (e.g. an SSLEngine task having finished).
     *
     * @param sc The SocketIF associated with this ChangeRequest
     * @param type The type associated with this ChangeRequest
     * @param interestOps The interestOps (SelectionKey.interestOps) associated
     * with this ChangeRequest. If the type of this ChangeRequest is anything
     * other than TYPE_OPS, the interestOps can be set to anything safely.
     */
    public ChangeRequest(SocketIF sc, int type, int interestOps) {
        this.sc = sc;
        this.type = type;
        this.interestOps = interestOps;
    }

    /**
     * Get the SocketIF associated with this ChangeRequest
     *
     * @return The SocketIF associated with this ChangeRequest
     */
    public SocketIF getChannel() {
        return this.sc;
    }

    /**
     * Get the type associated with this ChangeRequest
     *
     * @return The type associated with this ChangeRequest
     */
    public int getType() {
        return this.type;
    }

    /**
     * Get the interestOps associated with this ChangeRequest
     *
     * @return The interestOps associated with this ChangeRequest. Return of
     * this method is unspecified for ChangeRequests with types other than
     * TYPE_OPS.
     */
    public int getOps() {
        return this.interestOps;
    }
}
