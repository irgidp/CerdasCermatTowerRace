package yt.corazonid.cerdasCermatTowerRace.listener;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {
    private final GameManager gm;

    public EntityDamageListener(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!gm.isGameActive()) return;

        // Blok PVP langsung antar player
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        // Blok damage ke villager bot dari sumber selain projectile
        // (projectile dihandle di SnowballListener & ArrowListener)
        if (event.getEntity() instanceof Villager) {
            // Izinkan damage dari projectile (arrow/snowball) — dihandle listener lain
            // Tapi batalkan damage langsung (tangan, splash potion, dll)
            if (!(event.getDamager() instanceof org.bukkit.entity.Projectile)) {
                event.setCancelled(true);
            }
        }
    }

    // Blok fall damage untuk player game (mereka di pijakan, harusnya tidak jatuh)
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!gm.isGameActive()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        GamePlayer gp = gm.getGamePlayerByPlayer(player);
        if (gp != null && gp.isAlive()) {
            event.setCancelled(true); // Tidak ada fall damage saat di game
        }
    }
}
