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

import ch.dermitza.securenio.packet.worker.AbstractPacketWorker;
import ch.dermitza.securenio.packet.worker.SimplePacketWorker;
import ch.dermitza.securenio.util.logging.LoggerHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 *
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.18
 */
public class ClientTest implements Runnable {

    private final InetAddress address;
    private final int port;
    private final int maxPackets;
    private final int maxClients;
    private final boolean usingSSL;
    private final boolean needClientAuth;
    private AbstractPacketWorker packetWorker;
    private ClientListener[] listeners;
    private AbstractPacketWorker[] workers;
    private boolean[] running;
    private boolean[] success;
    long start;
    long elapsed;

    public ClientTest(InetAddress address, int port, boolean usingSSL,
            boolean needClientAuth, int maxPackets, int maxClients) {
        this.address = address;
        this.port = port;
        this.usingSSL = usingSSL;
        this.needClientAuth = needClientAuth;
        this.maxPackets = maxPackets;
        this.maxClients = maxClients;
    }
    
    public void invalidateSessions(){
        for( int i = 0; i < maxClients; i++){
            listeners[i].getClient().invalidateSession();
        }
    }

    public void initClients() {
        listeners = new ClientListener[maxClients];
        workers = new SimplePacketWorker[maxClients];
        running = new boolean[maxClients];
        success = new boolean[maxClients];
        start = System.currentTimeMillis();
        for (int i = 0; i < maxClients; i++) {
            workers[i] = new SimplePacketWorker();
            listeners[i] = new ClientListener(address, port, workers[i],
                    usingSSL, needClientAuth, maxPackets);
            running[i] = true;
            success[i] = false;
        }
        elapsed = System.currentTimeMillis() - start;
        System.out.println("Created " + maxClients + " clients in " + elapsed + "ms");
    }

    public void initSend() {
        start = System.currentTimeMillis();
        for (int i = 0; i < maxClients; i++) {
            listeners[i].getClient().send(listeners[i].getTestPacket(0));
        }
    }

    @Override
    public void run() {
        mainLoop:
        while (true) {
            // update running status
            for (int i = 0; i < maxClients; i++) {
                running[i] = listeners[i].clientRunning();
                success[i] = listeners[i].success();
            }

            boolean tmp = true;
            // check running status
            int succ = 0;
            for (int i = 0; i < maxClients; i++) {
                if (running[i]) {
                    tmp = true;
                    break;
                } else {
                    tmp = false;
                    if(success[i]){
                        succ++;
                    }
                }
            }
            if (!tmp) {
                // Everything finished running
                elapsed = System.currentTimeMillis() - start;
                System.out.println("Sent " + maxPackets * maxClients + " packets in " + elapsed + "ms");
                System.out.println("Connections: " + succ + " succeeded, " + (maxClients-succ) + " failed.");
                break;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
            }
        }
    }

    public static void main(String[] args) {
        //System.setProperty("javax.net.debug", "all");
        LoggerHandler.setLevel(Level.CONFIG);
        int clientNo = 100;
        int packetNo = 1;
        long start;
        long elapsed;

        InetAddress a = null;
        try {
            a = InetAddress.getByName("127.0.0.1");
            //a = InetAddress.getByName("alpharesearch.org");
            //a = InetAddress.getByName("192.168.1.11");
        } catch (UnknownHostException uhe) {
        }
        ClientTest ct = new ClientTest(a, 44503, false, false, packetNo, clientNo);
        //ClientTest ct = new ClientTest(a, 443, true, packetNo, clientNo);
        // init x clients
        ct.initClients();
        // Wait a while for all threads to kick off
        // offer 10ms per thread
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }
        // start the timer check thread
        new Thread(ct).start();
        // Kick off exchanges
        //try {
        //    Thread.sleep(5000);
        //} catch (InterruptedException ie) {
        //}
        ct.initSend();
        try {
            Thread.sleep(150);
        } catch (InterruptedException ie) {
        }
        //ct.invalidateSessions();
        //for (int i = 0; i < 30; i++) {
        //    try {
        //        Thread.sleep(1000);
        //    } catch (InterruptedException ie) {
        //    }
        //    ct.initSend();
        //}
    }
}
