package gov.usgs.cida.twitter.reader.data.client;

import gov.usgs.cida.twitterreader.commons.queue.TwitterQueues;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;

/**
 * Creates a new Twitter Client.
 *
 * In order to properly create the client, a set of authentication parameters
 * must be passed in. This set should either be:
 *
 * OAuth: the combination of a consumer key, consumer secret, token and secret
 *
 * or
 *
 * Basic Auth: a username and password.
 *
 * If both are populated, OAuth is preferred.
 *
 * @author isuftin
 */
public class TwitterClientBuilder {

    private ClientContext context = null;

    public TwitterClient build() {
        TwitterClient client;
        Client coreClient;

        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("context cannot be null and has to be populated with at least a user id, a term or a location");
        }

        coreClient = createCoreClient();
        client = new TwitterClient(coreClient);

        return client;
    }

    /**
     * Builds a TwitterClient given a authentication method
     *
     * @return
     */
    private Client createCoreClient() {
        Client result;
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        endpoint.followings(context.getUserIds());
        endpoint.trackTerms(context.getTerms());
        endpoint.locations(context.getLocations());

        ClientBuilder cb = new ClientBuilder().
                name("Twitter-Client").
                hosts(Constants.SITESTREAM_HOST).
                authentication(context.getAuth()).
                endpoint(endpoint).
                processor(new StringDelimitedProcessor(TwitterQueues.getMessageQueue())).
                eventMessageQueue(TwitterQueues.getEventQueue());

        result = cb.build();
        return result;
    }

    /**
     * @param context the context to set
     * @return
     */
    public TwitterClientBuilder setContext(ClientContext context) {
        this.context = context;
        return this;
    }
}