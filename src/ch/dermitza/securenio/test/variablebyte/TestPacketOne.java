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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public class TestPacketOne extends AbstractTestPacket implements TestPacketIF {

    private long longValue;
    private byte byteValue;
    private float floatValue;
    private String stringValue;

    //-------------------------- PACKETIF METHODS ----------------------------//
    @Override
    public short getHeader() {
        return AbstractTestPacket.TYPE_ONE;
    }

    @Override
    public void reconstruct(ByteBuffer source) {
        byte[] bytes = source.array();
        this.longValue = ((bytes[0] & 0xFFL) << 56)
                | ((bytes[1] & 0xFFL) << 48)
                | ((bytes[2] & 0xFFL) << 40)
                | ((bytes[3] & 0xFFL) << 32)
                | ((bytes[4] & 0xFFL) << 24)
                | ((bytes[5] & 0xFFL) << 16)
                | ((bytes[6] & 0xFFL) << 8)
                | ((bytes[7] & 0xFFL));
        this.byteValue = bytes[8];
        int intBits = ((bytes[9] & 0xFF) << 24)
                | ((bytes[10] & 0xFF) << 16)
                | ((bytes[11] & 0xFF) << 8)
                | ((bytes[12] & 0xFF));
        this.floatValue = Float.intBitsToFloat(intBits);
        int stringLen = bytes[13]; // String can be up to 128 single-byte characters;
        this.stringValue = new String(bytes, 14, stringLen, Charset.forName("UTF-8"));
        System.out.println("Length: " + bytes.length + " bytes, longValue: "
                + longValue + " byteValue: " + byteValue + " floatValue: "
                + floatValue + " stringValue: " + stringValue);
    }

    @Override
    public ByteBuffer toBytes() {
        byte[] bytes;
        //  long size + byte size + float size + string size ref + string size
        int length = 8 + 1 + 4 + 1 + this.stringValue.getBytes(Charset.forName("UTF-8")).length;
        // + header + payload size (total 3 bytes)
        bytes = new byte[length+3];
        bytes[0] = TYPE_ONE;
        bytes[1] = (byte) ((length >> 8) & 0xFF);
        bytes[2] = (byte) (length & 0xFF);
        bytes[3] = (byte) (longValue >>> 56);
        bytes[4] = (byte) (longValue >>> 48);
        bytes[5] = (byte) (longValue >>> 40);
        bytes[6] = (byte) (longValue >>> 32);
        bytes[7] = (byte) (longValue >>> 24);
        bytes[8] = (byte) (longValue >>> 16);
        bytes[9] = (byte) (longValue >>> 8);
        bytes[10] = (byte) (longValue);
        bytes[11] = byteValue;
        int intBits = Float.floatToIntBits(floatValue);
        bytes[12] = (byte) (intBits >>> 24);
        bytes[13] = (byte) (intBits >>> 16);
        bytes[14] = (byte) (intBits >>> 8);
        bytes[15] = (byte) (intBits);
        bytes[16] = (byte) this.stringValue.getBytes(Charset.forName("UTF-8")).length;
        System.arraycopy(stringValue.getBytes(Charset.forName("UTF-8")), 0, bytes, 17, bytes[16]);
        
        return ByteBuffer.wrap(bytes);
    }

    //------------------------TESTPACKETIF METHODS ---------------------------//
    @Override
    public long getLong() {
        return this.longValue;
    }

    @Override
    public void setLong(long value) {
        this.longValue = value;
    }

    @Override
    public byte getByte() {
        return this.byteValue;
    }

    @Override
    public void setByte(byte value) {
        this.byteValue = value;
    }

    @Override
    public float getFloat() {
        return this.floatValue;
    }

    @Override
    public void setFloat(float value) {
        this.floatValue = value;
    }

    @Override
    public boolean hasString() {
        return ((this.stringValue != null) ? true : false);
    }

    @Override
    public String getString() {
        return this.stringValue;
    }

    @Override
    public void setString(String value) {
        this.stringValue = value;
    }
}
