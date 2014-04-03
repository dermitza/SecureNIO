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
package ch.dermitza.securenio.test;

import ch.dermitza.securenio.socket.timeout.worker.Timeout;
import ch.dermitza.securenio.socket.timeout.worker.TimeoutWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class TimeoutWorkerTest {

    public static void main(String[] args) {
        int minTO = 500;
        int maxTO = 1500;
        int timeoutCnt = 1000;
        int removalCnt = timeoutCnt / 5;
        boolean sleep = false;
        long sleepTime = 50;
        ArrayList<Timeout> timeouts = new ArrayList<>(timeoutCnt);
        Random r = new Random();
        double period_ms;
        long start;

        // Make our timeouts
        for (int i = 0; i < timeoutCnt; i++) {
            int randTO = minTO + r.nextInt(maxTO - minTO + 1);
            timeouts.add(new Timeout(null, null, randTO));
        }

        TimeoutWorker w = new TimeoutWorker();
        new Thread(w).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }

        start = System.nanoTime();
        for (int i = 0; i < timeoutCnt; i++) {
            w.insert(timeouts.get(i));
            if (sleep) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                }
            }
        }
        period_ms = (System.nanoTime() - start) / 1000000d;
        System.out.println("Added " + timeoutCnt + " timeouts in " + period_ms + "ms");

        /* Randomize the removals */
        Collections.shuffle(timeouts, r);
        start = System.nanoTime();
        for (int i = 0; i < removalCnt; i++) {
            if(!timeouts.get(i).hasExpired()){
                // timeout.expire() was NOT called;
                w.cancel(timeouts.get(i));
            }
            //if (sleep) {
            //    try {
            //        Thread.sleep(sleepTime);
            //    } catch (InterruptedException ie) {
            //   }
            //}
        }
        period_ms = (System.nanoTime() - start) / 1000000d;
        System.out.println("Removed " + removalCnt + " timeouts in " + period_ms + "ms");

        // and shutdown
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) {
        }
        w.setRunning(false);
    }
}
