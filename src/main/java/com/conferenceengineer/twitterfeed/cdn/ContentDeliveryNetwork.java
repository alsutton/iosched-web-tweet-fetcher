package com.conferenceengineer.twitterfeed.cdn;

import java.io.IOException;

/**
 * Interface representing a content delivery network
 */
public interface ContentDeliveryNetwork extends AutoCloseable {

    /**
     * Initialise a connection to a container on a CDN.
     */
    public void connect(final String container);

    /**
     * Disconnect from the CDN.
     */
    public void disconnect();

    /**
     * Close this connection.
     */
    public void close();

    /**
     * Request the CDN refreshes the copy of a particular data key held across its nodes.
     */

    public void requestRefresh(final String key);

    /**
     * Has any data been sent to the CDN.
     */

    public boolean hasDataBeenSent();

    /**
     * Put some data into a container at the CDN. This ensures the container
     * exists and populates it.
     *
     * @param key The key/filename under which the data will be stored in the container.
     * @param data The data to be stored.
     */

    public void putData(String key, byte[] data) throws IOException;

    /**
     * Put some data into a container at the CDN. This ensures the container
     * exists and populates it.
     *
     * @param key The key/filename under which the data will be stored in the container.
     * @param data The data to be stored.
     */

    public void putData(String key, String data) throws IOException;

}
