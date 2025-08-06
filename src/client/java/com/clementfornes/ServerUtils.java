package com.clementfornes;

import net.minecraft.client.MinecraftClient;

public class ServerUtils {
    public static boolean isExcaliaServer(MinecraftClient client) {
        if (client.getCurrentServerEntry() == null)
            return false;
        return client.getCurrentServerEntry().address.toLowerCase()
                .contains("excalia");
    }
}
