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
package ch.dermitza.securenio.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A minimal {@link LogRecord} formatter for the SecureNIO library. Inspired by
 * GLOG (https://code.google.com/p/google-glog/), prints minimal information to
 * a console handler. TODO: setup filehandler.
 * 
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.19
 */
public class MiniFormatter extends Formatter {

    private static final DateFormat df = new SimpleDateFormat("MMdd HH:mm:ss.SSS");
    private static final int BASE_MESSAGE_LENGTH =
            1 // Level char.
            + 4 // Month + day
            + 1 // space
            + 12 // Timestamp
            + 1 // space
            + 1 // THREAD
            + 4 // Room for thread ID.
            + 1; // space

    @Override
    public String format(LogRecord lr) {
        char c;
        switch(lr.getLevel().intValue()){
            case 1000:
                c = 'F'; // Indicates a fatal exception generally paired with
                break; // actions to shut down the errored process.
            case 900:
                c = 'E'; // Indicates an unexpected error.
                break;
            case 800:
                c = 'W'; // Indicates a warning message likely worth of attention.
                break;
            case 700:
                c = 'I'; // Indicates a message of general interest.
                break;
            default:
                c = 'D'; // Indicates the message is for debugging purposes only.
                break;
        }       
        
        String message = formatMessage(lr);
        int messageLength = BASE_MESSAGE_LENGTH
                + 2 // Colon and space
                + message.length();
        String className = lr.getSourceClassName();
        String methodName = null;
        if (className != null) {
            messageLength += className.length();
            methodName = lr.getSourceMethodName();
            if (methodName != null) {
                messageLength += 1;  // Period between class and method.
                messageLength += methodName.length();
            }
        }
        
        StringBuilder sb = new StringBuilder(messageLength)
                .append(c)
                .append(df.format(new Date(lr.getMillis())))
                .append(" T")
                .append(lr.getThreadID());

        if(className != null){
            sb.append(' ').append(className);
            if(methodName != null){
                sb.append('.').append(methodName);
            }
        }
        sb.append(": ").append(message);

        Throwable tr = lr.getThrown();
        if (tr != null) {
            sb.append(System.getProperty("line.separator"));
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                tr.printStackTrace(pw);
                sb.append(sw.toString());
            }
        }

        return sb.append(System.getProperty("line.separator")).toString();
    }
}
