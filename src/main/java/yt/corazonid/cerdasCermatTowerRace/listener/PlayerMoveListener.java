package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * PlayerMoveListener - Cegah player bergerak saat game.
 *
 * PENJELASAN UNTUK PEMULA:
 * - PlayerMoveEvent dipanggil setiap saat player bergerak (bahkan melihat kanan/kiri!)
 * - Kita hanya blok perpindahan POSISI (X, Y, Z berubah)
 * - Tapi kita IZINKAN rotasi kepala (melihat kiri/kanan/atas/bawah)
 *
 * CARA CEK:
 * - event.getFrom() = posisi sebelum bergerak
 * - event.getTo()   = posisi setelah bergerak
 * - Jika X/Y/Z berubah = player pindah → kita cancel
 * - Jika hanya Yaw/Pitch berubah = player melihat → kita biarkan
 *
 * PENTING: EventPriority.HIGH agar kita proses sebelum plugin lain yang mungkin
 * mengizinkan gerakan.
 */
public class PlayerMoveListener implements Listener {
    private final GameManager gm;

    public PlayerMoveListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Hanya freeze saat game aktif
        if (!gm.isGameActive()) return;

        String playerName = event.getPlayer().getName();
        GamePlayer gp = gm.getGamePlayer(playerName);

        // Bukan player game atau sudah mati → bebas bergerak
        if (gp == null || !gp.isAlive()) return;
        // Player tidak di-freeze → bebas bergerak
        if (!gp.isFrozen()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Cek apakah ada perpindahan posisi (bukan hanya rotasi kepala)
        boolean positionChanged = (from.getX() != to.getX() ||
                from.getY() != to.getY() ||
                from.getZ() != to.getZ());

        if (positionChanged) {
            // Kembalikan player ke posisi lama, tapi pertahankan arah pandang (yaw/pitch)
            // Ini memastikan player bisa melihat kiri/kanan/atas/bawah tapi tidak bisa pindah
            Location stay = from.clone();
            stay.setYaw(to.getYaw());
            stay.setPitch(to.getPitch());
            event.setTo(stay);
        }
    }
}
