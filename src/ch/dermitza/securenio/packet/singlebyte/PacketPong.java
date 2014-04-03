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

import java.nio.ByteBuffer;

/**
 * A Pong packet implementation
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public final class PacketPong extends SimplePacket {

    private final byte[] bytes;

    /**
     * Create a new PONG packet.
     */
    public PacketPong() {
        super("PONG");
        bytes = new byte[1];
        bytes[0] = (byte)getHeader();
    }

    @Override
    public ByteBuffer toBytes() {
        return ByteBuffer.wrap(bytes);
    }

    @Override
    public short getHeader() {
        return PONG;
    }
}
