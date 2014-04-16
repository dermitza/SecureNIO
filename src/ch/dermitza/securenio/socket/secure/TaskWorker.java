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

import ch.dermitza.securenio.util.logging.LoggerHandler;
import java.util.ArrayDeque;
import java.util.logging.Logger;

/**
 * A threaded implementation of a worker processing tasks required by an
 * {@link javax.net.ssl.SSLEngine} that is associated with a
 * {@link SecureSocket}. <p> The worker sequentially processes tasks that need
 * to be completed, in a FIFO fashion, notifying the associated
 * {@link TaskListener} once they have been completed. The {@link TaskWorker} is
 * otherwise waiting for incoming tasks via the {@link #addSocket(SecureSocket)}
 * method.
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class TaskWorker implements Runnable {

    private static final Logger logger = LoggerHandler.getLogger(TaskWorker.class.getName());
    private final ArrayDeque<SecureSocket> queue = new ArrayDeque<>();
    //private final ArrayList<TaskListener> listeners = new ArrayList<>();
    private boolean running = false;
    private final TaskListener listener;

    /**
     * Create a {@link TaskWorker} instance with a single {@link TaskListener}
     * reference. The {@link TaskListener} is notified whenever any task has
     * finished being processed by the TaskWorker.
     *
     * @param listener The {@link TaskListener} to be notified of completed
     * tasks
     */
    public TaskWorker(TaskListener listener) {
        this.listener = listener;
    }

    /**
     * Add a {@link SecureSocket} with an underlying
     * {@link javax.net.ssl.SSLEngine} that requires a task to be run. Tasks are
     * run in a FIFO queue according to order of socket insertion.
     *
     * @param socket The SecureSocket that requires a task to be run
     */
    public void addSocket(SecureSocket socket) {
        synchronized (queue) {
            queue.add(socket);
            //fireListenersNeeded(task.getSocket());
            queue.notify();
        }
    }

    /**
     * The run() method of the {@link TaskWorker}. Here, sequential processing
     * of {@link javax.net.ssl.SSLEngine} tasks that need to be completed is
     * done in a FIFO fashion, notifying the associated {@link TaskListener}
     * once they have been completed. The {@link TaskWorker} is otherwise
     * waiting for incoming tasks via the {@link #addSocket(SecureSocket)}
     * method.
     */
    @Override
    public void run() {
        logger.config("Initializing...");
        running = true;
        SecureSocket socket;
        runLoop:
        while (running) {
            // Wait for data to become available
            synchronized (queue) {
                while (queue.isEmpty()) {
                    // Check whether someone asked us to shutdown
                    // If its the case, and as the queue is empty
                    // we are free to break from the main loop and
                    // call shutdown();
                    if (!running) {
                        break runLoop;
                    }
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                    }
                }
                // Queue has some data here
                // get the first instance
                socket = queue.remove();
            }
            // Run the runnable in this thread
            Runnable r;
            while ((r = socket.getEngine().getDelegatedTask()) != null) {
                r.run();
            }
            // Runnable finished running here, signal the listener
            listener.taskComplete(socket);
        }
        shutdown();
    }

    /**
     * Check whether the {@link TaskWorker} is running.
     *
     * @return true if it is running, false otherwise
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Set the running status of the {@link TaskWorker}. If the running status
     * of the worker is set to false, the TaskWorker is interrupted (if waiting
     * for a task) in order to cleanly shutdown.
     *
     * @param running Whether the TaskWorker should run or not
     */
    public void setRunning(boolean running) {
        this.running = running;
        // If the worker is already blocked in queue.wait()
        // and someone asked us to shutdown,
        // we should interrupt it so that it shuts down
        // after processing all possible pending requests
        if (!running) {
            synchronized (queue) {
                queue.notify();
            }
        }
    }

    /**
     * Shutdown procedure. This method is called if the {@link TaskWorker} was
     * asked to shutdown; it cleanly process the shutdown procedure.
     */
    private void shutdown() {
        logger.config("Shutting down...");
        // Clear the queue
        queue.clear();
        // Remove all listener references
        //listeners.clear();
    }
}
