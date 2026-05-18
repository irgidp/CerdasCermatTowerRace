package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Handle snowball hit → kurangi pijakan musuh 1 block (hanya kena player/villager, BUKAN pijakan)
 */
public class SnowballListener implements Listener {
    private final GameManager gm;

    public SnowballListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!gm.isGameActive()) return;
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player shooter)) return;

        GamePlayer shooterGP = gm.getGamePlayerByPlayer(shooter);
        if (shooterGP == null) return;

        GamePlayer targetGP = null;

        // Kena player asli
        if (event.getHitEntity() instanceof Player hitPlayer) {
            targetGP = gm.getGamePlayerByPlayer(hitPlayer);
        }
        // Kena villager (bot)
        else if (event.getHitEntity() instanceof Villager hitVillager) {
            for (GamePlayer gp : gm.getRegisteredPlayers().values()) {
                if (gp.isBot() && gp.getVillager() != null
                        && gp.getVillager().getEntityId() == hitVillager.getEntityId()) {
                    targetGP = gp;
                    break;
                }
            }
        }

        if (targetGP == null || targetGP == shooterGP || !targetGP.isAlive()) return;

        // Kurangi 1 block
        gm.lowerPlatform(targetGP, 1);
        shooter.sendMessage("§b❄ Snowball mengenai §f" + targetGP.getPlayerName() + "§b! Pijakan turun 1!");
        if (!targetGP.isBot()) {
            Player tp = targetGP.getPlayer();
            if (tp != null) tp.sendMessage("§c❄ Kamu terkena snowball dari §f" + shooter.getName() + "§c! Pijakan turun 1!");
        }
        Bukkit.broadcastMessage("§b❄ " + shooter.getName() + " §fmengenai §b" + targetGP.getPlayerName() + " §fdengan snowball!");
    }
}
