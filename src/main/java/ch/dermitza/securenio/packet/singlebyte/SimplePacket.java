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
package ch.dermitza.securenio.packet.singlebyte;

import ch.dermitza.securenio.packet.PacketIF;

import java.nio.ByteBuffer;

/**
 * Simple packets are 1-byte packets not requiring any reconstruction.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public abstract class SimplePacket implements PacketIF {

    /**
     * The PING packet header
     */
    public static final byte PING = 0x01;
    /**
     * The PONG packet header
     */
    public static final byte PONG = 0x02;
    /**
     * The HEARTBEAT packet header
     */
    public static final byte HEARTBEAT = 0x03;
    /**
     * The UNKNOWN packet header
     */
    public static final byte UNKNOWN = 0x00;
    private final String packetName;

    /**
     * Create a new simple (one-byte long) packet
     * @param name The string representation (name) of the packet
     */
    public SimplePacket(String name) {
        this.packetName = name;
    }

    /**
     * Simple packets are 1-byte packets and thus do not require any
     * reconstruction
     *
     * @param source The data source to reconstruct the packet from
     */
    @Override
    public void reconstruct(ByteBuffer source) {
        // unused
    }

    /**
     * Get the name of the packet (String representation)
     * @return the name of the packet
     */
    public String getPacketName() {
        return this.packetName;
    }
}
