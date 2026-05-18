package yt.corazonid.cerdasCermatTowerRace.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class GamePlayer {
    private final String playerName;
    private final Player player;
    private final int slot;
    private final boolean isBot;

    private int platformY;
    private int baseX;
    private int baseZ;
    private World world;
    private int point;
    private boolean alive;
    private boolean frozen;

    // Villager yang mewakili bot di dunia (berdiri di pijakan)
    private Villager villagerEntity;
    // Ranking akhir saat mati (1 = juara 1 / pemenang, dst dari belakang)
    private int finalRank = 0;

    /** Constructor untuk PLAYER ASLI */
    public GamePlayer(String playerName, Player player, int slot) {
        this.playerName = playerName;
        this.player     = player;
        this.slot       = slot;
        this.isBot      = false;
        this.world      = player != null ? player.getWorld() : null;
        this.platformY  = 0;
        this.baseX      = 0;
        this.baseZ      = 0;
        this.point      = 0;
        this.alive      = true;
        this.frozen     = false;
    }

    /** Constructor untuk BOT (villager simulasi) */
    public GamePlayer(String botName, int slot, World world) {
        this.playerName = botName;
        this.player     = null;
        this.slot       = slot;
        this.isBot      = true;
        this.world      = world;
        this.platformY  = 0;
        this.baseX      = 0;
        this.baseZ      = 0;
        this.point      = 0;
        this.alive      = true;
        this.frozen     = false;
    }

    public String getPlayerName()   { return playerName; }
    public Player getPlayer()       { return player; }
    public int getSlot()            { return slot; }
    public boolean isBot()          { return isBot; }
    public int getPlatformY()       { return platformY; }
    public int getBaseX()           { return baseX; }
    public int getBaseZ()           { return baseZ; }
    public int getPoint()           { return point; }
    public boolean isAlive()        { return alive; }
    public boolean isFrozen()       { return frozen; }
    public World getWorld()         { return world; }
    public Villager getVillager()   { return villagerEntity; }
    public int getFinalRank()       { return finalRank; }

    public void setPlatformY(int y)         { this.platformY = y; }
    public void setBaseX(int x)             { this.baseX = x; }
    public void setBaseZ(int z)             { this.baseZ = z; }
    public void setPoint(int point)         { this.point = point; }
    public void setAlive(boolean b)         { this.alive = b; }
    public void setFrozen(boolean b)        { this.frozen = b; }
    public void setWorld(World w)           { this.world = w; }
    public void setVillager(Villager v)     { this.villagerEntity = v; }
    public void setFinalRank(int r)         { this.finalRank = r; }

    public void raisePlatform(int blocks)   { this.platformY += blocks; }
    public void lowerPlatform(int blocks)   { this.platformY = Math.max(0, this.platformY - blocks); }
    public void addPoint(int amount)        { this.point += amount; }
    public void deductPoint(int amount)     { this.point -= amount; }

    public World getEffectiveWorld() {
        if (player != null) return player.getWorld();
        return world;
    }

    public Location getStandLocation() {
        World w = getEffectiveWorld();
        if (w == null) return null;
        return new Location(w, baseX + 0.5, platformY + 1, baseZ + 0.5);
    }

    public Location getPlatformLocation() {
        World w = getEffectiveWorld();
        if (w == null) return null;
        return new Location(w, baseX, platformY, baseZ);
    }

    @Override
    public String toString() {
        return String.format("%s%s (Slot %d | Y=%d | Poin=%d | %s)",
                playerName, isBot ? " [BOT]" : "",
                slot, platformY, point, alive ? "Hidup" : "Mati");
    }
}
