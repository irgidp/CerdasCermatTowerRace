package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.BotManager;
import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /addbot <nama> [easy|medium|hard|random]
 *
 * Contoh:
 *   /addbot BotAlpha          → bot dengan level MEDIUM (default)
 *   /addbot BotAlpha hard     → bot yang cepat jawab
 *   /addbot BotBeta easy      → bot yang lambat jawab
 *   /addbot BotGamma random   → bot yang level-nya random setiap soal
 *
 * KENAPA TIDAK PAKAI /regis UNTUK BOT?
 * /regis pakai Bukkit.getPlayer(nama) yang butuh player beneran online.
 * /addbot langsung buat GamePlayer(nama, slot, world) tanpa butuh Player online.
 */
public class AddBotCommand implements CommandExecutor {

    private final GameManager gm;

    public AddBotCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        // Butuh minimal 1 argumen (nama bot)
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /addbot <nama> [easy|medium|hard|random]");
            sender.sendMessage("§7Level default: §fmedium");
            return true;
        }

        // Harus dijalankan oleh player (bukan console) karena butuh World
        // Kalau dari console, kita ambil world default
        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            // Console → pakai world pertama yang ada
            world = org.bukkit.Bukkit.getWorlds().get(0);
        }

        String botName = args[0];

        // Parse level (argumen ke-2, opsional)
        BotManager.BotLevel level = BotManager.BotLevel.MEDIUM; // default
        if (args.length >= 2) {
            try {
                level = BotManager.BotLevel.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cLevel tidak valid! Pilih: easy, medium, hard, random");
                return true;
            }
        }

        // Tambahkan bot lewat BotManager
        BotManager botManager = gm.getBotManager();
        String error = botManager.addBot(botName, level, world);

        if (error != null) {
            sender.sendMessage(error);
        } else {
            int slot = gm.getGamePlayer(botName).getSlot();
            sender.sendMessage("§a✓ Bot §f" + botName
                    + " §a[" + level.name() + "] ditambahkan ke §6Slot " + slot + "§a!");
            sender.sendMessage("§7Bot akan menjawab soal otomatis saat /setsoal dijalankan.");
        }

        return true;
    }
}
