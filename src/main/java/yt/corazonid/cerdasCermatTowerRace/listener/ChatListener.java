package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * ChatListener - Dengarkan semua pesan chat player.
 *
 * PENJELASAN UNTUK PEMULA:
 * - Listener adalah class yang "mendengarkan" event dari Bukkit
 * - @EventHandler = anotasi yang menandai method ini sebagai event handler
 * - AsyncPlayerChatEvent = event saat player mengirim pesan chat
 * - event.setCancelled(true) = sembunyikan pesan dari chat publik
 *
 * KENAPA AsyncPlayerChatEvent?
 * - "Async" artinya event ini berjalan di thread yang berbeda dari main thread
 * - Kita harus hati-hati: modifikasi world TIDAK boleh dilakukan di async thread
 * - Makanya kita pakai Bukkit.getScheduler().runTask() untuk pindah ke main thread
 *   sebelum melakukan perubahan dunia (spawn block, teleport, dll)
 *
 * EventPriority.LOWEST = kita proses dulu sebelum plugin lain
 */
public class ChatListener implements Listener {
    private final GameManager gm;

    public ChatListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = event.getMessage().trim();

        // Minta GameManager untuk proses chat ini
        // Jika return true → sembunyikan dari chat publik (jawaban benar)
        boolean shouldCancel = gm.processChat(playerName, message);

        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
}
