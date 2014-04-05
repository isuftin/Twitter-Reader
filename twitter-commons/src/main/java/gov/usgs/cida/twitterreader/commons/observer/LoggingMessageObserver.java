package gov.usgs.cida.twitterreader.commons.observer;

import ch.qos.logback.classic.Logger;
import gov.usgs.cida.twitterreader.commons.queue.TwitterQueues;
import java.util.Observable;
import org.slf4j.LoggerFactory;

/**
 * Creates a message observer that receives and logs messages
 *
 * @author isuftin
 */
public class LoggingMessageObserver extends MessageObserver {

    private ch.qos.logback.classic.Logger logger;

    /**
     * Creates a LoggingEventObserver with a default logger
     */
    public LoggingMessageObserver() {
        this((Logger) LoggerFactory.getLogger(LoggingEventObserver.class));
    }

    /**
     * Creates a LoggingEventObserver using a provided Logger
     *
     * @param logger
     */
    public LoggingMessageObserver(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("Logger may not be null");
        }
        this.logger = logger;
    }

    @Override
    public void handleEvent(String messageObject) {
        logger.info(String.format("Incoming Message: %s", messageObject));
    }

    /**
     * Register this object to the event bus
     */
    @Override
    public void register() {
        TwitterQueues.registerObserver(this);
    }

    /**
     * @see MessageObserver#unregister()
     */
    @Override
    public void unregister() {
        TwitterQueues.unregisterObserver(this);
    }
}
