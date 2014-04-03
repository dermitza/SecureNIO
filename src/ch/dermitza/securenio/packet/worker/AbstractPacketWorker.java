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
package ch.dermitza.securenio.packet.worker;

import ch.dermitza.securenio.packet.PacketIF;
import ch.dermitza.securenio.packet.PacketListener;
import ch.dermitza.securenio.socket.SocketIF;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * An abstract, extensible implementation of a Packet Worker. This thread waits
 * for raw data arriving from remote peers through {@link SocketIF}s, adds them
 * to respective queues and then reassembles packets based on the data in these
 * queues. Each socket maintains a queue of data, for there is a possibility
 * that the packets received are fragmented and cannot be reassembled during
 * processing. In this case, processing should be delegated until additional
 * data on that socket arrives.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public abstract class AbstractPacketWorker implements Runnable {

    /**
     * The default buffer size (in bytes) per socket
     */
    public static final int BUFFER_SIZE = 512;
    private final ArrayList<PacketListener> listeners = new ArrayList<>();
    /**
     * Maps a SocketChannel to a list of ByteBuffer instances
     */
    protected final HashMap<SocketIF, ByteBuffer> pendingData = new HashMap<>();
    /**
     * Sockets that need to be operated upon (i.e. have pending operations)
     */
    protected final ArrayDeque<SocketIF> pendingSockets = new ArrayDeque<>();
    private boolean running = false;
    private byte[] tempArray;

    /**
     * Queue data received from a {@link SocketIF} for processing and
     * reconstruction. A deep copy of the passed {@link ByteBuffer} is made
     * locally. Following, a data queue for that {@link SocketIF} is created if
     * it does not exist, and the current piece of data received is added to
     * that queue for later processing and reconstruction.
     *
     * @param socket The SocketIF data was received from
     * @param data The ByteBuffer containing the data (bytes) received
     * @param count The number of bytes received
     *
     * @see #processData()
     */
    public void addData(SocketIF socket, ByteBuffer data, int count) {
        tempArray = new byte[count];
        System.arraycopy(data.array(), 0, tempArray, 0, count);
        synchronized (this.pendingSockets) {
            if (!pendingSockets.contains(socket)) {
                // Check that we do not add a socket twice. Once is enough
                // to trigger processing
                pendingSockets.add(socket);
            }
            synchronized (this.pendingData) {
                ByteBuffer buffer = this.pendingData.get(socket);
                if (buffer == null) {
                    // allocate a large enough buffer to hold the data we
                    // just received
                    int size = (count > BUFFER_SIZE) ? count : BUFFER_SIZE;
                    buffer = ByteBuffer.allocate(size);
                    this.pendingData.put(socket, buffer);
                }


                if (count > buffer.remaining()) {
                    int diff = count - buffer.remaining();
                    //System.out.println("Buffer needs resizing, remaining " + buffer.remaining() + " needed " + count + " difference " + diff);
                    //System.out.println("old size: " + buffer.capacity());
                    // Allocate a new buffer. To minimize new buffer allocation,
                    // we resize the buffer appropriately, in case this is a
                    // *really* busy channel. Notes: If it is an extremely busy
                    // channel, the buffer will keep growing, potentially making
                    // the worker run out of memory trying to continuously
                    // allocate larger and larger buffers. Also, a continuously
                    // growing buffer can also indicate that the underlying
                    // data is never or wrongly processed, that can indicate a
                    // problem with the end application.
                    int extSize = (diff > BUFFER_SIZE) ? diff : BUFFER_SIZE;
                    ByteBuffer temp = ByteBuffer.allocate(buffer.capacity() + extSize);
                    //System.out.println("new size: " + temp.capacity());
                    // Flip existing buffer to prepare for putting in the replacement
                    buffer.flip();
                    //System.out.println("pos " + buffer.position() + " lim " + buffer.limit() + " cap " + buffer.capacity());
                    // put existing buffer into the temporary replacement
                    temp.put(buffer);
                    // Remove the old reference
                    this.pendingData.remove(socket);
                    // Replace reference
                    buffer = temp;
                    // associate the new buffer with the socket
                    this.pendingData.put(socket, buffer);
                    // Buffer is now resized appropriately, let the data be
                    // added naturally
                }
                // Make a copy of the data
                buffer.put(tempArray);
                //System.out.println("pos " + buffer.position() + " lim " + buffer.limit() + " cap " + buffer.capacity());
            }
            pendingSockets.notify();
        }
    }

    /**
     * This is the main entry point of received data processing and reassembly.
     * This is left for the application layer to decide how to process raw
     * incoming data
     */
    protected abstract void processData();

    /**
     * The run() method of the {@link AbstractPacketWorker}. Here, sequential
     * processing of data that need to be reconstructed and processed should be
     * done in a FIFO fashion. The {@link AbstractPacketWorker} is otherwise
     * waiting for incoming tasks via the
     * {@link #addData(SocketIF, ByteBuffer, int)} method.
     */
    @Override
    public void run() {
        running = true;
        //System.out.println("PacketWorker running");

        runLoop:
        while (running) {
            // Wait for data to become available
            synchronized (pendingSockets) {
                while (pendingSockets.isEmpty()) {
                    // Check whether someone asked us to shutdown
                    // If its the case, and as the queue is empty
                    // we are free to break from the main loop and
                    // call shutdown();
                    if (!running) {
                        break runLoop;
                    }
                    try {
                        pendingSockets.wait();
                    } catch (InterruptedException e) {
                    }
                }
                // We have some data on a socket here
            }
            // Do something with the data here
            processData();
        }
        shutdown();
    }

    /**
     * Check whether the {@link AbstractPacketWorker} is running.
     *
     * @return true if it is running, false otherwise
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Set the running status of the {@link AbstractPacketWorker}. If the
     * running status of the worker is set to false, the AbstractPacketWorker is
     * interrupted (if waiting for a task) in order to cleanly shutdown.
     *
     * @param running Whether the PacketWorker should run or not
     */
    public void setRunning(boolean running) {
        this.running = running;
        // If the worker is already blocked in queue.wait()
        // and someone asked us to shutdown,
        // we should interrupt it so that it shuts down
        // after processing all possible pending requests
        if (!running) {
            synchronized (pendingSockets) {
                pendingSockets.notify();
            }
        }
    }

    /**
     * Shutdown procedure. This method is called if the
     * {@link AbstractPacketWorker} was asked to shutdown; it cleanly process
     * the shutdown procedure, clearing any queued data remaining and removing
     * all listener references.
     */
    private void shutdown() {
        // Clear the queue
        pendingData.clear();
        pendingSockets.clear();
        // Remove all listener references
        listeners.clear();
    }

    //----------------------- LISTENER METHODS -------------------------------//
    /**
     * Allows registration of multiple {@link PacketListener}s to this
     * {@link AbstractPacketWorker}.
     *
     * @param listener The listener to register to this PacketWorker
     */
    public void addListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Allows de-registration of multiple {@link PacketListener}s from this
     * {@link AbstractPacketWorker}.
     *
     * @param listener The listener to unregister from this PacketWorker
     */
    public void removeListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Once a {@link PacketIF} has been completely reconstructed, registered
     * listeners are notified via this method. This method creates a local copy
     * of the already registered listeners when firing events, to avoid
     * potential concurrent modification exceptions.
     *
     * @param socket The SocketIF a complete PacketIF is reconstructed
     * @param packet The completely reconstructed AbstractPacket
     *
     * @see PacketListener#paketArrived(SocketIF, PacketIF)
     */
    protected void fireListeners(SocketIF socket, PacketIF packet) {
        PacketListener[] temp;
        if (!listeners.isEmpty()) {
            temp = (PacketListener[]) listeners.toArray(new PacketListener[listeners.size()]);
            for (PacketListener listener : temp) {
                listener.paketArrived(socket, packet);
            }
        }
    }
}
