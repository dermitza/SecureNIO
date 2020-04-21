/**
 * This file is part of NIOTCP. Copyright (C) 2014 K. Dermitzakis
 * <dermitza@gmail.com>
 *
 * NIOTCP is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * NIOTCP is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with NIOTCP. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.dermitza.securenio.test.variablebyte;

import ch.dermitza.securenio.packet.PacketIF;
import ch.dermitza.securenio.packet.PacketListener;
import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.socket.SocketIF;
import java.net.InetAddress;

/**
 * A simplistic multi-socket client implementation listening for packets
 * 
 * @author K. Dermitzakis
 * @version 0.20
 * @since 0.19
 */
public class MultiSocketClientListener implements PacketListener {

    private MultiSocketClient client;
    private final int maxPackets;
    private int packets = 0;
    private final int socketNo;
    private boolean success = false;

    public MultiSocketClientListener(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean needClientAuth, int maxPackets, int socketNo) {
        client = new MultiSocketClient(address, port, packetWorker, usingSSL,
                needClientAuth, socketNo);
        this.socketNo = socketNo;
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

    public TestPacketOne getTestPacket(int count) {
        TestPacketOne p = new TestPacketOne();
        p.setByte((byte) 0xF1);
        p.setFloat(55.0123f);
        p.setLong(System.currentTimeMillis());
        p.setString("Packet " + count);
        return p;
    }

    @Override
    public void paketArrived(SocketIF socket, PacketIF packet) {
        //System.out.println("Packet arrived: " + packet.getPacketName());
        packets++;
        //System.out.println("Packets: " + packets);
        if (packets == (maxPackets*socketNo)) {
            success = true;
            // Enough packets, shutdown
            client.setRunning(false);
        } else if (packets % socketNo == 0) {
            if (client.isConnected()) {
                client.send(getTestPacket(packets));
            }
        }
    }

    public boolean success() {
        return this.success;
    }

    public boolean clientRunning() {
        return client.isRunning();
    }
    
    public boolean handshakesFinished(){
        return client.handshakesFinished();
    }

    public MultiSocketClient getClient() {
        return this.client;
    }
}
