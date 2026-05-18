package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public class ListPlayerCommand implements CommandExecutor {
    private final GameManager gm;

    public ListPlayerCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        Collection<GamePlayer> players = gm.getRegisteredPlayers().values();

        if (players.isEmpty()) {
            sender.sendMessage("§7Belum ada player / bot yang terdaftar.");
            sender.sendMessage("§7Gunakan §f/regis <nama>§7 untuk player asli.");
            sender.sendMessage("§7Gunakan §f/addbot <nama> [easy|medium|hard|random]§7 untuk bot.");
            return true;
        }

        long botCount    = players.stream().filter(GamePlayer::isBot).count();
        long playerCount = players.size() - botCount;

        sender.sendMessage("§6§l══════════ DAFTAR PLAYER ══════════");
        sender.sendMessage("§f" + playerCount + " player asli  §7|  §e" + botCount + " bot simulasi");
        sender.sendMessage("");

        for (GamePlayer gp : players) {
            String status     = gp.isAlive() ? "§aHidup" : "§cMati";
            String typeTag    = gp.isBot() ? "§e[BOT] " : "§f[REAL]§f ";
            String levelTag   = "";
            if (gp.isBot()) {
                // Tampilkan level bot dari BotManager
                var level = gm.getBotManager().getBotLevel(gp.getPlayerName());
                levelTag = level != null ? " §7(" + level.name() + ")" : "";
            }
            sender.sendMessage(String.format("§6[Slot %d] %s§f%-14s §7Y:§e%-4d §7Poin:§b%-4d %s%s",
                    gp.getSlot(),
                    typeTag,
                    gp.getPlayerName(),
                    gp.getPlatformY(),
                    gp.getPoint(),
                    status,
                    levelTag));
        }

        sender.sendMessage("§6§l══════════════════════════════");
        return true;
    }
}
