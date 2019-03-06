package com.conferenceengineer.twitterfeed;

import com.conferenceengineer.twitterfeed.cdn.ContentDeliveryNetwork;
import com.conferenceengineer.twitterfeed.cdn.ContentDeliveryNetworkFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * Fetch the tweets for a certain query and store them.
 */
public class TweetFetcher {
    private static final String UPDATES_CONTAINER = "updates.conferenceengineer.com";

    // TODO insert live API keys
    private static final String TWITTER_CONSUMER_KEY = "[INSERT-API-KEY]";     // Conference Engineer Key
    private static final String TWITTER_CONSUMER_SECRET = "[INSERT-API-KEY]"; // Conference Engineer Key

    // Database access credentials
    private static final String JDBC_DRIVER_CLASS = "org.postgresql.Driver";
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/conferenceengineer";

    // TODO insert credentials
    private static final String JDBC_USERNAME = "";
    private static final String JDBC_PASSWORD = "";

    /**
     * The query used to find conferences with a hashtag.
     */

    private static final String CONFERENCE_SELECT_QUERY = "select id, hashtag from conference where hashtag is not null";


    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("You must specify the path for the tag store file");
            System.exit(-1);
        }

        LastFetchedTweetMap tweetMap = new LastFetchedTweetMap(args[0]);
        try {
            Twitter twitter = new TwitterFactory(createConfigurationBuilder().build()).getInstance();
            twitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);
            twitter.setOAuth2Token(twitter.getOAuth2Token());

            // Tickle the driver
            Class.forName(JDBC_DRIVER_CLASS);

            try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD)) {
                try (Statement stmt = connection.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery(CONFERENCE_SELECT_QUERY)) {
                        while (rs.next()) {
                            String id = rs.getString(1);
                            String hashtag = rs.getString(2);
                            if (hashtag != null) {
                                export(twitter, tweetMap, id, hashtag);
                            }
                        }
                    }
                }
            }

            tweetMap.save();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private static ConfigurationBuilder createConfigurationBuilder() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setApplicationOnlyAuthEnabled(true);
        return cb;
    }

    private static String getContainerNameForConference(String conferenceId) {
        CloudStoreCredentials credentials = CloudStoreCredentials.getInstance();
        return credentials.getContainerPrefix()+UPDATES_CONTAINER;
    }

    /**
     * Export the search results for a conference to a file
     */

    private static void export(Twitter twitter, final Map<String,Long> mostRecentKnownTweets, final
                String conferenceId, final String hashTag)
            throws TwitterException, JSONException, FileNotFoundException {
        long mostRecentTweetTime = Long.MIN_VALUE;

        Query query = new Query(hashTag);
        query.setCount(100);
        QueryResult results = twitter.search(query);

        JSONArray jsonArray = new JSONArray();
        for(Status result: results.getTweets()) {
            JSONObject object = new JSONObject();
            // Skip unattributed tweets.
            if(result.getUser() == null) {
                continue;
            }
            // Skip retweets.
            if(result.isRetweet()) {
                continue;
            }

            long creationTime = result.getCreatedAt().getTime();

            object.put("name", result.getUser().getName());
            object.put("text", result.getText());
            object.put("id", result.getId());
            object.put("retweetCount", result.getRetweetCount());
            object.put("createdAt", creationTime);
            object.put("screenName", result.getUser().getScreenName());
            object.put("userId", result.getUser().getId());
            object.put("verified", result.getUser().isVerified());
            object.put("profileImageURL", result.getUser().getOriginalProfileImageURL());

            if(creationTime > mostRecentTweetTime) {
                mostRecentTweetTime = creationTime;
            }

            jsonArray.put(object);
        }

        JSONObject root = new JSONObject();
        root.put("tweets", jsonArray);

        Long mostRecentKnownTweetTime = mostRecentKnownTweets.get(hashTag);
        if(mostRecentKnownTweetTime == null || mostRecentTweetTime > mostRecentKnownTweetTime ) {
            sendTweetsToCDN(conferenceId, root);
            mostRecentKnownTweets.put(hashTag, mostRecentTweetTime);
        }

    }

    private static void sendTweetsToCDN(final String conferenceId, final JSONObject tweets) {
        try(ContentDeliveryNetwork cdn =
                    ContentDeliveryNetworkFactory.getInstance(getContainerNameForConference(conferenceId))) {
            cdn.putData("events/"+conferenceId+"/iosched14/tweets.json", tweets.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

