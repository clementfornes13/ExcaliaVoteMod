package com.clementfornes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class VoteService {
    private final String endpoint;
    private final Logger logger;

    public int totalVotes = -1;
    public Map<String, Long> sites;

    public VoteService(String endpointBase, Logger logger) {
        this.endpoint = endpointBase;
        this.logger = logger;
    }

    public CompletableFuture<Void> fetch(String username) {
        return CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create(endpoint + username);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(conn.getInputStream())).getAsJsonObject();

                totalVotes = json.get("votes").getAsInt();
                sites = json.getAsJsonObject("sites").entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getAsLong()));
                logger.info("[ExcaliaVoteMod] Successfully fetched vote data for {}", username);
            } catch (Exception e) {
                totalVotes = -1;
                sites = null;
                logger.error("[ExcaliaVoteMod] Failed to fetch vote data for {}: {}",
                        username, e.getMessage());
            }
        });
    }
}
