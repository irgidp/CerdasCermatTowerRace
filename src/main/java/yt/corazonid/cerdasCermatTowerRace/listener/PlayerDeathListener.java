package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * PlayerDeathListener - Tangkap event saat player mati.
 *
 * PENJELASAN UNTUK PEMULA:
 * - PlayerDeathEvent dipanggil Bukkit setiap kali player mati
 * - Kita cek apakah player mati karena lava (getLastDamageCause)
 * - Kalau ya → panggil GameManager.handlePlayerDeath()
 *
 * CARA CEK PENYEBAB KEMATIAN:
 * event.getEntity().getLastDamageCause().getCause()
 * → DamageCause.LAVA = mati karena lava
 * → DamageCause.FIRE = mati karena api (juga terkait lava)
 * → DamageCause.FIRE_TICK = mati karena terbakar
 */
public class PlayerDeathListener implements Listener {
    private final GameManager gm;

    public PlayerDeathListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Cek game aktif
        if (!gm.isGameActive()) return;

        String playerName = event.getEntity().getName();

        // Cek apakah player ini terdaftar di game
        if (!gm.isRegistered(playerName)) return;

        // Proses kematian (kurangi point, set spectator, dll)
        gm.handlePlayerDeath(playerName);

        // Hapus death message supaya tidak clutter
        event.setDeathMessage(null);
    }

    // Helper method untuk cek apakah bukan player game yang mati
    private boolean isRegistered(String name) {
        return gm.isRegistered(name);
    }
}
