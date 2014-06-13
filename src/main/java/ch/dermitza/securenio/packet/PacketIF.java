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
package ch.dermitza.securenio.packet;

import java.nio.ByteBuffer;

/**
 * A network packet interface, containing required methods to create an
 * application-level packet, to reconstrust such a packet from raw bytes
 * received, and to get the contents of this packet as raw bytes. The
 * implementation specific details are application dependent.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public interface PacketIF {

    /**
     * Get the header of this packet
     *
     * @return The header of this packet, can be 1 or 2 bytes long
     */
    public short getHeader();

    /**
     * Reconstruct this PacketIF from the given ByteBuffer
     *
     * @param source The ByteBuffer to reconstruct this PacketIF from
     */
    public void reconstruct(ByteBuffer source);

    /**
     * Get the contents of this PacketIF as a ByteBuffer
     *
     * @return the contents of this PacketIF as a ByteBuffer
     */
    public ByteBuffer toBytes();
}
