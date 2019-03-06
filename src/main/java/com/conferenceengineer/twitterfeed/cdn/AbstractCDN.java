package com.conferenceengineer.twitterfeed.cdn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class AbstractCDN implements ContentDeliveryNetwork, AutoCloseable {

    private boolean mHasDataBeenSent = false;

    @Override
    public void close() {
        disconnect();
    }

    public boolean hasDataBeenSent() {
        return mHasDataBeenSent;
    }

    protected void hasDataBeenSent(boolean hasDataBeenSent) {
        mHasDataBeenSent = hasDataBeenSent;
    }

    @Override
    public void putData(final String key, final String data)
        throws IOException {
        try {
            byte[] dataBytes = data.getBytes("UTF-8");
            putData(key, dataBytes);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }
}
