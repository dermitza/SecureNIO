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
package ch.dermitza.securenio.test.singlebyte;

import ch.dermitza.securenio.TCPClient;
import ch.dermitza.securenio.packet.PacketIF;
import ch.dermitza.securenio.packet.PacketListener;
import ch.dermitza.securenio.packet.singlebyte.PacketPing;
import ch.dermitza.securenio.packet.singlebyte.SimplePacket;
import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.socket.SocketIF;
import java.net.InetAddress;

/**
 * A simplistic client implementation listening for packets
 * @author K. Dermitzakis
 * @version 0.20
 * @since   0.18
 */
public class ClientListener implements PacketListener {

    private TCPClient client;
    private final int maxPackets;
    private int packets = 0;
    private boolean success = false;

    public ClientListener(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean needClientAuth, int maxPackets) {
        client = new TCPClient(address, port, packetWorker, usingSSL,
                needClientAuth);
        if (usingSSL) {
            String keyStoreLoc = null;
            char[] ksPassPhrase = null;
            if (needClientAuth) {
                keyStoreLoc = "client.jks";
                ksPassPhrase = "client".toCharArray();
            }
            client.setupSSL("serverPublic.jks", keyStoreLoc,
                    "serverPublic".toCharArray(), ksPassPhrase);
        }
        client.addListener(this);
        this.maxPackets = maxPackets;
        new Thread(client).start();
    }

    @Override
    public void paketArrived(SocketIF socket, PacketIF packet) {
        System.out.println("Packet arrived: " + ((SimplePacket)packet).getPacketName());
        packets++;
        if (packets >= maxPackets) {
            success = true;
            // Enough packets, shutdown
            client.setRunning(false);
        } else {
            if (client.isConnected()) {
                client.send(socket, new PacketPing());
            }
        }
    }

    public boolean success() {
        return this.success;
    }

    public boolean clientRunning() {
        return client.isRunning();
    }

    public TCPClient getClient() {
        return this.client;
    }
}
