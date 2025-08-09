package com.clementfornes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExcaliaVoteModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("excaliavotemod-client");

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("excaliavotemod.json");

    // Intervalles
    private static final long FETCH_INTERVAL_MINUTES = 5L;

    // Scheduler (daemon) pour tâches périodiques
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ExcaliaVoteMod-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private final ConfigManager config = new ConfigManager(CONFIG_PATH);
    private final VoteService voteService = new VoteService("https://www.excalia.fr/vote/user/", LOGGER);
    private final HudOverlay hudOverlay = new HudOverlay(config);

    // Handle pour annuler le fetch périodique
    private ScheduledFuture<?> voteFetchTask;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ExcaliaVoteMod] Client initialization started");
        config.load();

        KeyBindings.register();

        // Sauvegarde périodique de la config (fallback au cas où)
        SCHEDULER.scheduleAtFixedRate(config::save, FETCH_INTERVAL_MINUTES, FETCH_INTERVAL_MINUTES, TimeUnit.MINUTES);

        // HUD render
        HudRenderCallback.EVENT.register((ctx, tick) -> hudOverlay.onHudRender(ctx, tick, voteService));

        // Démarre le fetch quand on rejoint un serveur Excalia
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ServerUtils.isExcaliaServer(client))
                return;
            String playerName = client.getSession().getUsername();
            LOGGER.info("[ExcaliaVoteMod] {} joined Excalia server, starting vote fetch", playerName);
            startVoteFetch(playerName);
        });

        // Stoppe le fetch à la déconnexion
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stopVoteFetch());

        LOGGER.info("[ExcaliaVoteMod] Client initialization complete");
    }

    private synchronized void startVoteFetch(String username) {
        // Annule une tâche existante si besoin
        if (voteFetchTask != null && !voteFetchTask.isCancelled()) {
            voteFetchTask.cancel(false);
        }

        // Fetch immédiat
        voteService.fetch(username)
                .thenAccept(v -> LOGGER.info("[ExcaliaVoteMod] Vote data fetched for {}", username));

        // Fetch périodique
        voteFetchTask = SCHEDULER.scheduleAtFixedRate(
                () -> voteService.fetch(username),
                0L,
                FETCH_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
    }

    private synchronized void stopVoteFetch() {
        if (voteFetchTask != null) {
            voteFetchTask.cancel(false);
            voteFetchTask = null;
            LOGGER.info("[ExcaliaVoteMod] Vote fetch task cancelled");
        }
    }
}