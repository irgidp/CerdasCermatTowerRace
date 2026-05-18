package yt.corazonid.cerdasCermatTowerRace.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;

public class RegisCommand implements CommandExecutor {
    private final GameManager gm;

    public RegisCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Cek permission
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin untuk command ini!");
            return true;
        }

        // Cek apakah ada argumen (nama player)
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /regis <playername>");
            return true;
        }

        String playerName = args[0];

        // Panggil GameManager untuk register
        String error = gm.registerPlayer(playerName);
        if (error != null) {
            sender.sendMessage(error); // Ada error
        } else {
            int slot = gm.getGamePlayer(playerName).getSlot();
            sender.sendMessage("§a✓ " + playerName + " berhasil didaftarkan ke §6Slot " + slot + "§a!");
            // Broadcast ke semua player
            org.bukkit.Bukkit.broadcastMessage("§a[Cerdas Cermat Tower Race] §f" + playerName + " telah bergabung! §7(Slot " + slot + ")");
        }

        return true;
    }
}
