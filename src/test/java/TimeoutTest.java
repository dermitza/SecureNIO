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
import org.junit.Test;

/**
 * @author K. Dermitzakis
 * @version 0.18
 */
public class TimeoutTest {

    @Test
    public void test() {
        Timeout t1 = new Timeout(null, null, 1500);
        System.out.println("Timeout 1: " + t1.getTimeout());
        System.out.println("Created 1: " + t1.getCreated());
        System.out.println("Delta1   : " + t1.getDelta());
        Timeout t2 = new Timeout(null, null, 1500);
        System.out.println("Timeout 2: " + t2.getTimeout());
        System.out.println("Created 2: " + t2.getCreated());
        System.out.println("Delta2   : " + t2.getDelta());
        System.out.println("T1CT2    : " + t1.compareTo(t2));
        System.out.println("T2CT1    : " + t2.compareTo(t1));
    }

}
