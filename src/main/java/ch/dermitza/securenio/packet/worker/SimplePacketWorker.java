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

import ch.dermitza.securenio.packet.singlebyte.PacketPing;
import ch.dermitza.securenio.packet.singlebyte.PacketPong;
import ch.dermitza.securenio.packet.singlebyte.PacketUnknown;
import ch.dermitza.securenio.packet.singlebyte.SimplePacket;
import ch.dermitza.securenio.socket.SocketIF;

import java.nio.ByteBuffer;

/**
 * A packet worker implementation that ONLY handles single-byte packets.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class SimplePacketWorker extends AbstractPacketWorker {

    @Override
    protected void processData() {
        SimplePacket packet;
        synchronized (this.pendingSockets) {
            while (!pendingSockets.isEmpty()) {
                // Peek at the first element
                SocketIF socket = pendingSockets.getFirst();
                // See if this socket has data
                ByteBuffer data = pendingData.get(socket);
                if (data == null) {
                    // This socket is not pending, remove it from the queue
                    pendingSockets.poll();
                    // Go for the next socket
                    break;
                }
                // Prepare the buffer
                data.flip();
                // This socket has some data
                while (data.hasRemaining()) {
                    // read the first byte
                    byte head = data.get();
                    // based on the first byte, create a new packet
                    switch (head) {
                        case SimplePacket.PING:
                            packet = new PacketPing();
                            // This simple packet is reconstructed, send to all listeners
                            fireListeners(socket, packet);
                            break;
                        case SimplePacket.PONG:
                            packet = new PacketPong();
                            // This simple packet is reconstructed, send to all listeners
                            fireListeners(socket, packet);
                            break;
                        case SimplePacket.UNKNOWN:
                            packet = new PacketUnknown();
                            // Unknown packet
                            fireListeners(socket, packet);
                            break;
                    }
                }
                // No more data in the buffer, clear it
                data.clear();
                // no more data on this socket, remove it
                pendingData.remove(socket);
                pendingSockets.removeFirst();
            }
        }
    }
}
