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

import ch.dermitza.securenio.socket.SocketIF;
import java.nio.ByteBuffer;

/**
 * An abstract, extensible implementation of a variable length packet worker.
 * This worker extends {@link AbstractPacketWorker} to support variable length
 * packets. <p> For the default implementation, packets should extend
 * {@link ch.dermitza.securenio.packet.PacketIF} and have a 3-byte header: 1
 * byte for packet designation and 2 bytes for message length. This results in
 * 255 packet types, with maximum packet length of 32,767 bytes if using java
 * shorts or 65,535 bytes if using an unsigned length implementation. NOTE:
 * packet length excludes the 3-byte header, i.e. is payload length. If more
 * packet types or a larger packet (payload) size is needed, use the argument
 * constructor. Note that the largest header size the constructor can take is 2
 * bytes, and the length variable can either be 2 or 4 bytes, representing java
 * shorts and ints respectively. <p>
 *
 * Application code should extend the singular method
 * {@link #assemblePacket(ch.dermitza.securenio.socket.SocketIF, short, byte[])}
 * to assemble the reconstructed packet and subsequently
 * {@link #fireListeners(SocketIF, ch.dermitza.securenio.packet.PacketIF)} with
 * the reconstructed packet.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public abstract class VariableLengthPacketWorker extends AbstractPacketWorker {

    /**
     * The default header length
     */
    public static final int HEADER_LENGTH = 1;
    /**
     * The default packet size length
     */
    public static final int SIZE_LENGTH = 2;
    private final int headerLength;
    private final int sizeLength;

    /**
     * Initializes this packet worker to work with packets having a custom
     * header and payload length.
     *
     * @param singleByte true if the header is one byte, false if it is 2 bytes
     * long
     * @param shortSize true if the payload length is 2 bytes, false if it is 4
     * bytes long
     */
    public VariableLengthPacketWorker(boolean singleByte, boolean shortSize) {
        this.headerLength = singleByte ? HEADER_LENGTH : 2;
        this.sizeLength = shortSize ? SIZE_LENGTH : 4;
    }

    /**
     * Initializes this packet worker to work with packets having a 1 byte
     * header and 2 bytes payload length.
     */
    public VariableLengthPacketWorker() {
        this(true, true);
    }

    /**
     * Process data received on pending sockets, in a FIFO fashion. If data
     * received is not enough to reconstruct a packet, the socket's pending
     * status is removed, as we cannot do further processing on it. The socket's
     * pending status will be re-instantiated once more data on that particular
     * socket has been received. Sockets that have been completely drained from
     * data are also removed here.
     */
    @Override
    protected void processData() {
        synchronized (this.pendingSockets) {
            while (!pendingSockets.isEmpty()) {
                // Peek at the first element
                SocketIF socket = pendingSockets.getFirst();
                // See if this socket has data
                ByteBuffer data = pendingData.get(socket);
                if (data == null) {
                    //System.out.print("Socket has no data, removing");
                    // This socket is not pending, remove it from the queue
                    pendingSockets.removeFirst();
                    // Go for the next socket
                    break;
                }
                parseBuffer(socket, data);
            }
        }
    }

    /**
     * Parse a buffer with data on a socket. If there is enough data to
     * reconstruct a packet here, then
     * {@link #assemblePacket(SocketIF, short, byte[])} is called. If there is
     * not enough data to reconstruct a packet,
     * {@link #prepareForWait(ByteBuffer)} is called, and the socket's pending
     * status is removed.
     *
     * @param socket The socket we have some data to parse on
     * @param data The data that needs parsing
     *
     * @see #assemblePacket(SocketIF, short, byte[])
     * @see #prepareForWait(ByteBuffer)
     */
    private void parseBuffer(SocketIF socket, ByteBuffer data) {
        synchronized (this.pendingSockets) {
            //System.out.println("Preparing the buffer");
            // prepare the buffer. This should happen ONLY once
            //System.out.println("limit " + data.limit() + " position " + data.position());
            data.flip();
            //System.out.println("After flip: limit " + data.limit() + " position " + data.position());

            while (data.position() < data.limit()) {
                if (data.limit() - data.position() > (headerLength + sizeLength)) {
                    // get the first byte
                    // get the header
                    short head;
                    if (headerLength == 1) {
                        head = data.get();
                    } else {
                        head = data.getShort();
                    }
                    // get the message length
                    int len;
                    if (sizeLength == 2) {
                        len = data.getShort();
                    } else {
                        len = data.getInt();
                    }
                    //System.out.println("Payload Length: " + len);
                    if (data.limit() - data.position() >= len) {
                        // we have at least one complete packet, make a copy and
                        // send it for reassembly
                        byte[] bytes = new byte[len];
                        data.get(bytes);
                        assemblePacket(socket, head, bytes);
                        //System.out.println("Removing read bytes");
                        // Fix the buffer
                        //System.out.println("Limit: " + data.limit() + " position: " + data.position());
                        data.compact();
                        //System.out.println("After compact Limit: " + data.limit() + " position: " + data.position());
                        data.flip();
                        //System.out.println("After flip Limit: " + data.limit() + " position: " + data.position());
                        if (data.position() == data.limit()) {
                            //System.out.println("No more data, removing socket and buffer");
                            // no more data on this socket, remove it
                            pendingData.remove(socket);
                            pendingSockets.removeFirst();
                        }
                    } else {
                        //System.out.println("Not enough data to reconstruct packet");
                        //System.out.println("Data length: " + (data.limit() - data.position()));
                        prepareForWait(data);
                        break;
                    }
                } else {
                    //System.out.println("Not enough data to read yet");
                    prepareForWait(data);
                    break;
                }
            }
            //System.out.println("loop was broken");
        }
    }

    /**
     * This method is called if we have read some data from the buffer on a
     * particular socket during {@link #processData()}, but the buffer does not
     * have enough data received to reconstruct a complete packet. In this case,
     * we need to reset the buffer in a configuration where it can receive
     * additional data. Furthermore, as the socket we read data on does still
     * not have enough data to reconstruct a complete packet, we remove it from
     * being pending, as it is not pending for further processing at this point.
     * It will become pending again in subsequent data reception.
     *
     * @param data The ByteBuffer to reset in a writable configuration
     */
    private void prepareForWait(ByteBuffer data) {
        //System.out.println("Limit: " + data.limit() + " position: " + data.position());
        // set the position to the limit
        data.position(data.limit());
        //System.out.println("Setting position = limit: " + data.limit() + " position: " + data.position());
        data.flip();
        //System.out.println("After flip: Limit: " + data.limit() + " position: " + data.position());
        data.compact();
        //System.out.println("After compact: Limit: " + data.limit() + " position: " + data.position());
        pendingSockets.removeFirst();
        //System.out.println("PendingSockets isempty(): " + pendingSockets.isEmpty());
    }

    /**
     * This method is called from the packet worker when a complete packet has
     * been received. It should be extended to reconstruct the packet into what
     * the application code needs, and subsequently fire the packet listeners to
     * act on the packet received.
     *
     * @param socket The socket on which a complete packet was received
     * @param head The head of the packet
     * @param data The packet's payload
     *
     * @see #fireListeners(SocketIF, ch.dermitza.securenio.packet.PacketIF)
     */
    protected abstract void assemblePacket(SocketIF socket, short head, byte[] data);
}
