package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command: /settimer <seconds>
 * Mengatur durasi timer soal berikutnya tanpa edit kode.
 */
public class SetTimerCommand implements CommandExecutor {
    private final GameManager gm;

    public SetTimerCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /settimer <seconds>");
            sender.sendMessage("§7Timer saat ini: §f" + gm.getQuestionTimeSeconds() + " detik");
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cDurasi harus angka (detik).");
            return true;
        }

        String error = gm.setQuestionTimeSeconds(seconds);
        if (error != null) {
            sender.sendMessage(error);
            return true;
        }

        sender.sendMessage("§a✓ Timer soal diubah menjadi §f" + gm.getQuestionTimeSeconds() + " detik§a (berlaku untuk soal berikutnya)." );
        return true;
    }
}

