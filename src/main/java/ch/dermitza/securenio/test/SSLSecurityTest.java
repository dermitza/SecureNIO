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

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class SSLSecurityTest {

    public static void main(String[] args) throws Exception {
        
        //System.err.println("Creating SSL context");
        char[] passphrase = "server".toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        //ks.load(new FileInputStream("test.jks"), passphrase);
        ks.load(new FileInputStream("server.jks"), passphrase);

        //System.err.println("Loaded keystore");
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        //System.err.println("Initialized trustManagerFactory");
        context.init(null, tmf.getTrustManagers(), null);
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setEnabledProtocols(new String[]{"SSLv3", "TLSv1.2"});
        String[] protocols = engine.getEnabledProtocols();
        System.out.println("===========PROTOCOLS=========");
        for (int i = 0; i < protocols.length; i++) {
            System.out.println(protocols[i]);
        }
        engine.setEnabledCipherSuites(new String[]{
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_RSA_WITH_AES_128_CBC_SHA"
                });
        //String[] suites = engine.getEnabledCipherSuites();
        String[] suites = engine.getSupportedCipherSuites();
        System.out.println("=============SUITES===========");
        for (int i = 0; i < suites.length; i++) {
            System.out.println(suites[i]);
        }
    }
}
