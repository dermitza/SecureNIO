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

import ch.dermitza.securenio.socket.timeout.worker.Timeout;
import ch.dermitza.securenio.util.MinContainer;
import org.junit.Test;

import java.util.Random;

/**
 *
 * @author K. Dermitzakis
 * @version 0.18
 */

public class TimeoutContainerTest {

    @Test
    public void test() {
        int minTO = 50;
        int maxTO = 1500;
        int timeoutCnt = 10000;
        int trials = 10;
        boolean sleep = false;
        long sleepTime = 50;
        MinContainer<Timeout> c = new MinContainer<>();
        Timeout[] timeouts = new Timeout[timeoutCnt];
        Random r = new Random();
        double period_ms;
        long start = System.nanoTime();
        for (int d = 0; d < trials; d++) {
            for (int i = 0; i < timeoutCnt; i++) {
                int randTO = minTO + r.nextInt(maxTO - minTO + 1);
                timeouts[i] = new Timeout(null, null, randTO);
                c.add(timeouts[i]);
                if (sleep) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        period_ms = (System.nanoTime() - start) / 1000000d;
        System.out.println("Took " + period_ms/trials + "ms to create and add " + timeoutCnt + " timeouts over " + trials + " trials");
        start = System.nanoTime();
        for (int d = 0; d < trials; d++) {
            //System.out.println("Trial " + d);
            for (int i = 0; i < timeoutCnt; i++) {
                //System.out.println("Timeout " + i);
                c.remove(timeouts[i]);
                if (sleep) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        period_ms = (System.nanoTime() - start) / 1000000d;
        System.out.println("Took " + period_ms/trials + "ms to remove " + timeoutCnt + " timeouts over " + trials + " trials");
    }
}
