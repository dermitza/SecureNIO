# <h1 align=center>![SecureNIO](https://github.com/dermitza/SecureNIO/raw/master/SecureNIO.png) SecureNIO<h1>
## <h2 align=center>A minimal, non-blocking, Java NIO TCP framework supporting SSL/TLS<h2>
## Features
* _Non-blocking -_ Exclusively.
* _Minimal -_ Compiled .jar is 121KB small, only the essentials are included.
* _Scalable -_ Supporting thousands of concurrent sockets, optimized for small data-size, short-lived communication.
* _Small memory footprint -_ Server memory usage: ~10MB idle, ~200MB for 1000 concurrent two-way SSL/TLS clients.
* _Simple -_ Implement your custom packets extending a single interface, and override two simple methods to get started.
* _Well-documented -_ LOL
* _Extensible -_ Custom packets and packet workers supported.
* _SSL/TLS support -_ Supports both SSL/TLS encrypted and non-encrypted SocketChannels
* _One or two-way SSL/TLS authentication -_ You decide.
* _Configurable SSL/TLS protocols supported -_ via text file.
* _Configurable cipher suites supported -_ via text file.
* _Auto-scaling packet buffers -_ Never miss another application packet.
* _Variable length packets -_ Variable length application packets are supported.
* _Supports timeouts -_ Timeouts are internally used to disconnect expired SSL sessions, but can also be used extrinsically (e.g. to disconnect from a remote peer, to re-validate an SSL handshake and so on).

## Changes

* _v0.18 -_ First released version

## Binaries

All binaries are compiled using Java 1.7.0_09; Java HotSpot(TM) 64-Bit Server VM 23.5-b02 unless otherwise noted.

* _Version 0.18 -_ [SecureNIO\_v0.18.jar](https://github.com/dermitza/SecureNIO/raw/master/dist/SecureNIO_v0.18.jar)  [SecureNIO_v0.18_doc.zip](https://github.com/dermitza/SecureNIO/raw/master/dist/SecureNIO_v0.18_doc.zip)

## Documentation

### Examples

Two examples on how to use this framework are provided, each in their respective packages:

* _ch.dermitza.securenio.test.singlebyte -_ A simple, one byte long application packet client/server implementation
* _ch.dermitza.securenio.test.variablebyte -_ A simple, variable byte application packet client/server implementation

### Generating self-signed KeyStores (public and private keys) and TrustStores (public keys)

Sample server and client KeyStores and TrustStores have been provided and are ready to use. Needless to say, **do not use these for anything other than testing**. Steps to create a self-signed KeyStore (and certificates) and TrustStore for a server implementation:

1. Generate a server KeyStore
```
keytool -genkey -keyalg RSA -alias server -keystore server.jks -storepass server -validity 360 -keysize 2048
```
2. Extract the public key from the public-private key pair that you created
```
keytool -export -alias server -keystore server.jks -rfc -file serverPublic.cert
```
3. Create the truststore using the public key (advised to use a different password, e.g. serverPublic)
```
keytool -import -alias server -file serverPublic.cert -keystore serverPublic.jks -storetype JKS
```

You can now use the serverPublic.jks in your clients to authenticate the server. To generate a KeyStore and/or TrustStore for the client (two-way authentication), repeat the above steps using appropriate names (i.e. replace server with client where appropriate).

### JavaDoc

You can currently browse the JavaDoc through the zipped version provided above. Additional documentation (sometimes extensive) is included in the source files.

## License

GNU AFFERO GENERAL PUBLIC LICENSE Version 3

Copyright (C) 2014 K. Dermitzakis <dermitza@gmail.com>
 
SecureNIO is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

SecureNIO is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with SecureNIO. If not, see <http://www.gnu.org/licenses/>.
