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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A static handler for all {@link Logger}s for the SecureNIO library. It sets
 * all loggers as children of a top level handler ("SecureNIO") and applies
 * appropriate formatting for them.
 * 
 * @author K. Dermitzakis
 * @version 0.19
 * @since   0.19
 */
public class LoggerHandler {

    private static final Logger top = Logger.getLogger("SecureNIO");

    static {
        Handler[] handlers = top.getHandlers();
        for (Handler handler : handlers) {
            top.removeHandler(handler);
        }
        MiniFormatter formatter = new MiniFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        top.addHandler(consoleHandler);
    }
    
    /**
     * Get a {@link Logger}. This is a pass-through implementation that ensures
     * all loggers for SecureNIO are children of the parent ("SecureNIO") logger.
     * 
     * @param name The name to assign to the logger
     * @return The returned Logger
     */
    public static Logger getLogger(String name){
        Logger l = Logger.getLogger(name);
        l.setParent(top);
        return Logger.getLogger(name);
    }
    
    /**
     * Sets the logging level to all {@link Logger} children of the parent
     * ("SecureNIO") logger and to all their handlers.
     * 
     * @param level The {@link Level} to set the loggers to
     */
    public static void setLevel(Level level){
        top.setLevel(level);
        Handler[] handlers = top.getHandlers();
        // Publish changes to handlers
        for(Handler handler: handlers){
            handler.setLevel(level);
        }
    }
}
