package yt.corazonid.cerdasCermatTowerRace;

import org.bukkit.plugin.java.JavaPlugin;
import yt.corazonid.cerdasCermatTowerRace.commands.*;
import yt.corazonid.cerdasCermatTowerRace.listener.*;
import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;

public final class CerdasCermatTowerRace extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);

        // Commands
        getCommand("regis").setExecutor(new RegisCommand(gameManager));
        getCommand("unregis").setExecutor(new UnregisCommand(gameManager));
        getCommand("addbot").setExecutor(new AddBotCommand(gameManager));
        getCommand("removebot").setExecutor(new RemoveBotCommand(gameManager));
        getCommand("listplayer").setExecutor(new ListPlayerCommand(gameManager));
        getCommand("startboard").setExecutor(new StartBoardCommand(gameManager));
        getCommand("startgame").setExecutor(new StartGameCommand(gameManager));
        getCommand("setsoal").setExecutor(new SetSoalCommand(gameManager));
        getCommand("settimer").setExecutor(new SetTimerCommand(gameManager));
        getCommand("resetgame").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
                sender.sendMessage("§cTidak punya izin!");
                return true;
            }
            gameManager.resetGame();
            sender.sendMessage("§a✓ Game direset!");
            return true;
        });

        // Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new SnowballListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new ArrowListener(gameManager, this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(gameManager), this);

        getLogger().info("╔══════════════════════════════════════╗");
        getLogger().info("║ Cerdas Cermat Tower Race ENABLED! 🏆 ║");
        getLogger().info("║  /addbot <nama> [easy|medium|hard]   ║");
        getLogger().info("╚══════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
