package yt.corazonid.cerdasCermatTowerRace.manager;

import yt.corazonid.cerdasCermatTowerRace.CerdasCermatTowerRace;
import yt.corazonid.cerdasCermatTowerRace.listener.ArrowListener;
import org.bukkit.plugin.RegisteredListener;
import yt.corazonid.cerdasCermatTowerRace.model.GamePlayer;
import yt.corazonid.cerdasCermatTowerRace.model.Question;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// import org.bukkit.potion.PotionEffect;
// import org.bukkit.potion.PotionEffectType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GameManager {

    private final CerdasCermatTowerRace plugin;
    private final Map<String, GamePlayer> registeredPlayers = new LinkedHashMap<>();
    private BotManager botManager;

    // Soal
    private final Map<String, Question> questions  = new HashMap<>();
    private Question currentQuestion               = null;
    private long     questionStartTime             = 0;
    private int      questionNumber                = 0;
    private final Set<String> answeredThisQuestion = new HashSet<>();
    // Simpan jawaban tiap player untuk ditampilkan saat timeout
    private final Map<String, String> playerAnswers = new LinkedHashMap<>();
    // Timer task untuk timeout 25 detik
    private org.bukkit.scheduler.BukkitTask timeoutTask = null;
    // Apakah soal sudah ditutup (timeout/semua jawab)
    private boolean questionClosed = false;

    // State
    private boolean gameActive   = false;
    private Location boardOrigin = null;
    private int      lavaY       = 0;
    private int      nextLavaRise = 0;
    private final List<Location> placedBlocks = new ArrayList<>();

    // Leaderboard akhir (urutan mati, indeks 0 = mati pertama = rank terakhir)
    private final List<String> deathOrder = new ArrayList<>();

    // Warna concrete powder per slot (8 warna beda)
    private static final Material[] SLOT_COLORS = {
            null,                                    // index 0 unused
            Material.GOLD_BLOCK,         // Slot 1
            Material.EMERALD_BLOCK,           // Slot 2
            Material.DIAMOND_BLOCK,           // Slot 3
            Material.REDSTONE_BLOCK,        // Slot 4
            Material.COPPER_BLOCK,         // Slot 5
            Material.COAL_BLOCK,           // Slot 6
            Material.NETHERITE_BLOCK,     // Slot 7
            Material.IRON_BLOCK,          // Slot 8
    };

    // Layout slot — sesuai foto lo:
    // Baris atas  : 5  1  8(=slot7 di foto)  → tapi lo foto urutan 5,1,8
    // Baris tengah: 3     4
    // Baris bawah : 7  2  6
    // Spacing = 5 (jarak pusat ke pusat, biar ada gap 3 block di antara pijakan 1x1 sesuai req)
    // Foto lo: jarak blok ke blok = 4 kotak grid ≈ 4 block → spacing 4
    private static final int SPACING = 4;
    private static final int[][] SLOT_POSITIONS = {
            {0, 0},                         // 0: unused
            {SPACING,     0},               // Slot 1: tengah atas
            {SPACING,     SPACING * 2},     // Slot 2: tengah bawah
            {0,           SPACING},         // Slot 3: kiri tengah
            {SPACING * 2, SPACING},         // Slot 4: kanan tengah
            {0,           0},               // Slot 5: kiri atas
            {SPACING * 2, SPACING * 2},     // Slot 6: kanan bawah
            {0,           SPACING * 2},     // Slot 7: kiri bawah
            {SPACING * 2, 0},               // Slot 8: kanan atas
    };

    public GameManager(CerdasCermatTowerRace plugin) {
        this.plugin     = plugin;
        this.botManager = new BotManager(plugin, this);
        loadQuestions();
    }

    // ═══ LOAD SOAL ════════════════════════════════════════════════════════════

    private void loadQuestions() {
        InputStream is = plugin.getResource("questions.yml");
        if (is == null) { plugin.getLogger().warning("questions.yml tidak ditemukan!"); return; }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
        if (!cfg.contains("questions")) return;
        for (String key : cfg.getConfigurationSection("questions").getKeys(false)) {
            String soal    = cfg.getString("questions." + key + ".soal", "");
            String jawaban = cfg.getString("questions." + key + ".jawaban", "");
            questions.put(key.toLowerCase(), new Question(key, soal, jawaban));
        }
        plugin.getLogger().info("[CerdasCermatTowerRace] Loaded " + questions.size() + " soal.");
    }

    // ═══ REGISTRASI ════════════════════════════════════════════════════════════

    public String registerPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null)                            return "§cPlayer '" + playerName + "' tidak online!";
        if (registeredPlayers.containsKey(playerName)) return "§cPlayer '" + playerName + "' sudah terdaftar!";
        if (registeredPlayers.size() >= 8)             return "§cSudah penuh! Maksimal 8 slot.";
        int slot = registeredPlayers.size() + 1;
        registeredPlayers.put(playerName, new GamePlayer(playerName, player, slot));
        return null;
    }

    public boolean unregisterPlayer(String name) { return registeredPlayers.remove(name) != null; }
    public Map<String, GamePlayer> getRegisteredPlayers() { return registeredPlayers; }
    public GamePlayer getGamePlayer(String name)           { return registeredPlayers.get(name); }
    public boolean isRegistered(String name)               { return registeredPlayers.containsKey(name); }
    public BotManager getBotManager()                      { return botManager; }
    public boolean isGameActive()                          { return gameActive; }
    public Question getCurrentQuestion()                   { return currentQuestion; }
    public int getLavaY()                                  { return lavaY; }
    public Location getBoardOrigin()                       { return boardOrigin; }
    public Set<String> getQuestionIds()                    { return questions.keySet(); }
    public boolean isQuestionClosed()                      { return questionClosed; }

    public GamePlayer getGamePlayerByPlayer(Player player) {
        return registeredPlayers.get(player.getName());
    }

    // ═══ BUILD BOARD ══════════════════════════════════════════════════════════

    public String buildBoard(Location origin) {
        if (registeredPlayers.isEmpty()) return "§cTidak ada player / bot!";

        this.boardOrigin = origin.clone();
        placedBlocks.clear();

        int startY = origin.getBlockY();
        World world = origin.getWorld();

        lavaY = startY -1;
        spawnLavaFloor(world, origin.getBlockX(), lavaY, origin.getBlockZ());

        for (GamePlayer gp : registeredPlayers.values()) {
            int slot = gp.getSlot();
            int px   = origin.getBlockX() + SLOT_POSITIONS[slot][0] - 4;
            int pz   = origin.getBlockZ() + SLOT_POSITIONS[slot][1] - 4;
            int py   = startY - 1;

            gp.setBaseX(px);
            gp.setBaseZ(pz);
            gp.setPlatformY(py);
            gp.setWorld(world);

            // Spawn gold block (pijakan dasar)
            // placeBlock(world, px, py, pz, Material.GOLD_BLOCK);

            // Spawn concrete powder menara (4 block ke atas, warna per slot)
            Material color = SLOT_COLORS[slot];
            for (int i = 0; i <= 3; i++) {
                placeBlock(world, px, py + i, pz, color);
            }

            // Teleport player asli
            if (!gp.isBot()) {
                Player p = gp.getPlayer();
                if (p != null && p.isOnline()) {
                    p.teleport(new Location(world, px + 0.5, py + 4, pz + 0.5));
                }
            } else {
                // Spawn villager sebagai representasi bot
                spawnBotVillager(gp, world, px, py + 4, pz);
            }

            gp.setFrozen(true);
            Bukkit.broadcastMessage("§a  ✓ " + gp.getPlayerName()
                    + (gp.isBot() ? " §7[BOT]" : "") + " §a→ Slot " + slot);
        }

        // applyGameRestrictions();
        return null;
    }

    private void spawnBotVillager(GamePlayer gp, World world, int x, int y, int z) {
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        Villager v = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
        v.setCustomName("§e" + gp.getPlayerName() + " §7[BOT]");
        v.setCustomNameVisible(true);
        v.setAI(false);             // Tidak bergerak
        v.setInvulnerable(false);   // Bisa kena snowball/arrow
        v.setGravity(true);
        v.setSilent(true);
        gp.setVillager(v);
    }

    private void placeBlock(World world, int x, int y, int z, Material mat) {
        Block b = world.getBlockAt(x, y, z);

        // Paksa perubahan blok agar selalu berjalan di Main Thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            b.setType(mat);
            placedBlocks.add(b.getLocation());
        });
    }

    private void spawnLavaFloor(World world, int cx, int y, int cz) {
        // 21x21: dari cx-10 sampai cx+10
        for (int x = cx - 10; x <= cx + 10; x++) {
            for (int z = cz - 10; z <= cz + 10; z++) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() != Material.AIR) {
                    continue;
                }
                b.setType(Material.LAVA, true);
                placedBlocks.add(b.getLocation());
            }
        }
    }

    // private void applyGameRestrictions() {
    //     for (GamePlayer gp : registeredPlayers.values()) {
    //         if (gp.isBot()) continue;
    //         Player p = gp.getPlayer();
    //         if (p == null || !p.isOnline()) continue;
    //         p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   99999, 254, false, false));
    //         p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 99999, 128, false, false));
    //     }
    // }

    // private void removeGameRestrictions() {
    //     for (GamePlayer gp : registeredPlayers.values()) {
    //         if (gp.isBot()) continue;
    //         Player p = gp.getPlayer();
    //         if (p == null || !p.isOnline()) continue;
    //         p.removePotionEffect(PotionEffectType.SLOWNESS);
    //         p.removePotionEffect(PotionEffectType.JUMP_BOOST);
    //     }
    // }

    // ═══ START GAME ════════════════════════════════════════════════════════════

    public String startGame() {
        if (registeredPlayers.isEmpty()) return "§cTidak ada player!";
        if (gameActive)                  return "§cGame sudah berjalan!";
        if (boardOrigin == null)         return "§cJalankan /startboard dulu!";
        gameActive = true;
        for (GamePlayer gp : registeredPlayers.values()) {
            if (gp.isBot()) continue;
            Player p = gp.getPlayer();
            if (p == null || !p.isOnline()) continue;
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
        }
        Bukkit.broadcastMessage("§6§l══════════════════════════════");
        Bukkit.broadcastMessage("§6§l  ⚡ GAME DIMULAI! ⚡");
        Bukkit.broadcastMessage("§6§l══════════════════════════════");
        return null;
    }

    // ═══ SET SOAL ══════════════════════════════════════════════════════════════

    public String setQuestion(String questionId) {
        if (!gameActive) return "§cGame belum dimulai!";
        Question q = questions.get(questionId.toLowerCase());
        if (q == null)   return "§cSoal '" + questionId + "' tidak ditemukan!";

        // Cancel timeout soal sebelumnya
        if (timeoutTask != null && !timeoutTask.isCancelled()) timeoutTask.cancel();

        currentQuestion   = q;
        questionStartTime = System.currentTimeMillis();
        questionNumber++;
        answeredThisQuestion.clear();
        playerAnswers.clear();
        questionClosed = false;

        // Hitung berapa block lava akan naik
        int[] lavaRange = getLavaRiseRange(questionNumber);
        this.nextLavaRise = lavaRange[0] + new Random().nextInt(lavaRange[1] - lavaRange[0] + 1);

        Bukkit.broadcastMessage("§b§l══════════════════════════════");
        Bukkit.broadcastMessage("§b§l  📐 SOAL #" + questionNumber);
        Bukkit.broadcastMessage("§f  " + q.getSoal());
        Bukkit.broadcastMessage("§7  Ketik jawaban langsung di chat! (maks §e25 detik§7)");
        Bukkit.broadcastMessage("§c  🌋 Lava akan naik §f" + this.nextLavaRise + " §cblock setelah soal ini!");
        Bukkit.broadcastMessage("§b§l══════════════════════════════");

        // Bot jawab otomatis
        botManager.triggerBotAnswers(q);

        // Timeout 25 detik — tutup soal, tampilkan recap, gacha, naikkan lava
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, this::closeQuestion, 25 * 20L);

        return null;
    }

    /** Dipanggil saat 25 detik habis atau semua player sudah jawab */
    public void closeQuestion() {
        if (questionClosed) return;
        questionClosed = true;

        if (timeoutTask != null && !timeoutTask.isCancelled()) timeoutTask.cancel();
        botManager.cancelAllTasks();

        Question q = currentQuestion;
        if (q == null) return;

        // Tampilkan rekap jawaban
        Bukkit.broadcastMessage("§e§l════════ WAKTU HABIS / RECAP ════════");
        Bukkit.broadcastMessage("§7Jawaban yang benar: §a" + q.getJawaban());
        Bukkit.broadcastMessage("");

        // Tampilkan jawaban semua player (yang sempat jawab)
        for (GamePlayer gp : registeredPlayers.values()) {
            if (!gp.isAlive()) continue;
            String jawaban = playerAnswers.getOrDefault(gp.getPlayerName(), null);
            if (jawaban != null) {
                boolean benar = answeredThisQuestion.contains(gp.getPlayerName());
                String warna  = benar ? "§a" : "§c";
                Bukkit.broadcastMessage("  " + warna + gp.getPlayerName()
                        + (gp.isBot() ? " §7[BOT]" : "")
                        + " §fmenjawab §f" + jawaban + " " + (benar ? "§a✓" : "§c✗"));
            } else if (!gp.isBot()) {
                Bukkit.broadcastMessage("  §7" + gp.getPlayerName() + " tidak menjawab.");
            }
        }
        Bukkit.broadcastMessage("§e§l══════════════════════════════");

        // Gacha item untuk semua player hidup (bukan bot) setelah 1 detik
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gachaItemForAll();
            // Naikkan lava setelah gacha (2 detik lagi)
            Bukkit.getScheduler().runTaskLater(plugin, this::raiseLavaForQuestion, 40L);
        }, 20L);
    }

    // ═══ PROSES JAWABAN PLAYER ASLI ═══════════════════════════════════════════

    public boolean processChat(String playerName, String message) {
        if (currentQuestion == null || !gameActive || questionClosed) return false;
        GamePlayer gp = registeredPlayers.get(playerName);
        if (gp == null || gp.isBot() || !gp.isAlive()) return false;

        if (answeredThisQuestion.contains(playerName)) {
            Player p = gp.getPlayer();
            if (p != null) p.sendMessage("§7[Kamu sudah menjawab di soal ini]");
            return true;
        }

        // Rekam jawaban (benar atau salah)
        playerAnswers.put(playerName, message);

        if (!currentQuestion.isCorrect(message)) {
            // Jawaban salah — sembunyikan dari chat tapi tidak ada feedback
            return true;
        }

        // BENAR!
        answeredThisQuestion.add(playerName);
        long   ms      = System.currentTimeMillis() - questionStartTime;
        double sec     = ms / 1000.0;
        int    blocks  = calculateBlocksUp(sec);

        Bukkit.broadcastMessage("§a✓ §f" + playerName + " §atelah menjawab dengan benar! §7(+" + blocks + " block, " + String.format("%.1f", sec) + "s)");
        if (blocks > 0) raisePlatform(gp, blocks);

        // Cek apakah semua player hidup sudah jawab → tutup soal lebih cepat
        checkAllAnswered();
        return true;
    }

    /** Cek apakah semua player ASLI yang hidup sudah menjawab */
    private void checkAllAnswered() {
        long aliveReal = registeredPlayers.values().stream()
                .filter(g -> g.isAlive() && !g.isBot()).count();
        long answered  = registeredPlayers.values().stream()
                .filter(g -> g.isAlive() && !g.isBot() && answeredThisQuestion.contains(g.getPlayerName())).count();
        if (aliveReal > 0 && answered >= aliveReal) {
            // Semua player asli sudah jawab, tutup soal setelah 2 detik
            if (!questionClosed) {
                Bukkit.getScheduler().runTaskLater(plugin, this::closeQuestion, 40L);
            }
        }
    }

    // ═══ PROSES JAWABAN BOT ════════════════════════════════════════════════════

    public void processBotAnswer(String botName, String answer, double secondsTaken) {
        if (currentQuestion == null || !gameActive || questionClosed) return;
        if (answeredThisQuestion.contains(botName)) return;
        GamePlayer gp = registeredPlayers.get(botName);
        if (gp == null || !gp.isAlive() || !gp.isBot()) return;

        answeredThisQuestion.add(botName);
        playerAnswers.put(botName, answer);
        int blocks = calculateBlocksUp(secondsTaken);

        Bukkit.broadcastMessage("§a✓ §f" + botName + " §7[BOT] §atelah menjawab dengan benar! §7(+" + blocks + " block, " + String.format("%.1f", secondsTaken) + "s)");
        if (blocks > 0) raisePlatform(gp, blocks);
    }

    // ═══ HITUNG BLOCK NAIK ════════════════════════════════════════════════════

    private int calculateBlocksUp(double seconds) {
        if (seconds < 5)  return 5;
        if (seconds < 10) return 4;
        if (seconds < 15) return 3;
        if (seconds < 20) return 2;
        if (seconds < 25) return 1;
        return 0;
    }

    // ═══ NAIKKAN / TURUNKAN PIJAKAN ═══════════════════════════════════════════

    public void raisePlatform(GamePlayer gp, int blocks) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = gp.getEffectiveWorld();
            if (world == null) return;

            Material color = SLOT_COLORS[gp.getSlot()];

            // Jalankan loop sebanyak jumlah block yang didapat
            for (int b = 0; b < blocks; b++) {
                int finalB = b; // <-- BUAT VARIABEL PEMBANTU INI

                // Berikan delay bertahap kelipatan 20 tick (1 detik) per block
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!gp.isAlive() || !gameActive) return;

                    int targetSpawnY = gp.getPlatformY() + 4;
                    placeBlock(world, gp.getBaseX(), targetSpawnY, gp.getBaseZ(), color);

                    gp.raisePlatform(1);
                    int currentY = gp.getPlatformY();

                    if (!gp.isBot()) {
                        Player p = gp.getPlayer();
                        if (p != null && p.isOnline()) {
                            // 1. Ambil lokasi player SAAT INI untuk meng-copy Yaw dan Pitch-nya
                            Location currentLoc = p.getLocation();

                            // 2. Buat lokasi target Y yang baru, tapi masukkan Yaw dan Pitch asli player
                            Location targetLoc = new Location(
                                    world,
                                    gp.getBaseX() + 0.5,
                                    currentY + 4,
                                    gp.getBaseZ() + 0.5,
                                    currentLoc.getYaw(),   // Pertahankan arah hadap horizontal
                                    currentLoc.getPitch()  // Pertahankan arah hadap vertikal
                            );

                            p.teleport(targetLoc);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.2f + ((float) finalB * 0.1f));
                        }
                    } else {
                        Villager v = gp.getVillager();
                        if (v != null && !v.isDead()) {
                            v.teleport(new Location(world, gp.getBaseX() + 0.5, currentY + 4, gp.getBaseZ() + 0.5));
                        }
                    }
                }, (long) finalB * 10L);
            }
        });
    }

    public void lowerPlatform(GamePlayer gp, int blocks) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = gp.getEffectiveWorld();
            if (world == null) return;

            // Jalankan loop penurunan sebanyak jumlah block yang dikurangi
            for (int b = 0; b < blocks; b++) {
                int finalB = b; // <-- BUAT VARIABEL PEMBANTU INI

                // Berikan delay bertahap kelipatan 20 tick (1 detik) per block
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!gp.isAlive() || !gameActive) return;

                    int targetAirY = gp.getPlatformY() + 3;
                    world.getBlockAt(gp.getBaseX(), targetAirY, gp.getBaseZ()).setType(Material.AIR);

                    gp.lowerPlatform(1);
                    int currentY = gp.getPlatformY();

                    if (!gp.isBot()) {
                        Player p = gp.getPlayer();
                        if (p != null && p.isOnline()) {
                            // 1. Ambil lokasi player SAAT INI untuk meng-copy Yaw dan Pitch-nya
                            Location currentLoc = p.getLocation();

                            // 2. Buat lokasi target Y yang baru dengan tetap membawa Yaw dan Pitch asli
                            Location targetLoc = new Location(
                                    world,
                                    gp.getBaseX() + 0.5,
                                    currentY + 4,
                                    gp.getBaseZ() + 0.5,
                                    currentLoc.getYaw(),   // Pertahankan arah hadap horizontal
                                    currentLoc.getPitch()  // Pertahankan arah hadap vertikal
                            );

                            p.teleport(targetLoc);
                            p.playSound(p.getLocation(), Sound.BLOCK_DEEPSLATE_STEP, 0.7f, 0.6f);
                        }
                    } else {
                        Villager v = gp.getVillager();
                        if (v != null && !v.isDead()) {
                            v.teleport(new Location(world, gp.getBaseX() + 0.5, currentY + 4, gp.getBaseZ() + 0.5));
                        }
                    }

                    if ((currentY + 4) <= lavaY) {
                        handlePlayerDeath(gp.getPlayerName());
                    }
                }, (long) finalB * 10L); // <-- Ganti variabel b di sini menjadi finalB
            }
        });
    }

    // ═══ GACHA ITEM ════════════════════════════════════════════════════════════

    /**
     * Gacha item untuk semua player hidup (bukan bot).
     * 60% snowball → kurangi pijakan musuh 1 block (kena player)
     * 40% bow+arrow → kurangi pijakan musuh 3 block (kena player), efek shake kamera
     */
    private void gachaItemForAll() {
        List<GamePlayer> alivePlayers = registeredPlayers.values().stream()
                .filter(g -> g.isAlive() && !g.isBot()).toList();
        if (alivePlayers.isEmpty()) return;

        Random rng = new Random();
        ArrowListener arrowListener = null;
        for (RegisteredListener rl : org.bukkit.event.player.PlayerItemHeldEvent.getHandlerList().getRegisteredListeners()) {
            if (rl.getListener() instanceof ArrowListener al) {
                arrowListener = al;
                break;
            }
        }
        for (GamePlayer gp : alivePlayers) {
            Player p = gp.getPlayer();
            if (p == null || !p.isOnline()) continue;

            boolean getBow = rng.nextInt(100) < 40; // 40% bow, 60% snowball

            if (getBow) {
                // Bow 1 durability + 1 arrow
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta meta = bow.getItemMeta();
                meta.setUnbreakable(false);
                // Set damage supaya tinggal 1 durability (durability max bow = 384)
                if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                    dmg.setDamage(383); // 384 - 1 = tinggal 1 hit
                }
                bow.setItemMeta(meta);
                p.getInventory().addItem(bow);
                p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                Bukkit.broadcastMessage("§e🏹 " + gp.getPlayerName() + " mendapatkan §6Bow & Arrow§e!");
                if (arrowListener != null && p.getInventory().getItemInMainHand().getType() == Material.BOW) {
                    arrowListener.startCameraShake(p);
                }
            } else {
                // Snowball
                p.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
                Bukkit.broadcastMessage("§b❄ " + gp.getPlayerName() + " mendapatkan §fSnowball§b!");
            }
        }
    }

    // ═══ NAIKKAN LAVA ══════════════════════════════════════════════════════════

    private void raiseLavaForQuestion() {
        if (boardOrigin == null) return;
        int rise = this.nextLavaRise;

        Bukkit.broadcastMessage("§c§l🌋 LAVA NAIK " + this.nextLavaRise + " BLOCK!");

        World world = boardOrigin.getWorld();
        int cx = boardOrigin.getBlockX(), cz = boardOrigin.getBlockZ();

        for (int i = 0; i < rise; i++) {
            lavaY++;
            final int targetY = lavaY;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                            spawnLavaFloor(world, cx, targetY, cz),
                    (long) i * 10L   // Animasi: setiap 10 tick (0.5 detik) naik 1 layer
            );
        }

        // Setelah lava naik, cek bot yang tenggelam
        Bukkit.getScheduler().runTaskLater(plugin, this::checkBotLavaDeath, (long) rise * 10L + 10L);
    }

    private int[] getLavaRiseRange(int questionNum) {
        if (questionNum <= 2)  return new int[]{1, 2};
        if (questionNum <= 5)  return new int[]{2, 3};
        if (questionNum <= 10) return new int[]{3, 5};
        if (questionNum <= 15) return new int[]{3, 7};
        if (questionNum <= 20) return new int[]{5, 9};
        return new int[]{7, 10};
    }

    // ═══ CEK BOT MATI KENA LAVA ════════════════════════════════════════════════

    private void checkBotLavaDeath() {
        for (GamePlayer gp : new ArrayList<>(registeredPlayers.values())) {
            if (!gp.isBot() || !gp.isAlive()) continue;
            if ((gp.getPlatformY() + 4) <= lavaY) handlePlayerDeath(gp.getPlayerName());
        }
    }

    // ═══ KEMATIAN ══════════════════════════════════════════════════════════════

    public void handlePlayerDeath(String playerName) {
        GamePlayer gp = registeredPlayers.get(playerName);
        if (gp == null || !gp.isAlive()) return;

        gp.setAlive(false);
        deathOrder.add(playerName);

        long aliveCount = registeredPlayers.values().stream().filter(GamePlayer::isAlive).count();
        int  deduction  = (int) aliveCount;
        gp.deductPoint(deduction);

        String tag = gp.isBot() ? " §7[BOT]" : "";
        Bukkit.broadcastMessage("§c💀 " + playerName + tag + " terkena LAVA! §7(-" + deduction + " poin, sisa: §e" + gp.getPoint() + "§7)");

        // Set spectator untuk player asli
        if (!gp.isBot()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = gp.getPlayer();
                if (p != null && p.isOnline()) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("§cKamu terkena lava! Sekarang spectator.");
                }
            }, 1L);
        } else {
            // Remove villager
            Villager v = gp.getVillager();
            if (v != null && !v.isDead()) v.remove();
        }

        // Hapus menara
        World world = gp.getEffectiveWorld();
        // Ubah bagian hapus menara di handlePlayerDeath menjadi:
        if (world != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i <= 4; i++) {
                    world.getBlockAt(gp.getBaseX(), gp.getPlatformY() + i, gp.getBaseZ()).setType(Material.AIR);
                }
            });
        }

        // Cek kondisi menang
        checkWinCondition();
    }

    private void checkWinCondition() {
        List<GamePlayer> alive = registeredPlayers.values().stream()
                .filter(GamePlayer::isAlive).toList();

        if (alive.size() <= 1) {
            // Game selesai!
            Bukkit.getScheduler().runTaskLater(plugin, () -> announceResults(alive), 20L);
        }
    }

    private void announceResults(List<GamePlayer> remaining) {
        gameActive = false;
        if (timeoutTask != null && !timeoutTask.isCancelled()) timeoutTask.cancel();
        botManager.cancelAllTasks();

        // Buat urutan ranking (pemenang dulu, lalu urutan terbalik deathOrder)
        List<String> ranking = new ArrayList<>();
        // Yang masih hidup = juara 1
        for (GamePlayer gp : remaining) ranking.add(gp.getPlayerName());
        // Yang mati: kebalik (mati terakhir = rank lebih tinggi)
        List<String> reversed = new ArrayList<>(deathOrder);
        Collections.reverse(reversed);
        ranking.addAll(reversed);

        Bukkit.broadcastMessage("§6§l══════════════════════════════");
        Bukkit.broadcastMessage("§6§l  🏆 GAME SELESAI! 🏆");
        Bukkit.broadcastMessage("§6§l══════════════════════════════");

        String[] medals = {"§6🥇", "§7🥈", "§c🥉", "§f4.", "§f5.", "§f6.", "§f7.", "§f8."};
        for (int i = 0; i < ranking.size(); i++) {
            String name = ranking.get(i);
            GamePlayer gp = registeredPlayers.get(name);
            int poin = gp != null ? gp.getPoint() : 0;
            String medal = i < medals.length ? medals[i] : "§f" + (i+1) + ".";
            Bukkit.broadcastMessage(medal + " §f" + name
                    + (gp != null && gp.isBot() ? " §7[BOT]" : "")
                    + " §7- §b" + poin + " poin");
        }
        Bukkit.broadcastMessage("§6§l══════════════════════════════");

        // Set pemenang ke survival / keluar spectator
        if (!remaining.isEmpty()) {
            GamePlayer winner = registeredPlayers.get(remaining.get(0).getPlayerName());
            if (winner != null && !winner.isBot()) {
                Player p = winner.getPlayer();
                if (p != null && p.isOnline()) {
                    p.sendMessage("§6§l🏆 SELAMAT! Kamu adalah PEMENANG!");
                }
            }
        }
    }

    // ═══ CLEANUP ════════════════════════════════════════════════════════════════

    public void cleanupBlocks() {
        for (Location loc : placedBlocks) loc.getBlock().setType(Material.AIR);
        placedBlocks.clear();
    }

    public void resetGame() {
        if (timeoutTask != null && !timeoutTask.isCancelled()) timeoutTask.cancel();
        cleanupBlocks();
        // removeGameRestrictions();
        // Remove semua villager bot
        for (GamePlayer gp : registeredPlayers.values()) {
            if (gp.isBot()) {
                Villager v = gp.getVillager();
                if (v != null && !v.isDead()) v.remove();
            }
        }
        botManager.reset();
        registeredPlayers.clear();
        currentQuestion   = null;
        questionNumber    = 0;
        questionStartTime = 0;
        questionClosed    = false;
        answeredThisQuestion.clear();
        playerAnswers.clear();
        deathOrder.clear();
        gameActive        = false;
        boardOrigin       = null;
        lavaY             = 0;
        Bukkit.broadcastMessage("§7[CerdasCermatTowerRace] Game direset.");
    }
}
