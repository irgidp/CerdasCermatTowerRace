package yt.corazonid.cerdasCermatTowerRace.model;

/**
 * Model untuk satu soal matematika.
 *
 * KONSEP:
 * - Soal disimpan di questions.yml dengan format { soal, jawaban }
 * - Jawaban di-trim dan di-lowercase saat pengecekan, jadi spasi ekstra tidak masalah
 */
public class Question {
    private final String id;       // ID unik soal (misal: "q1", "q5")
    private final String soal;     // Teks pertanyaan yang ditampilkan ke player
    private final String jawaban;  // Jawaban yang benar

    public Question(String id, String soal, String jawaban) {
        this.id = id;
        this.soal = soal;
        this.jawaban = jawaban.trim().toLowerCase();  // Normalize saat load
    }

    public String getId()    { return id; }
    public String getSoal()  { return soal; }
    public String getJawaban() { return jawaban; }

    /**
     * Cek apakah jawaban player benar.
     * Case-insensitive dan trim whitespace.
     *
     * Contoh:
     *   jawaban di config: "42"
     *   player ketik: "42 " → benar (setelah trim)
     *   player ketik: "42" → benar
     *   player ketik: "043" → SALAH (kita tidak handle leading zero)
     */
    public boolean isCorrect(String attempt) {
        if (attempt == null) return false;
        return attempt.trim().equalsIgnoreCase(this.jawaban);
    }
}
