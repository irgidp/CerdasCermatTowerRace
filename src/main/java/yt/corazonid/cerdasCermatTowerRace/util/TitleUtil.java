package yt.corazonid.cerdasCermatTowerRace.util;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class TitleUtil {

    private TitleUtil() {
        // Utility class
    }

    public static void broadcastTitle(Server server, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        for (Player player : server.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
        }
    }
}

