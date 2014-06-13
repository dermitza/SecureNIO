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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * An {@link ExecutorService} implementation of implementing timeouts for
 * {@link ch.dermitza.securenio.socket.SocketIF}s.
 * 
 * @deprecated since the number of threads allocated is too high for a low to
 * medium client size (anything above 10 clients really).
 * @author K. Dermitzakis
 * @version 0.18
 */
public class TimeoutExecutor {

    /**
     * Reasonable timeout value 30 seconds (?) for relevant discussions on
     * timeout length see: https://bugzilla.mozilla.org/show_bug.cgi?id=365898
     * http://support.f5.com/kb/en-us/solutions/public/13000/800/sol13863.html
     *
     */
    public static final long TIMEOUT_MS = 30000;
    private final ExecutorService timeoutService = Executors.newCachedThreadPool();

    /**
     * Schedule a timeout that expires in the future.
     *
     * @param worker the worker to submit
     * @return a Future representing pending completion of the task
     */
    public Future<TimeoutRunnable> scheduleTimeout(TimeoutRunnable worker) {
        return timeoutService.submit(worker, worker);
    }

    /**
     * Shuts the underlying {@link ExecutorService} down.
     */
    public void shutdown() {
        timeoutService.shutdownNow();
    }
}
