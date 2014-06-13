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

import ch.dermitza.securenio.util.MinContainer;
import ch.dermitza.securenio.util.logging.LoggerHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A threaded implementation of a worker processing {@link Timeout}s required by
 * the {@link ch.dermitza.securenio.AbstractSelector}. <p> The worker processes
 * timeouts in an expiration-based FIFO fashion. Every existing Timeout is being
 * waited upon until either cancelled or expired. If a timeout expires, the
 * {@link Timeout#expired()} method is called on that timeout. <p> Every
 * insertion or removal of Timeouts causes the TimeoutWorker to wake up from
 * waiting, recalculate the shortest timeout to wait for, and go back to sleep.
 * The {@link TimeoutWorker} is otherwise waiting for incoming tasks via the
 * {@link #insert(Timeout)} method.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class TimeoutWorker implements Runnable {

    private static final Logger logger = LoggerHandler.getLogger(TimeoutWorker.class.getName());
    private final MinContainer<Timeout> timeouts = new MinContainer<>();
    //private final Object lock = new Object();
    private Timeout currentTO = null;
    private long waitTime;
    private boolean running = false;
    private int inserted = 0;
    private int expired = 0;
    private int cancelled = 0;

    /**
     * Insert a {@link Timeout} that needs to be waited upon. Every Timeout
     * insertion causes the TimeoutWorker to wake up from waiting, recalculate
     * the shortest timeout to wait for, and go back to sleep. <p> This
     * implementation is thread-safe.
     *
     * @param timeout The Timeout to be waited upon.
     */
    public synchronized void insert(Timeout timeout) {
        timeouts.add(timeout);
        inserted++;
        this.notify();
    }

    /**
     * Cancel an already waited-on {@link Timeout}. Every Timeout removal causes
     * the TimeoutWorker to wake up from waiting, recalculate the shortest
     * timeout to wait for, and go back to sleep. <p> This implementation is
     * thread-safe.
     *
     * @param timeout The timeout to cancel
     */
    public synchronized void cancel(Timeout timeout) {
        if (timeouts.isEmpty()) {
            // Nothing to cancel
            return;
        }
        if (timeouts.remove(timeout)) {
            cancelled++;
        } else {
            logger.info("Trying to cancel already removed timeout");
        }
        logger.log(Level.FINEST, "Timeout cancelled at {0}, expiring at: {1}",
                new Object[]{System.currentTimeMillis(), timeout.getDelta()});
        this.notify();
    }

    /**
     * Check whether the {@link TimeoutWorker} is running. <p> This
     * implementation is thread-safe.
     *
     * @return true if it is running, false otherwise
     */
    public synchronized boolean isRunning() {
        return this.running;
    }

    /**
     * Set the running status of the {@link TimeoutWorker}. If the running
     * status of the worker is set to false, the TimeoutWorker is interrupted
     * (if waiting for a task) in order to cleanly shutdown. <p> This
     * implementation is thread-safe.
     *
     * @param running Whether the TaskWorker should run
     */
    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (!running) {
            this.notify();
        }
    }

    /**
     * The run() method of the {@link TimeoutWorker}. Here, every existing
     * Timeout is being waited upon until either cancelled or expired. If a
     * timeout expires, the {@link Timeout#expired()} method is called on that
     * timeout. <p> Every insertion or removal of Timeouts causes the
     * TimeoutWorker to wake up from waiting, recalculate the shortest timeout
     * to wait for, and go back to sleep.
     *
     */
    @Override
    public void run() {
        logger.config("Initializing...");
        running = true;
        runLoop:
        while (running) {
            synchronized (this) {
                while (timeouts.isEmpty()) {
                    logger.finest("Waiting for timeout");
                    // Check whether someone asked us to shutdown
                    // If its the case, and as the queue is empty
                    // we are free to break from the main loop and
                    // call shutdown();
                    if (!running) {
                        break runLoop;
                    }
                    try {
                        this.wait();
                    } catch (InterruptedException ie) {
                        logger.log(Level.INFO, "TimeoutWorker lock interrupted on empty container", ie);
                    }
                }
                // At least one timeout was added, loop until timeouts are empty
                while (!timeouts.isEmpty()) {
                    updateCurrentTimeout();
                    // It could be the case that while getting the next time to
                    // wait on, all timeouts have been removed, if this is the
                    // case, break on waiting, otherwise the thread is stuck on
                    // waiting forever
                    if (timeouts.isEmpty()) {
                        break;
                    }
                    try {
                        logger.log(Level.FINEST, "Waiting at {0} until: {1}",
                                new Object[]{System.currentTimeMillis(),
                                    System.currentTimeMillis() + waitTime});
                        this.wait(waitTime);
                    } catch (InterruptedException ie) {
                        logger.log(Level.INFO, "TimeoutWorker lock interrupted while waiting on timeout", ie);
                    }
                    // Here, either the timeout expired or the lock was notified
                    // due to at least one new timeout being added
                    if (!running) {
                        break runLoop;
                    }
                }
                logger.finest("Out of timeoutWait loop, no more timeouts");
            }
        }
        shutdown();
    }

    /**
     * Shutdown procedure. This method is called if the {@link TimeoutWorker}
     * was asked to shutdown; it cleanly process the shutdown procedure. <p>
     * This implementation is thread-safe.
     */
    private synchronized void shutdown() {
        logger.config("Shutting down...");
        logger.log(Level.FINEST,
                "Processed {0} timeouts, {1} expired, {2} cancelled",
                new Object[]{inserted, expired, cancelled});
        timeouts.clear();
    }

    /**
     * This method is called every time the TimeoutWorker thread is interrupted,
     * either due to insertion, removal, or an expiration of a {@link Timeout}.
     * Calling this method checks the current Timeout for expiration and if it
     * has expired, calls its {@link Timeout#expired()} method. It then tries to
     * get the next Timeout to be waited on, cancelling any already expired
     * timeouts while doing so, and calculating the minimum wait time for the
     * TimeoutWorker to wait for.
     */
    private void updateCurrentTimeout() {
        if ((currentTO = timeouts.getMin()) == null) {
            return;
        }

        while (currentTO.isExpired() && !timeouts.isEmpty()) {
            logger.log(Level.FINEST, "Timeout expired at {0}, expiring at:{1}",
                    new Object[]{System.currentTimeMillis(),
                        currentTO.getDelta()});
            currentTO.expired();
            if (timeouts.remove(currentTO)) {
                expired++;
            } else {
                logger.info("Trying to remove already removed timeout");
            }

            if ((currentTO = timeouts.getMin()) == null) {
                return;
            }
        }
        // We either have no remaining timeout or one timeout we should wait
        // on. Calculate the new minimum waiting time
        waitTime = currentTO.getDelta() - System.currentTimeMillis();
    }
}
