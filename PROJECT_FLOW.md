# CerdasCermatTowerRace - Project Flow

Dokumen ini merangkum alur kerja (flow) dan urutan eksekusi plugin dari inisialisasi, command, event listener, sampai game selesai dan reset.

## 1. Ringkasan Arsitektur

- **Entry point**: `yt.corazonid.cerdasCermatTowerRace.CerdasCermatTowerRace`
- **Core logic**: `yt.corazonid.cerdasCermatTowerRace.manager.GameManager`
- **Bot control**: `yt.corazonid.cerdasCermatTowerRace.manager.BotManager`
- **Model**: `GamePlayer`, `Question`
- **Input/Output**:
  - Commands di `commands/*`
  - Event listeners di `listener/*`
  - Soal di `src/main/resources/questions.yml`

## 2. Flow Startup Plugin

1. **Server memuat plugin** → memanggil `onEnable()` di `CerdasCermatTowerRace`.
2. **GameManager dibuat**:
   - `GameManager` konstruktor memanggil `loadQuestions()`.
   - `questions.yml` dibaca dan disimpan ke `Map<String, Question>`.
   - Timer soal default di-set ke **15 detik** (bisa diubah via `/settimer`).
3. **Register command executors** untuk semua command (`/regis`, `/addbot`, dst).
4. **Register event listeners**:
   - `ChatListener`
   - `PlayerDeathListener`
   - `SnowballListener`
   - `ArrowListener`
   - `PlayerMoveListener`
   - `EntityDamageListener`
5. **Log startup** ditampilkan di console.

## 3. Struktur Data Inti

### 3.1 `GameManager`
- `registeredPlayers`: player asli + bot (maks 8).
- `questions`: bank soal hasil load `questions.yml`.
- `currentQuestion`, `questionStartTime`, `questionNumber`.
- `answeredThisQuestion`, `playerAnswers`, `questionClosed`.
- `questionTimeSeconds`: durasi timer soal (default 15, range 5–60 detik).
- `gameActive`, `boardOrigin`, `lavaY`, `nextLavaRise`, `placedBlocks`.
- `deathOrder`: urutan mati untuk ranking akhir.

### 3.2 `GamePlayer`
- Identitas: `playerName`, `slot`, `isBot`.
- Posisi menara: `baseX`, `baseZ`, `platformY`.
- Status: `alive`, `frozen`.
- Poin dan streak (`correctStreak`, `rewardedStreakMilestones`).
- Untuk bot: `Villager` sebagai representasi di dunia.

### 3.3 `Question`
- `id`, `soal`, `jawaban`.
- `isCorrect()` melakukan trim dan case-insensitive compare.

## 4. Flow Registrasi Peserta

### 4.1 `/regis <player>`
1. Cek permission `cerdasCermatTowerRace.admin`.
2. `GameManager.registerPlayer()`:
   - Pastikan player online.
   - Cek slot <= 8.
   - Masukkan ke `registeredPlayers`.
3. Broadcast info join + slot.

### 4.2 `/addbot <nama> [level]`
1. Cek permission.
2. Tentukan `world` dari sender (player) atau world default (console).
3. `BotManager.addBot()`:
   - Simpan `GamePlayer` bot ke `registeredPlayers`.
   - Simpan level bot ke `botLevels`.
4. Broadcast bot join + slot.

### 4.3 `/unregis <player>` dan `/removebot <bot>`
- Menghapus dari `registeredPlayers`.
- Bot akan dihapus juga dari `botLevels` dan `Villager` dibersihkan.

## 5. Flow Membangun Arena

### `/startboard`
1. Cek permission.
2. Sender wajib **player** (karena butuh lokasi).
3. `GameManager.buildBoard(location)`:
   - Set `boardOrigin`.
   - Bersihkan daftar `placedBlocks`.
   - Spawn lantai lava 21x21 di bawah arena (`lavaY = startY - 1`).
   - Untuk setiap `GamePlayer`:
     - Tentukan slot dan posisi base (layout 8 slot).
     - Spawn 4 blok menara (warna sesuai slot).
     - Teleport player ke puncak menara.
     - Jika bot, spawn `Villager` sebagai representasi.
     - Set `frozen = true`.

## 6. Flow Mulai Game

### `/startgame`
1. Cek permission.
2. `GameManager.startGame()`:
   - Validasi: ada player, board sudah dibuat.
   - Set `gameActive = true`.
   - Set gamemode player ke survival dan clear inventory.
3. Broadcast "Game dimulai".

## 7. Flow Soal dan Jawaban

### 7.1 `/settimer <seconds>`
1. Cek permission.
2. Validasi range (5–60 detik).
3. Set `questionTimeSeconds` di `GameManager`.
4. Timer baru berlaku untuk **soal berikutnya**.

### 7.2 `/setsoal <id>`
1. Cek permission.
2. `GameManager.setQuestion()`:
   - Validasi game aktif.
   - Ambil soal dari `questions`.
   - Reset state soal: `answeredThisQuestion`, `playerAnswers`, `questionClosed`.
   - Hitung **range kenaikan lava** berdasarkan nomor soal.
   - Broadcast soal + info lava + durasi timer.
   - Trigger jawaban bot otomatis (`BotManager.triggerBotAnswers`).
   - Start timeout sesuai `questionTimeSeconds` → `closeQuestion()`.

### 7.3 Jawaban Player (Chat)
1. `ChatListener.onChat()` menangkap chat.
2. `GameManager.processChat()`:
   - Validasi game aktif, soal berjalan.
   - Cek player belum jawab.
   - Simpan jawaban di `playerAnswers`.
   - Jika salah: reset streak.
   - Jika benar:
     - Tambah streak.
     - Hitung block naik berdasar waktu jawab (skala mengikuti timer).
     - Naikkan platform dengan animasi (`raisePlatform`).
3. Jika semua player hidup sudah menjawab, soal ditutup lebih cepat.

### 7.4 Jawaban Bot
1. `BotManager.triggerBotAnswers()` membuat task untuk setiap bot.
2. Delay bot menyesuaikan **durasi timer** (hard/medium/easy).
3. Setelah delay, `GameManager.processBotAnswer()`:
   - Jika masih soal yang sama, bot menjawab benar otomatis.
   - Platform bot naik sesuai waktu.

### 7.5 Penutupan Soal (Timeout / Semua Jawab)
1. `GameManager.closeQuestion()`:
   - Batalkan task timeout.
   - Batalkan task bot yang belum jalan.
   - Broadcast rekap jawaban (benar/salah/tidak jawab).
2. **Reward streak** untuk player asli.
3. Lava langsung naik setelah recap.

## 8. Flow Kena Serangan

### 8.1 Snowball
- `SnowballListener`:
  - Jika snowball mengenai player/villager lain: turunkan platform target 1 blok.

### 8.2 Arrow
- `ArrowListener`:
  - Jika arrow mengenai player/villager lain: turunkan platform target 3 blok.
  - Ada efek "camera shake" ketika player memegang bow.

### 8.3 Damage lainnya
- `EntityDamageListener`:
  - Blok PVP langsung (player vs player).
  - Bot villager hanya bisa kena projectile.
  - Blok fall damage untuk player aktif.

## 9. Flow Kematian dan Akhir Game

### 9.1 Kematian
- `PlayerDeathListener` memproses kematian player.
- `GameManager.handlePlayerDeath()`:
  - Set player mati (`alive = false`).
  - Masukkan ke `deathOrder`.
  - Kurangi poin berdasarkan jumlah player hidup.
  - Set spectator (player asli) / remove villager (bot).
  - Hapus menara dari dunia.
  - Cek kondisi menang.

### 9.2 Kondisi Menang
- Jika sisa pemain hidup <= 1:
  - `announceResults()` menampilkan ranking akhir.
  - Ranking = pemain hidup + `deathOrder` terbalik.
  - Game selesai (`gameActive = false`).

## 10. Flow Reset Game

### `/resetgame`
- Executor inline di `CerdasCermatTowerRace.onEnable()`.
- `GameManager.resetGame()`:
  - Batalkan semua task.
  - Bersihkan semua block arena (lava + menara).
  - Remove semua villager bot.
  - Reset streak & reward milestone untuk semua peserta.
  - Clear semua state (`registeredPlayers`, `currentQuestion`, `deathOrder`, dll).
  - Broadcast "Game direset".

## 11. Urutan Ideal Saat Play

1. **Admin**: `/regis <player>` atau `/addbot <nama> [level]`
2. **Admin**: `/startboard`
3. **Admin**: `/startgame`
4. **Admin**: (opsional) `/settimer <seconds>`
5. **Admin**: `/setsoal <id>`
6. **Player**: jawab di chat
7. **Loop**: soal berikutnya dengan `/setsoal`
8. **Game selesai otomatis** setelah tersisa 1 pemain.
9. **Admin**: `/resetgame` bila ingin ulang.

## 12. Catatan Implementasi

- Jawaban bot selalu benar dan mengikuti delay berdasarkan level, disesuaikan dengan timer soal.
- Lava naik setelah tiap soal, dipengaruhi `getLavaRiseRange()`.
- Poin hanya berkurang saat mati (bukan bertambah saat benar).
- `PlayerMoveListener` membekukan player (hanya boleh gerak kamera).
