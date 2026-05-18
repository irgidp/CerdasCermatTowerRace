package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.CerdasCermatTowerRace;
import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArrowListener implements Listener {
    private final GameManager gm;
    private final CerdasCermatTowerRace plugin;

    // Menyimpan task shake yang sedang berjalan berdasarkan UUID player
    private final Map<UUID, BukkitTask> shakingTasks = new HashMap<>();

    public ArrowListener(GameManager gm, CerdasCermatTowerRace plugin) {
        this.gm     = gm;
        this.plugin  = plugin;
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        if (!gm.isGameActive()) return;
        Player player = event.getPlayer();
        GamePlayer gp = gm.getGamePlayerByPlayer(player);
        if (gp == null || !gp.isAlive()) return;

        var newItem = player.getInventory().getItem(event.getNewSlot());
        boolean holdingBow = newItem != null && newItem.getType() == org.bukkit.Material.BOW;

        if (holdingBow) {
            // Jalankan shake jika player baru mengganti ke item Bow dan belum ada task berjalan
            if (!shakingTasks.containsKey(player.getUniqueId())) {
                startCameraShake(player);
            }
        } else {
            // Langsung matikan efek shake detik itu juga saat ganti ke item lain
            stopCameraShake(player.getUniqueId());
        }
    }

    public void startCameraShake(Player player) {
        UUID uuid = player.getUniqueId();
        float[] yawOffsets = {0f, -10f, 0f, 10f};
        float[] pitchOffsets = {10f, 0f, -10f, 0f};
        int[] step = {0};

        // Buat loop task timer yang berjalan setiap 4 tick (0.2 detik)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Validasi: Jika player offline atau game selesai, matikan task
            if (!player.isOnline() || !gm.isGameActive()) {
                stopCameraShake(uuid);
                return;
            }

            // Validasi: Jika item di tangan utama bukan BOW lagi, matikan task
            var held = player.getInventory().getItemInMainHand();
            if (held.getType() != org.bukkit.Material.BOW) {
                stopCameraShake(uuid);
                return;
            }

            // Validasi: Jika data player hilang atau sudah mati (terkena lava), matikan task
            GamePlayer gp = gm.getGamePlayerByPlayer(player);
            if (gp == null || !gp.isAlive()) {
                stopCameraShake(uuid);
                return;
            }

            // Eksekusi efek shake kamera (merubah yaw pandangan)
            float offset = yawOffsets[step[0] % yawOffsets.length];
            float pitchOffset = pitchOffsets[step[0] % pitchOffsets.length];
            step[0]++;

            var loc = player.getLocation();
            loc.setYaw(loc.getYaw() + offset);
            loc.setPitch(loc.getPitch() + pitchOffset);
            player.teleport(loc);

        }, 0L, 4L);

        // Masukkan ke map agar bisa dipantau dan dihentikan sewaktu-waktu
        shakingTasks.put(uuid, task);

    }

    private void stopCameraShake(UUID uuid) {
        BukkitTask task = shakingTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel(); // Menghapus task dari memori server secara bersih
        }
    }

    // ─── Arrow kena target (Logika tetap sama) ──────────────────────────────────
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!gm.isGameActive()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        GamePlayer shooterGP = gm.getGamePlayerByPlayer(shooter);
        if (shooterGP == null) return;

        GamePlayer targetGP = null;

        if (event.getHitEntity() instanceof Player hitPlayer) {
            targetGP = gm.getGamePlayerByPlayer(hitPlayer);
        } else if (event.getHitEntity() instanceof Villager hitVillager) {
            for (GamePlayer gp : gm.getRegisteredPlayers().values()) {
                if (gp.isBot() && gp.getVillager() != null
                        && gp.getVillager().getEntityId() == hitVillager.getEntityId()) {
                    targetGP = gp;
                    break;
                }
            }
        }

        if (targetGP == null || targetGP == shooterGP || !targetGP.isAlive()) return;

        gm.lowerPlatform(targetGP, 3);

        shooter.sendMessage("§6🏹 Arrow mengenai §f" + targetGP.getPlayerName() + "§6! Pijakan turun 3!");
        if (!targetGP.isBot()) {
            Player tp = targetGP.getPlayer();
            if (tp != null) tp.sendMessage("§c🏹 Kamu terkena arrow dari §f" + shooter.getName() + "§c! Pijakan turun 3!");
        }
        Bukkit.broadcastMessage("§6🏹 " + shooter.getName() + " §fmengenai §6" + targetGP.getPlayerName() + " §fdengan arrow! §c(-3 block)");

        arrow.remove();
    }
}
