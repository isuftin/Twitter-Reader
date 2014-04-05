package gov.usgs.cida.twitterreader.commons.observer;

import com.twitter.hbc.core.event.Event;

/**
 * Sets an interface for handling Events
 *
 * @author isuftin
 */
public abstract class EventObserver extends ClientObserver {

    /**
     * Handles a new incoming event
     *
     * @param event
     */
    @Override
    public abstract void handleEvent(Event event);

    @Override
    public void handleEvent(String messageObject) {
        throw new UnsupportedOperationException("Event handler does not handle String.class");
    }

}
