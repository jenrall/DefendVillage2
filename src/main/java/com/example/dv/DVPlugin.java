package com.example.dv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class DVPlugin extends JavaPlugin {

    private final Map<String, Arena> arenas = new HashMap<>();

    private Arena currentArena;
    private boolean running = false;
    private int currentWave = 0;
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> activeZombies = new HashSet<>();
    private final Set<UUID> activeVillagers = new HashSet<>();
    private BukkitTask waveTask;
    private BossBar bossBar;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bossBar = Bukkit.createBossBar("Defend Village", BarColor.RED, BarStyle.SOLID);
        getCommand("dv").setExecutor(new DVCommand(this));
        getServer().getPluginManager().registerEvents(new DVListener(this), this);
        getLogger().info("DefendVillage enabled!");
    }

    @Override
    public void onDisable() {
        stopGame();
        getLogger().info("DefendVillage disabled!");
    }

    public Map<String, Arena> getArenas() { return arenas; }
    public Arena getArena(String name) { return arenas.get(name.toLowerCase()); }

    public boolean createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) return false;
        arenas.put(name.toLowerCase(), new Arena(name));
        return true;
    }

    public boolean deleteArena(String name) {
        if (!arenas.containsKey(name.toLowerCase())) return false;
        arenas.remove(name.toLowerCase());
        return true;
    }

    public boolean isRunning() { return running; }
    public Arena getCurrentArena() { return currentArena; }
    public Set<UUID> getParticipants() { return participants; }

    public boolean startGame(String name) {
        Arena a = getArena(name);
        if (a == null) return false;
        if (running) return false;
        if (a.getPos1() == null || a.getPos2() == null) return false;
        if (a.getVillagerSpots().isEmpty()) return false;
        if (a.getZombieSpawns().isEmpty()) return false;

        currentArena = a;
        running = true;
        currentWave = 0;
        participants.clear();
        activeZombies.clear();
        activeVillagers.clear();

        for (Location loc : a.getVillagerSpots()) {
            Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
            v.setAI(false);
            v.setSilent(true);
            activeVillagers.add(v.getUniqueId());
        }

        Bukkit.broadcast(Component.text("Defend Village started! /dv join", NamedTextColor.GOLD));

        int interval = getConfig().getInt("wave-interval-seconds", 30) * 20;
        waveTask = Bukkit.getScheduler().runTaskTimer(this, this::nextWave, 20L, interval);
        return true;
    }

    private void nextWave() {
        if (!running || currentArena == null) return;
        currentWave++;
        int maxWaves = getConfig().getInt("waves", 10);

        if (currentWave > maxWaves) {
            win();
            return;
        }

        int first = getConfig().getInt("zombies-first-wave", 3);
        int perWave = getConfig().getInt("zombies-per-wave", 2);
        int count = first + (currentWave - 1) * perWave;

        List<Location> spawns = currentArena.getZombieSpawns();
        for (int i = 0; i < count; i++) {
            Location spawn = spawns.get(i % spawns.size());
            Zombie z = (Zombie) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
            z.setShouldBurnInDay(false);
            activeZombies.add(z.getUniqueId());
        }

        Bukkit.broadcast(Component.text("Wave " + currentWave + "/" + maxWaves + " - " + count + " zombies!", NamedTextColor.RED));
        updateBossBar();
    }

    public void onZombieDeath(UUID id) {
        activeZombies.remove(id);
        updateBossBar();
    }

    public void onVillagerDeath(UUID id) {
        activeVillagers.remove(id);
        if (activeVillagers.isEmpty() && running) {
            lose();
        } else {
            updateBossBar();
        }
    }

    public void joinGame(Player p) {
        if (!running || currentArena == null) {
            p.sendMessage(Component.text("No game running.", NamedTextColor.RED));
            return;
        }
        if (participants.contains(p.getUniqueId())) {
            p.sendMessage(Component.text("Already joined.", NamedTextColor.YELLOW));
            return;
        }
        participants.add(p.getUniqueId());
        bossBar.addPlayer(p);
        if (currentArena.getCenter() != null) p.teleport(currentArena.getCenter());
        p.sendMessage(Component.text("Joined! Defend the villagers!", NamedTextColor.GREEN));
    }

    public void leaveGame(Player p) {
        participants.remove(p.getUniqueId());
        bossBar.removePlayer(p);
    }

    private void updateBossBar() {
        if (!running) return;
        int maxWaves = getConfig().getInt("waves", 10);
        bossBar.setTitle("Wave " + currentWave + "/" + maxWaves +
                " | Zombies: " + activeZombies.size() +
                " | Villagers: " + activeVillagers.size());
        bossBar.setProgress(maxWaves > 0 ? Math.min(1.0, (double) currentWave / maxWaves) : 0);
    }

    private void win() {
        Bukkit.broadcast(Component.text("You won! All waves survived!", NamedTextColor.GOLD));
        Material mat = Material.matchMaterial(getConfig().getString("reward-material", "DIAMOND"));
        if (mat == null) mat = Material.DIAMOND;
        int amt = getConfig().getInt("reward-amount", 10);
        int xp = getConfig().getInt("reward-xp", 200);

        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.getInventory().addItem(new ItemStack(mat, amt));
                p.giveExp(xp);
            }
        }
        stopGame();
    }

    private void lose() {
        Bukkit.broadcast(Component.text("All villagers died! You lost!", NamedTextColor.DARK_RED));
        stopGame();
    }

    public void stopGame() {
        running = false;
        if (waveTask != null) { waveTask.cancel(); waveTask = null; }

        for (UUID id : new HashSet<>(activeZombies)) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        for (UUID id : new HashSet<>(activeVillagers)) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        activeZombies.clear();
        activeVillagers.clear();

        for (UUID id : new HashSet<>(participants)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) bossBar.removePlayer(p);
        }
        participants.clear();
        bossBar.removeAll();
        bossBar.setProgress(0);
        bossBar.setTitle("Defend Village");
        currentArena = null;
        currentWave = 0;
    }

    public static class Arena {
        private final String name;
        private Location pos1;
        private Location pos2;
        private Location center;
        private final List<Location> villagerSpots = new ArrayList<>();
        private final List<Location> zombieSpawns = new ArrayList<>();

        public Arena(String name) { this.name = name; }

        public String getName() { return name; }
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
        public Location getCenter() { return center; }
        public List<Location> getVillagerSpots() { return villagerSpots; }
        public List<Location> getZombieSpawns() { return zombieSpawns; }

        public void setPos1(Location l) { this.pos1 = l; recalcCenter(); }
        public void setPos2(Location l) { this.pos2 = l; recalcCenter(); }
        public void setCenter(Location l) { this.center = l; }

        public void addVillagerSpot(Location l) { villagerSpots.add(l); }
        public void addZombieSpawn(Location l) { zombieSpawns.add(l); }

        private void recalcCenter() {
            if (pos1 == null || pos2 == null) return;
            World w = pos1.getWorld();
            if (w == null) return;
            double x = (pos1.getX() + pos2.getX()) / 2;
            double y = (pos1.getY() + pos2.getY()) / 2;
            double z = (pos1.getZ() + pos2.getZ()) / 2;
            this.center = new Location(w, x, y + 1, z);
        }

        public boolean contains(Location loc) {
            if (loc == null || pos1 == null || pos2 == null) return false;
            if (loc.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) return false;
            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());
            return loc.getX() >= minX && loc.getX() <= maxX
                    && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }
    }
}
