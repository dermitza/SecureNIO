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

import ch.dermitza.securenio.socket.SocketIF;

/**
 * Packet listeners listen for completely reassembled {@link PacketIF}s on their
 * respective {@link SocketIF}, and act upon them. Multiple packet listeners can
 * be registered with a single
 * {@link ch.dermitza.securenio.packet.worker.AbstractPacketWorker}.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public interface PacketListener {

    /**
     * This method is called from the
     * {@link ch.dermitza.securenio.packet.worker.AbstractPacketWorker} thread
     * once it has reassembled a complete {@link PacketIF} on a
     * {@link SocketIF}.
     *
     * @param socket The SocketIF that just had one complete packet reassembled
     * @param packet The reassembled packet on the SocketIF it arrived on
     */
    public void paketArrived(SocketIF socket, PacketIF packet);
}
