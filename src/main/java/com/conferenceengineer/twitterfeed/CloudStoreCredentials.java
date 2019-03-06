package com.conferenceengineer.twitterfeed;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to determine credentials for the cloud storage system.
 */
public class CloudStoreCredentials {

    private static final String PROVIDER ="rackspace-cloudfiles-uk";
    private static final String REGION = "LON";

    private String mUsername;
    private String mAPIKey;
    private String mContainerPrefix;

    private CloudStoreCredentials(final String username, final String apiKey, final String containerPrefix) {
        mUsername = username;
        mAPIKey = apiKey;
        mContainerPrefix = containerPrefix;
    }

    public String getProvider() {
        return PROVIDER;
    }

    public String getRegion() {
        return REGION;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getAPIKey() {
        return mAPIKey;
    }

    public String getContainerPrefix() { return mContainerPrefix; }

    //--------- Singleton implementation

    // TODO insert live API keys

    private static final CloudStoreCredentials DEVELOPMENT_CREDENTIALS
            = new CloudStoreCredentials("confeng", "[INSERT-API-KEY]", "dev-");

    private static final CloudStoreCredentials PRODUCTION_CREDENTIALS
            = new CloudStoreCredentials("confeng", "[INSERT-API-KEY]", "");

    private static CloudStoreCredentials sCloudstoreCredentials = null;

    public static CloudStoreCredentials getInstance() {
        if(sCloudstoreCredentials == null) {
            sCloudstoreCredentials = isServerOnlyOnInternalNetworks() ? DEVELOPMENT_CREDENTIALS : PRODUCTION_CREDENTIALS;
        }

        return sCloudstoreCredentials;
    }

    private static boolean isServerOnlyOnInternalNetworks() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface thisInterface = interfaces.nextElement();
                if(thisInterface.isLoopback()) {
                    continue;
                }

                for(InterfaceAddress thisAddress : thisInterface.getInterfaceAddresses()) {
                    if(!thisAddress.getAddress().isSiteLocalAddress()) {
                        return false;
                    }
                }
            }
        } catch(SocketException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Problem getting network interfaces");
        }
        return true;
    }
}
