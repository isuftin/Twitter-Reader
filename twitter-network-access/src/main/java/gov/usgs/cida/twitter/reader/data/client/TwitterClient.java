package gov.usgs.cida.twitter.reader.data.client;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import com.twitter.hbc.core.Client;
import gov.usgs.cida.twitterreader.commons.observer.interfaces.IClientObserver;
import gov.usgs.cida.twitterreader.commons.queue.TwitterQueues;


/**
 * A client to asynchronously pull messages from CIDA's Twitter queue
 *
 * @author isuftin
 */
public class TwitterClient {

    private Logger logger;
    private final TwitterQueues queues;
    private static Client client = null;
    private static Boolean isConnected = false;
    private static Boolean isStopped = false;

    TwitterClient(Client client) {
        this(client, (Logger) LoggerFactory.getLogger(TwitterClient.class));
    }
    
    TwitterClient(Client client, Logger logger) {
        TwitterClient.client = client;
        this.logger = logger;
        queues =  new TwitterQueues(this.logger);
        logger.debug("New Twitter client created");
    }
    
    /**
     * Connects the Twitter client to Twitter
     */
    public synchronized void start() {
        this.start(Boolean.FALSE);
    }

    /**
     * Connects the Twitter client to Twitter with the option to automatically
     * start queueing
     *
     * @param autoStartQueue automatically begin queueing when connected
     */
    public synchronized void start(Boolean autoStartQueue) {
        if (!isConnected && !isStopped) {
            logger.info("Twitter client starting");
            client.connect();
            isConnected = true;

            if (autoStartQueue) {
                queues.startQueueing();
            }
            logger.info("Twitter client started");
        }
    }

    /**
     * Disconnect client and ready it for disposal. Also stops all queued jobs
     *
     * @param waitMillis milliseconds to wait for the client to stop
     */
    public synchronized void stop(Integer waitMillis) {
        logger.info("Twitter client stopping");
        queues.stopQueueing();
        if (client != null) {
            client.stop(waitMillis);
        }
        isConnected = false;
        isStopped = true;
        logger.info("Twitter client stopped");
    }

    /**
     * During finalize, shuts down the Twitter client
     *
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (null != client) {
                this.stop(10000);
            }
        } finally {
            super.finalize();
        }
    }
    
    public void addObserver(IClientObserver observer) {
        TwitterQueues.registerObserver(observer);
    }

    public static boolean isStopped() {
        return isStopped;
    }

    public static boolean isConnected() {
        return isConnected;
    }

}
