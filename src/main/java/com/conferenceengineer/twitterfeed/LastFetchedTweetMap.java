package com.conferenceengineer.twitterfeed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Map to store the hashtags and the most recent known tweet time.
 */
public class LastFetchedTweetMap extends HashMap<String, Long> {

    private String mStoreFilename;

    protected LastFetchedTweetMap(final String storeFilename) {
        mStoreFilename = storeFilename;
        loadMap();
    }

    private void loadMap() {
        try (LineNumberReader lnr = new LineNumberReader(new FileReader(mStoreFilename))) {
            String line;
            while((line = lnr.readLine()) != null) {
                storeLineInformation(line);
            }
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Problem reading tag map", e);
        }
    }

    private void storeLineInformation(final String line) {
        if(line.isEmpty()) {
            return;
        }

        int dotIdx = line.indexOf(':');
        if(dotIdx == -1) {
            return;
        }

        String lastSeenTweetTime = line.substring(0, dotIdx);
        String tag = line.substring(dotIdx+1).trim();

        put(tag, Long.parseLong(lastSeenTweetTime));
    }

    public void save()
        throws IOException {
        File newFile = new File(mStoreFilename+".tmp");

        try (PrintWriter pw = new PrintWriter(new FileWriter(newFile, false))) {
            for(Map.Entry<String, Long> entry : entrySet()) {
                pw.println(entry.getValue()+":"+entry.getKey());
            }
        }

        File originalFile = new File(mStoreFilename);
        if(originalFile.delete() == false) {
            throw new IOException("Unable to delete stored hashtag file");
        }

        newFile.renameTo(originalFile);
    }
}
