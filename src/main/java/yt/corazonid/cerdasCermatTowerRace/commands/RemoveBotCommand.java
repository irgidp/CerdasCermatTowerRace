package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command: /removebot <nama>
 * Hapus bot dari game.
 */
public class RemoveBotCommand implements CommandExecutor {

    private final GameManager gm;

    public RemoveBotCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /removebot <nama>");
            return true;
        }

        String botName = args[0];

        if (gm.getBotManager().removeBot(botName)) {
            sender.sendMessage("§a✓ Bot §f" + botName + " §aberhasil dihapus.");
        } else {
            sender.sendMessage("§cBot §f" + botName + " §ctidak ditemukan.");
            // Coba hapus sebagai player biasa kalau ternyata salah kategori
            if (gm.isRegistered(botName)) {
                sender.sendMessage("§7Hint: " + botName + " terdaftar sebagai player asli. Pakai /unregis.");
            }
        }

        return true;
    }
}
