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
package ch.dermitza.securenio.test.variablebyte;

import ch.dermitza.securenio.packet.worker.VariableLengthPacketWorker;
import ch.dermitza.securenio.socket.SocketIF;
import java.nio.ByteBuffer;

/**
 * A simplistic packet worker listening for {@link TestPacketIF}s.
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class TestPacketWorker extends VariableLengthPacketWorker {

    @Override
    protected void assemblePacket(SocketIF socket, short head, byte[] data) {
        switch (head) {
            case AbstractTestPacket.TYPE_ONE:
                TestPacketOne packet = new TestPacketOne();
                packet.reconstruct(ByteBuffer.wrap(data));
                fireListeners(socket, packet);
                break;
        }
    }
}
