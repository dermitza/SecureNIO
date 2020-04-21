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

import ch.dermitza.securenio.TCPServer;
import ch.dermitza.securenio.packet.PacketIF;
import ch.dermitza.securenio.packet.PacketListener;
import ch.dermitza.securenio.packet.singlebyte.PacketPong;
import ch.dermitza.securenio.packet.singlebyte.SimplePacket;
import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.packet.worker.SimplePacketWorker;
import ch.dermitza.securenio.socket.SocketIF;
import ch.dermitza.securenio.util.logging.LoggerHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 *
 * @author K. Dermitzakis
 * @version 0.20
 * @since   0.18
 */
public class ServerTest implements PacketListener {

    private final TCPServer server;

    public ServerTest(InetAddress address, int port,
            AbstractPacketWorker packetWorker, boolean usingSSL,
            boolean needClientAuth) {
        server = new TCPServer(address, port, packetWorker, usingSSL,
                needClientAuth);
        if (usingSSL) {
            String trustStoreLoc = null;
            char[] tsPassPhrase = null;
            if(needClientAuth){
                trustStoreLoc = "clientPublic.jks";
                tsPassPhrase = "clientPublic".toCharArray();
            }
            server.setupSSL(trustStoreLoc, "server.jks", tsPassPhrase,
                    "server".toCharArray());
        }
        server.addListener(this);
        new Thread(server, "ServerThread").start();
    }

    @Override
    public void paketArrived(SocketIF channel, PacketIF packet) {
        //System.out.println("Packet arrived: " + packet.getPacketName());
        if (packet.getHeader() == SimplePacket.PING) {
            server.send(channel, new PacketPong());
        }
    }

    public static void main(String[] args) {
        LoggerHandler.setLevel(Level.ALL);
        //System.setProperty("javax.net.debug", "all");
        InetAddress a = null;
        try {
            a = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException uhe) {
        }
        SimplePacketWorker pw = new SimplePacketWorker();
        ServerTest s = new ServerTest(null, 44503, pw, true, false);
        //ServerTest s = new ServerTest(a, 44503, true);
        try {
            Thread.sleep(40000);
        } catch (InterruptedException ex) {
        }
        //s.server.setRunning(false);
        //try {
        //    Thread.sleep(10000);
        //} catch (InterruptedException ex) {

        //}
        //Collection<StackTraceElement[]> c = Thread.getAllStackTraces().values();
        //for(StackTraceElement[] e: c){
        //    for(int i=0; i < e.length; i++){
        //        System.out.println(e[i]);
        //    }
        //}

    }
}
