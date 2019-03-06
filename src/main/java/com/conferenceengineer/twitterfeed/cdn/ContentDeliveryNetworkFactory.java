package com.conferenceengineer.twitterfeed.cdn;

public final class ContentDeliveryNetworkFactory {

    private ContentDeliveryNetworkFactory() {
        super();
    }

    public static ContentDeliveryNetwork getInstance(final String containerName) {
        RackspaceCDN cdnConnection = new RackspaceCDN();
        cdnConnection.connect(containerName);
        return cdnConnection;
    }
}
