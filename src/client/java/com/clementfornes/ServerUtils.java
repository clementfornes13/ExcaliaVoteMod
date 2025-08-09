package com.clementfornes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;
import java.util.Set;

/**
 * Utilitaires liés au serveur courant (détection Excalia).
 */
public final class ServerUtils {
    private ServerUtils() {
    }

    // Domaines et IPs connus (ajoute ici si besoin)
    private static final Set<String> KNOWN_DOMAINS = Set.of("excalia.fr"); // match *.excalia.fr
    private static final Set<String> KNOWN_IPS = Set.of("213.182.214.23");

    /**
     * @return true si le client est connecté à un serveur Excalia (domain ou IP
     *         whitelisting),
     *         en ignorant le port.
     */
    public static boolean isExcaliaServer(MinecraftClient client) {
        if (client == null)
            return false;
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null || entry.address == null)
            return false;
        return isAddressExcalia(entry.address);
    }

    /**
     * Vérifie une adresse arbitraire "host[:port]" (IPv4/IPv6) contre la whitelist.
     */
    public static boolean isAddressExcalia(String address) {
        if (address == null || address.isBlank())
            return false;
        String host = extractHost(address).toLowerCase(Locale.ROOT);
        // IP exacte connue
        if (KNOWN_IPS.contains(host))
            return true;
        // Domain: match exact ou sous-domaine de KNOWN_DOMAINS
        for (String root : KNOWN_DOMAINS) {
            if (host.equals(root) || host.endsWith("." + root)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extrait l'hôte sans port d'une adresse type:
     * - "host:port"
     * - "[ipv6]:port"
     * - "host" ou "[ipv6]"
     */
    private static String extractHost(String address) {
        String s = address.trim();
        // IPv6 bracketed: [::1]:25565 -> ::1
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            if (end > 0) {
                return s.substring(1, end);
            }
            // bracket mal formé: fallback
            return s.replace("[", "").replace("]", "");
        }
        // IPv4/hostname: split on last ':' if present (to not break IPv6 non-bracketed,
        // rarely used by MC)
        int idx = s.lastIndexOf(':');
        if (idx > 0 && idx == s.indexOf(':')) {
            // une seule occurrence de ':' -> traité comme host:port
            return s.substring(0, idx);
        }
        // aucun port -> c’est déjà l’hôte
        return s;
    }
}