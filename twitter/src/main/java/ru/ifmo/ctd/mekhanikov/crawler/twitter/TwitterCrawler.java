package ru.ifmo.ctd.mekhanikov.crawler.twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctd.mekhanikov.crawler.Config;
import twitter4j.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TwitterCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterCrawler.class);
    private static final String FRIENDS_ENDPOINT = "/friends/ids";
    private static final int MIN_SLEEP_TIME = 5;

    private final Twitter twitter;

    private TwitterCrawler() throws IOException {
        Config.load();
        this.twitter = TwitterFactory.getSingleton();
        LOG.info("Twitter service initialized");
    }

    private void checkRate() throws TwitterException {
        RateLimitStatus rateLimitStatus = twitter.getRateLimitStatus().get(FRIENDS_ENDPOINT);
        while (rateLimitStatus.getRemaining() == 0) {
            LOG.info("No requests left. Sleep " + rateLimitStatus.getSecondsUntilReset() + " seconds");
            try {
                Thread.sleep(Math.max(rateLimitStatus.getSecondsUntilReset() + 1, MIN_SLEEP_TIME) * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rateLimitStatus = twitter.getRateLimitStatus().get(FRIENDS_ENDPOINT);
        }
        LOG.info("Requests left: " + rateLimitStatus.getRemaining());
    }

    private List<Long> getFriends(long userId) {
        LOG.info("Getting friends list of user " + userId);
        List<Long> friends = new ArrayList<>();
        long cursor = -1;
        while (cursor != 0) {
            try {
                checkRate();
                LOG.info(String.format("Sending request. userId=%d, cursor=%d", userId, cursor));
                IDs result = twitter.getFriendsIDs(userId, cursor);
                LOG.info("Response received");
                for (long id : result.getIDs()) {
                    friends.add(id);
                }
                cursor = result.getNextCursor();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }
        return friends;
    }

    private void doMain() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(Config.getInputFile()))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                long userId = Long.parseLong(line);
                System.out.println(getFriends(userId));
            }
        }
    }

    public static void main(String ... args) throws IOException {
        new TwitterCrawler().doMain();
    }
}