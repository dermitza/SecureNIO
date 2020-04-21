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

/**
 *
 * @author K. Dermitzakis
 * @version 0.18
 */
public interface TestPacketIF {
    
    public long getLong();
    public void setLong(long value);
    
    public byte getByte();
    public void setByte(byte value);
    
    public float getFloat();
    public void setFloat(float value);
    
    public boolean hasString();
    public String getString();
    public void setString(String value);
    
}
