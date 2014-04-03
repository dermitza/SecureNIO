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
package ch.dermitza.securenio.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

/**
 * A simplistic file utility class used to read and load SSL/TLS protocols and
 * certificates from files. Note that currently it does not distinguish between
 * protocols or certificates as it is using the same underlying
 * {@link #readLines(java.lang.String)} method. However, error checking at the
 * {@link ch.dermitza.securenio.AbstractSelector} method will intervene if the
 * files do not exist or are incorrect
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class FileUtil {

    private static LineNumberReader lr;
    private static FileReader fr;
    private static ArrayList<String> lines;

    /**
     * Instantiation of this class is not allowed
     */
    private FileUtil() {
        // Disallow instantiation
    }

    /**
     * Read SSL/TLS protocols from a file. If it cannot read the file for any
     * reason null is returned
     *
     * @param file The file to read SSL/TLS protocols from
     * @return SSL/TLS protocols loaded from the file or null if there was a
     * problem loading them
     */
    public static String[] readProtocols(String file) {
        try {
            return readLines(file);
        } catch (IOException ioe) {
            System.err.println("Error reading protocols from: " + file);
            System.err.println(ioe.getMessage());
            return null;
        }
    }

    /**
     * Read SSL/TLS cipher suites from a file. If it cannot read the file for
     * any reason null is returned
     *
     * @param file The file to read SSL/TLS cipher suites from
     * @return SSL/TLS cipher suites loaded from the file or null if there was a
     * problem loading them
     */
    public static String[] readCipherSuites(String file) {
        try {
            return readLines(file);
        } catch (IOException ioe) {
            System.err.println("Error reading cipher suits from: " + file);
            System.err.println(ioe.getMessage());
            return null;
        }
    }

    /**
     * Generic read line method implementation from a file. This implementation
     * allows empty lines and comment lines beginning with #. The method is used
     * by both {@link #readProtocols(java.lang.String)} and
     * {@link #readCipherSuites(java.lang.String)} and as such, these methods do
     * not distinguish between protocols and cipher suites.
     *
     *
     * @param file The file to read lines from
     * @return A string array containing the valid lines read or null if there
     * was an error reading the lines
     * @throws IOException If there is any error while trying to read from the
     * file provided
     */
    private static String[] readLines(String file) throws IOException {
        String line;
        fr = new FileReader(file);
        lines = new ArrayList<>();
        lr = new LineNumberReader(fr);
        while ((line = lr.readLine()) != null) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                lines.add(line);
            }
        }
        fr.close();
        lr.close();
        String[] ret = lines.toArray(new String[0]);
        lines.clear();
        lines = null;
        fr = null;
        lr = null;
        return ret;
    }

    public static void main(String[] args) {
        String[] test = readProtocols("protocols");
        for (int i = 0; i < test.length; i++) {
            System.out.println(test[i]);
        }
        System.out.println(lines == null);
        test = readCipherSuites("cipherSuites");
        for (int i = 0; i < test.length; i++) {
            System.out.println(test[i]);
        }
    }
}
