package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command: /startgame
 * Aktifkan game session → set gamemode survival, freeze player.
 */
public class StartGameCommand implements CommandExecutor {
    private final GameManager gm;

    public StartGameCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        String error = gm.startGame();
        if (error != null) {
            sender.sendMessage(error);
        } else {
            sender.sendMessage("§a✓ Game dimulai! Sekarang gunakan /setsoal <id> untuk mulai soal.");
        }

        return true;
    }
}
