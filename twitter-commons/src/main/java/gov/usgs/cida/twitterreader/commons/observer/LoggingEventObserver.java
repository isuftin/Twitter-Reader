package gov.usgs.cida.twitterreader.commons.observer;

import ch.qos.logback.classic.Logger;
import com.twitter.hbc.core.event.Event;
import gov.usgs.cida.twitterreader.commons.queue.TwitterQueues;
import org.slf4j.LoggerFactory;

/**
 * Creates a message observer that receives and logs messages
 *
 * @author isuftin
 */
public class LoggingEventObserver extends EventObserver {

    private Logger logger;

    /**
     * Creates a LoggingEventObserver with a default logger
     */
    public LoggingEventObserver() {
       this((Logger) LoggerFactory.getLogger(LoggingEventObserver.class));
    }

    /**
     * Creates a LoggingEventObserver using a provided Logger
     *
     * @param logger
     */
    public LoggingEventObserver(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("Logger may not be null");
        }
        this.logger = logger;
    }

    @Override
    public void handleEvent(Event event) {
        logger.info(String.format("Incoming Twitter Event: %s",  event.getMessage()));
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
