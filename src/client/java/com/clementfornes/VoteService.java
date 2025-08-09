package com.clementfornes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service asynchrone pour récupérer les votes d'un utilisateur.
 * - Timeout réseau configuré
 * - Parsing JSON robuste
 * - Exécuteur dédié (daemon) nommé
 */
public class VoteService {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private static final String JSON_VOTES_KEY = "votes";
    private static final String JSON_SITES_KEY = "sites";

    private final String endpoint; // se termine par '/'
    private final Logger logger;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ExcaliaVoteMod-VoteFetcher");
        t.setDaemon(true);
        return t;
    });

    /** Dernier total de votes (-1 si non initialisé / erreur). */
    public volatile int totalVotes = -1;

    /** Map des timestamps de reset par site (clé=id, valeur=epoch ms). */
    public volatile Map<String, Long> sites = null;

    public VoteService(String endpointBase, Logger logger) {
        this.endpoint = endpointBase.endsWith("/") ? endpointBase : endpointBase + "/";
        this.logger = logger;
    }

    /**
     * Récupère les données de vote pour l'utilisateur (appel non bloquant).
     * Met à jour {@link #totalVotes} et {@link #sites}.
     */
    public CompletableFuture<Void> fetch(String username) {
        final String url = endpoint + username;
        return CompletableFuture.runAsync(() -> fetchBlocking(url, username), executor);
    }

    private void fetchBlocking(String url, String username) {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(url);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP " + status);
            }

            try (InputStream in = conn.getInputStream();
                    Reader reader = new InputStreamReader(in)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                // votes
                totalVotes = json.has(JSON_VOTES_KEY) ? json.get(JSON_VOTES_KEY).getAsInt() : -1;

                // sites
                if (json.has(JSON_SITES_KEY) && json.get(JSON_SITES_KEY).isJsonObject()) {
                    JsonObject sitesJson = json.getAsJsonObject(JSON_SITES_KEY);
                    sites = sitesJson.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAsLong()));
                } else {
                    sites = null;
                }
            }

            logger.info("[ExcaliaVoteMod] Vote data fetched for {}", username);
        } catch (Exception e) {
            totalVotes = -1;
            sites = null;
            logger.error("[ExcaliaVoteMod] Failed to fetch vote data for {}: {}", username, e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    /** Arrête immédiatement l'exécuteur (à appeler si besoin lors du shutdown). */
    public void shutdown() {
        executor.shutdownNow();
    }
}