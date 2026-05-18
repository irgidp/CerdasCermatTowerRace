package yt.corazonid.cerdasCermatTowerRace.manager;

import yt.corazonid.cerdasCermatTowerRace.CerdasCermatTowerRace;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import yt.corazonid.cerdasCermatTowerRace.model.Question;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BotManager {

    private final CerdasCermatTowerRace plugin;
    private final GameManager gameManager;
    private final Map<String, BotLevel> botLevels = new LinkedHashMap<>();
    private final List<BukkitTask> activeTasks    = new ArrayList<>();

    public enum BotLevel { EASY, MEDIUM, HARD, RANDOM }

    public BotManager(CerdasCermatTowerRace plugin, GameManager gameManager) {
        this.plugin      = plugin;
        this.gameManager = gameManager;
    }

    public String addBot(String botName, BotLevel level, org.bukkit.World world) {
        if (gameManager.getRegisteredPlayers().containsKey(botName))
            return "§cNama '" + botName + "' sudah dipakai!";
        if (gameManager.getRegisteredPlayers().size() >= 8)
            return "§cSudah penuh! Maksimal 8 slot.";

        int slot = gameManager.getRegisteredPlayers().size() + 1;
        GamePlayer botGP = new GamePlayer(botName, slot, world);
        gameManager.getRegisteredPlayers().put(botName, botGP);
        botLevels.put(botName, level);

        Bukkit.broadcastMessage("§e🤖 Bot §f" + botName + " §e[" + level.name() + "] §etambah ke Slot " + slot);
        return null;
    }

    public boolean removeBot(String botName) {
        if (!botLevels.containsKey(botName)) return false;
        GamePlayer gp = gameManager.getRegisteredPlayers().get(botName);
        if (gp != null && gp.getVillager() != null && !gp.getVillager().isDead()) gp.getVillager().remove();
        gameManager.getRegisteredPlayers().remove(botName);
        botLevels.remove(botName);
        Bukkit.broadcastMessage("§7🤖 Bot " + botName + " dihapus.");
        return true;
    }

    public boolean isBot(String name)           { return botLevels.containsKey(name); }
    public Set<String> getBotNames()            { return botLevels.keySet(); }
    public BotLevel getBotLevel(String botName) { return botLevels.get(botName); }

    /**
     * Trigger semua bot untuk jawab soal setelah delay.
     * Waktu jawab disesuaikan sistem baru (max 25 detik):
     *   HARD   → 1-4 detik  (+5/+4 block)
     *   MEDIUM → 5-14 detik (+4/+3/+2 block)
     *   EASY   → 15-27 detik (+1/+0 block)
     */
    public void triggerBotAnswers(Question question) {
        cancelAllTasks();
        Random rng = new Random();

        for (Map.Entry<String, BotLevel> entry : botLevels.entrySet()) {
            String   botName = entry.getKey();
            BotLevel level   = entry.getValue();

            GamePlayer gp = gameManager.getRegisteredPlayers().get(botName);
            if (gp == null || !gp.isAlive()) continue;

            long delayTicks = calculateAnswerDelay(level, rng);

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!gameManager.isGameActive() || gameManager.isQuestionClosed()) return;
                if (gameManager.getCurrentQuestion() == null) return;
                if (!gameManager.getCurrentQuestion().getId().equals(question.getId())) return;

                GamePlayer botGP = gameManager.getRegisteredPlayers().get(botName);
                if (botGP == null || !botGP.isAlive()) return;

                double secondsTaken = delayTicks / 20.0;
                gameManager.processBotAnswer(botName, question.getJawaban(), secondsTaken);

            }, delayTicks);

            activeTasks.add(task);
        }
    }

    private long calculateAnswerDelay(BotLevel level, Random rng) {
        BotLevel effective = level;
        if (level == BotLevel.RANDOM) {
            BotLevel[] levels = {BotLevel.EASY, BotLevel.MEDIUM, BotLevel.HARD};
            effective = levels[rng.nextInt(levels.length)];
        }
        return switch (effective) {
            case HARD   -> 20  + rng.nextInt(60);   // 1-4 detik → +5 atau +4 block
            case MEDIUM -> 100 + rng.nextInt(180);  // 5-14 detik → +4/+3/+2 block
            case EASY   -> 300 + rng.nextInt(200);  // 15-25 detik → +1/+0 block
            default     -> 300;
        };
    }

    public void cancelAllTasks() {
        for (BukkitTask t : activeTasks) if (!t.isCancelled()) t.cancel();
        activeTasks.clear();
    }

    public void reset() {
        cancelAllTasks();
        botLevels.clear();
    }
}
