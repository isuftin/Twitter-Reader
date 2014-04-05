package gov.usgs.cida.twitterreader.commons.queue;

import ch.qos.logback.classic.Logger;
import com.google.common.eventbus.EventBus;
import com.twitter.hbc.core.event.Event;
import gov.usgs.cida.twitterreader.commons.observer.interfaces.IClientObserver;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class TwitterQueues {

    private static Logger logger;
    private final static EventBus eventBus;
    private final static BlockingQueue<String> messageQueue;
    private final static BlockingQueue<Event> eventQueue;
    private final static ScheduledExecutorService eventSes;
    private final static ScheduledExecutorService messageSes;
    private static ScheduledFuture<TwitterEventRunner> eventFuture = null;
    private static ScheduledFuture<TwitterMessageRunner> messageFuture = null;
    private static QueueParams eventQueueParams;
    private static QueueParams messageQueueParams;

    static {
        messageQueue = new LinkedBlockingQueue<>(100000);
        eventQueue = new LinkedBlockingQueue<>(1000);
        eventSes = Executors.newScheduledThreadPool(1);
        messageSes = Executors.newScheduledThreadPool(1);
        eventBus = new EventBus("Twitter-Eventbus");
        eventQueueParams = new QueueParams(0l, 1l, TimeUnit.MINUTES);
        messageQueueParams = new QueueParams(0l, 1l, TimeUnit.MINUTES);
    }

    public TwitterQueues(Logger logger) {
        if (logger == null) {
            TwitterQueues.logger = (Logger) LoggerFactory.getLogger(TwitterQueues.class);
        } else {
            TwitterQueues.logger = logger;
        }
    }

    /**
     * Starts both event and message queueing
     */
    public void startQueueing() {
        startMessageQueueing(getMessageQueueParams());
        startEventQueueing(getEventQueueParams());
    }

    /**
     * Stops both message and event queueing
     */
    public void stopQueueing() {
        TwitterQueues.stopEventQueueing();
        TwitterQueues.stopMessageQueueing();
    }

    /**
     * Starts message queuing for the Twitter client using default start time
     * and run time
     */
    public void startMessageQueueing() {
        startMessageQueueing(getMessageQueueParams());
    }

    /**
     * Starts the event queuing for the Twitter client using default start time
     * and run time
     */
    public void startEventQueueing() {
        startEventQueueing(getEventQueueParams());
    }

    /**
     * Stops the message queuing for the Twitter client
     */
    public static void stopMessageQueueing() {
        if (null != messageFuture && !messageFuture.isDone() && !messageFuture.isCancelled()) {
            messageFuture.cancel(false);
            logger.info("Message queueing stopped");
        }
    }

    /**
     * Stops the event queuing for the Twitter client
     */
    public static void stopEventQueueing() {
        if (null != eventFuture && !eventFuture.isDone() && !eventFuture.isCancelled()) {
            eventFuture.cancel(false);
            logger.info("Event queueing stopped");
        }
    }

    /**
     * Starts message queuing for the Twitter client
     *
     * @param params
     */
    public void startMessageQueueing(QueueParams params) {
        Long initialDelay = params.getInitialDelay();
        Long period = params.getPeriod();
        TimeUnit timeUnit = params.getTimeUnit();
        if (null == TwitterQueues.messageFuture || TwitterQueues.messageFuture.isDone() || TwitterQueues.messageFuture.isCancelled()) {
            TwitterQueues.messageFuture = (ScheduledFuture<TwitterMessageRunner>) messageSes.scheduleAtFixedRate(new TwitterMessageRunner(logger), initialDelay, period, timeUnit);
            logger.info("Message queueing started");
        }
    }

    /**
     * Starts event queuing for the Twitter client
     *
     * @param params
     */
    public void startEventQueueing(QueueParams params) {
        Long initialDelay = params.getInitialDelay();
        Long period = params.getPeriod();
        TimeUnit timeUnit = params.getTimeUnit();
        if (null == TwitterQueues.eventFuture || TwitterQueues.eventFuture.isDone() || TwitterQueues.eventFuture.isCancelled()) {
            TwitterQueues.eventFuture = (ScheduledFuture<TwitterEventRunner>) eventSes.scheduleAtFixedRate(new TwitterEventRunner(logger), initialDelay, period, timeUnit);
            logger.info("Event queueing started");
        }
    }

    /**
     * Thread runnable object that checks the message queue and posts new
     * messages to observers
     */
    private static class TwitterMessageRunner implements Runnable {

        private static Logger RUNNER_LOGGER;

        public TwitterMessageRunner() {
            TwitterMessageRunner.RUNNER_LOGGER = (Logger) LoggerFactory.getLogger(TwitterMessageRunner.class);
        }

        public TwitterMessageRunner(Logger logger) {
            TwitterMessageRunner.RUNNER_LOGGER = logger;
        }

        @Override
        public void run() {
            RUNNER_LOGGER.debug("TwitterMessageRunner checking queue");
            while (!messageQueue.isEmpty()) {
                try {
                    String message = messageQueue.take();
                    eventBus.post(message);
                } catch (InterruptedException ex) {
                    RUNNER_LOGGER.warn("Message queue thread interrupted: " + ex);
                }
            }
            RUNNER_LOGGER.debug("TwitterMessageRunner done checking queue");
        }
    }

    /**
     * Thread runnable object that checks the event queue and posts new events
     * to observers
     */
    private static class TwitterEventRunner implements Runnable {

        private static Logger RUNNER_LOGGER;

        public TwitterEventRunner() {
            TwitterEventRunner.RUNNER_LOGGER = (Logger) LoggerFactory.getLogger(TwitterEventRunner.class);
        }

        public TwitterEventRunner(Logger logger) {
            TwitterEventRunner.RUNNER_LOGGER = logger;
        }

        @Override
        public void run() {
            RUNNER_LOGGER.debug("TwitterEventRunner checking queue");
            while (!TwitterQueues.eventQueue.isEmpty()) {
                try {
                    Event event = TwitterQueues.eventQueue.take();
                    eventBus.post(event);
                } catch (InterruptedException ex) {
                    RUNNER_LOGGER.warn("Event queue thread interrupted: " + ex);
                }
            }
            RUNNER_LOGGER.debug("TwitterEventRunner done checking queue");
        }
    }

    public static BlockingQueue<String> getMessageQueue() {
        return TwitterQueues.messageQueue;
    }

    public static BlockingQueue<Event> getEventQueue() {
        return TwitterQueues.eventQueue;
    }

    /**
     * @param params
     */
    public static void setEventQueueParams(QueueParams params) {
        TwitterQueues.eventQueueParams = params;
    }

    /**
     * @param params
     */
    public static void setMessageQueueParams(QueueParams params) {
        TwitterQueues.messageQueueParams = params;
    }

    public static void registerObserver(IClientObserver observer) {
        // Try to unregister observer. If not registered, no biggie
        unregisterObserver(observer);
        eventBus.register(observer);
        logger.info("Registered observer" + observer.toString());
    }

    public static void unregisterObserver(IClientObserver observer) {
        try {
            eventBus.unregister(observer);
        } catch (IllegalArgumentException iae) {
            logger.debug("Attempted to unregister observer that was not registered. This is usually not a problem.");
        }
    }

    /**
     * @return the eventQueueParams
     */
    public static QueueParams getEventQueueParams() {
        return TwitterQueues.eventQueueParams;
    }

    /**
     * @return the messageQueueParams
     */
    public static QueueParams getMessageQueueParams() {
        return TwitterQueues.messageQueueParams;
    }
}
