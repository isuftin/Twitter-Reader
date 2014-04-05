package gov.usgs.cida.twitterreader.commons.observer.interfaces;

import com.twitter.hbc.core.event.Event;

/**
 *
 * @author isuftin
 */
public interface IClientObserver {

    /**
     * Handles a new incoming event
     *
     * @param eventObject
     */
    public void handleEvent(Event eventObject);

    /**
     * Handles a new incoming event
     *
     * @param messageObject
     */
    public void handleEvent(String messageObject);

    /**
     * Register this object to the event bus
     */
    public void register();

    /**
     * Unregister this object from the event bus
     */
    public void unregister();
}
