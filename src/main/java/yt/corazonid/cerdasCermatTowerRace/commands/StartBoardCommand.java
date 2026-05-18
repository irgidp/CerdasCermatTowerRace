package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /startboard
 *
 * Spawn pijakan di sekitar posisi admin berdiri.
 * Admin harus berdiri di tengah-tengah area yang mau dijadikan arena.
 *
 * PENJELASAN UNTUK PEMULA:
 * - Kita perlu tahu admin berdiri di mana, jadi sender harus Player (bukan console)
 * - Kita cast sender → Player dengan pengecekan instanceof dulu
 * - Location = posisi di dunia Minecraft (x, y, z, world)
 */
public class StartBoardCommand implements CommandExecutor {
    private final GameManager gm;

    public StartBoardCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        // Command ini HARUS dijalankan oleh player (bukan console)
        // karena kita butuh posisi admin sebagai titik spawn board
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommand ini hanya bisa dijalankan oleh player (bukan console)!");
            return true;
        }

        sender.sendMessage("§e⏳ Membangun arena...");

        // Panggil GameManager untuk build board di posisi player saat ini
        String error = gm.buildBoard(player.getLocation());
        if (error != null) {
            sender.sendMessage(error);
        } else {
            sender.sendMessage("§a✓ Arena berhasil dibangun!");
            sender.sendMessage("§7  • Pijakan telah di-spawn");
            sender.sendMessage("§7  • Lava telah dipasang di bawah");
            sender.sendMessage("§7  • Semua player sudah di-teleport");
            sender.sendMessage("§eSekarang jalankan /startgame untuk mulai!");
        }

        return true;
    }
}
