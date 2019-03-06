package com.conferenceengineer.twitterfeed.cdn;

import com.conferenceengineer.twitterfeed.CloudStoreCredentials;
import com.google.common.io.ByteSource;
import org.jclouds.ContextBuilder;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudfiles.v1.features.CDNApi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the CDN to connect to RackSpace
 */
public class RackspaceCDN extends AbstractCDN {

    private static final int DEFAULT_TTL = 60; // 1 minute.

    private static final List<String> EMPTY_EMAIL_ADDRESS_LIST = new ArrayList<String>();

    private static final CloudStoreCredentials CLOUD_STORE_CREDENTIALS = CloudStoreCredentials.getInstance();

    private CloudFilesApi mCloudFilesApi = null;
    private ObjectApi mObjectApi;
    private String mContainerName;

    private Set<String> mKnownContainers = new HashSet<String>();

    @Override
    public void connect(final String container) {
        mCloudFilesApi = ContextBuilder.newBuilder(CLOUD_STORE_CREDENTIALS.getProvider())
                                .credentials(
                                        CLOUD_STORE_CREDENTIALS.getUsername(),
                                        CLOUD_STORE_CREDENTIALS.getAPIKey()
                                )
                                .buildApi(CloudFilesApi.class);

        mObjectApi =
                mCloudFilesApi.getObjectApiForRegionAndContainer(CLOUD_STORE_CREDENTIALS.getRegion(), container);

        ensureContainerExists(container);
        mContainerName = container;
    }

    @Override
    public void disconnect() {
        if(mCloudFilesApi != null) {
            try {
                mCloudFilesApi.close();
            } catch (IOException e) {
                // Ignore IOException, problems during closing aren't relevant
                // to the data being pushed.
            }
            mCloudFilesApi = null;
        }
    }

    @Override
    public void requestRefresh(String key) {
        CDNApi cdnAPI = mCloudFilesApi. getCDNApiForRegion(CLOUD_STORE_CREDENTIALS.getRegion());
        cdnAPI.purgeObject(mContainerName, key, EMPTY_EMAIL_ADDRESS_LIST);
    }

    @Override
    public void putData(String key, byte[] data) {
        SimpleByteSource byteSource = new SimpleByteSource(data);
        Payload payload = Payloads.newByteSourcePayload(byteSource);
        payload.getContentMetadata().setContentType("application/json");
        mObjectApi.put(key, payload);
        hasDataBeenSent(true);
    }

    private void ensureContainerExists(final String container) {
        if(mKnownContainers.contains(container)) {
            return;
        }

        ContainerApi containerApi = mCloudFilesApi.getContainerApiForRegion(CLOUD_STORE_CREDENTIALS.getRegion());
        if(containerApi.get(container) == null) {
            containerApi.create(container);
            CDNApi cdnAPI = mCloudFilesApi. getCDNApiForRegion(CLOUD_STORE_CREDENTIALS.getRegion());
            cdnAPI.enable(container, DEFAULT_TTL);
        }

        mKnownContainers.add(container);
    }


    private static final class SimpleByteSource extends ByteSource {

        private final byte[] mData;

        private SimpleByteSource(byte[] data) {
            mData = data;
        }

        @Override
        public ByteArrayInputStream openStream() throws IOException {
            return new ByteArrayInputStream(mData);
        }

        @Override
        public long size() throws IOException {
            return mData.length;
        }

        @Override
        public byte[] read() throws IOException {
            return mData;
        }
    }
}
