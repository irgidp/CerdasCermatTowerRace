package yt.corazonid.cerdasCermatTowerRace.commands;

import yt.corazonid.cerdasCermatTowerRace.manager.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Set;

/**
 * Command: /setsoal <question_id>
 *
 * Contoh penggunaan:
 *   /setsoal q1   → load soal dengan ID "q1" dari questions.yml
 *   /setsoal q5   → load soal "q5"
 *
 * Saat command ini dijalankan:
 * 1. Soal di-broadcast ke semua player
 * 2. Timer mulai
 * 3. Lava naik sesuai aturan nomor soal
 */
public class SetSoalCommand implements CommandExecutor {
    private final GameManager gm;

    public SetSoalCommand(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cerdasCermatTowerRace.admin")) {
            sender.sendMessage("§cKamu tidak punya izin!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /setsoal <question_id>");

            // Tampilkan daftar soal yang tersedia sebagai bantuan
            Set<String> ids = gm.getQuestionIds();
            if (!ids.isEmpty()) {
                sender.sendMessage("§7Soal tersedia: §f" + String.join(", ", ids));
            }
            return true;
        }

        String questionId = args[0];
        String error = gm.setQuestion(questionId);

        if (error != null) {
            sender.sendMessage(error);
            // Tampilkan daftar soal yang tersedia
            Set<String> ids = gm.getQuestionIds();
            if (!ids.isEmpty()) {
                sender.sendMessage("§7Soal tersedia: §f" + String.join(", ", ids));
            }
        } else {
            sender.sendMessage("§a✓ Soal '" + questionId + "' berhasil diset!");
        }

        return true;
    }
}
