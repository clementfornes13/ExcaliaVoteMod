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
import java.util.concurrent.TimeUnit;

public class ExcaliaVoteModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("excaliavotemod-client");
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("excaliavotemod.json");
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final String VOTE_ENDPOINT = "https://www.excalia.fr/vote/user/";

    private final ConfigManager config = new ConfigManager(CONFIG_PATH);
    private final VoteService voteService = new VoteService(VOTE_ENDPOINT, LOGGER);
    private final HudOverlay hudOverlay = new HudOverlay(config);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ExcaliaVoteMod] Excalia Vote Mod Client Initializing");
        config.load();

        KeyBindings.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ServerUtils.isExcaliaServer(client))
                return;
            String playerName = client.getSession().getUsername();
            LOGGER.info("[ExcaliaVoteMod] Player {} joined Excalia server, scheduling vote fetch", playerName);
            scheduleVoteFetch(playerName);
        });
        HudRenderCallback.EVENT
                .register((drawContext, tickCounter) -> hudOverlay.onHudRender(drawContext, tickCounter, voteService));

        SCHEDULER.scheduleAtFixedRate(config::save, 5, 5, TimeUnit.MINUTES);
    }

    private void scheduleVoteFetch(String username) {
        voteService.fetch(username)
                .thenAccept(v -> LOGGER.info("[ExcaliaVoteMod] Vote data fetched for {}", username));
        SCHEDULER.scheduleAtFixedRate(
                () -> voteService.fetch(username),
                5, 5, TimeUnit.MINUTES);
    }
}