package yt.corazonid.cerdasCermatTowerRace.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;

public class UnregisCommand implements CommandExecutor {
    private final GameManager gameManager;

    public UnregisCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cAnda tidak memiliki izin untuk menggunakan command ini!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /unregis <playername>");
            return true;
        }

        String playerName = args[0];

        if (gameManager.unregisterPlayer(playerName)) {
            sender.sendMessage("§a✓ " + playerName + " berhasil dihapus");
        } else {
            sender.sendMessage("§c✗ Player " + playerName + " tidak terdaftar");
        }

        return true;
    }
}

