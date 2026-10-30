package com.gamerthuz.homes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All homes, kept in memory and saved to homes.yml. Homes are scoped by world group. */
public final class HomeStore {
    private static final Object LOCK = new Object();

    private final HomesPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();
    private final Map<UUID, String> lastNames = new HashMap<>();

    public HomeStore(HomesPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    private static String key(String group, String name) {
        return group + "/" + name.toLowerCase();
    }

    private void load() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yml.getConfigurationSection("players");
        if (players == null) return;
        for (String idStr : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection ps = players.getConfigurationSection(idStr);
            if (ps == null) continue;
            lastNames.put(id, ps.getString("name", ""));
            ConfigurationSection hs = ps.getConfigurationSection("homes");
            if (hs == null) continue;
            Map<String, Home> map = new LinkedHashMap<>();
            for (String group : hs.getKeys(false)) {
                ConfigurationSection gs = hs.getConfigurationSection(group);
                if (gs == null) continue;
                for (String k : gs.getKeys(false)) {
                    ConfigurationSection s = gs.getConfigurationSection(k);
                    if (s == null) continue;
                    Home h = new Home(id, s.getString("name", k));
                    h.group = group;
                    h.world = s.getString("world", "world");
                    h.x = s.getDouble("x");
                    h.y = s.getDouble("y");
                    h.z = s.getDouble("z");
                    h.yaw = (float) s.getDouble("yaw");
                    h.pitch = (float) s.getDouble("pitch");
                    Material m = Material.matchMaterial(s.getString("icon", "RED_BED"));
                    h.icon = m != null ? m : Material.RED_BED;
                    h.favorite = s.getBoolean("favorite", false);
                    h.publicHome = s.getBoolean("public", false);
                    h.created = s.getLong("created", System.currentTimeMillis());
                    h.visits = s.getInt("visits", 0);
                    map.put(key(group, h.name), h);
                }
            }
            homes.put(id, map);
        }
    }

    // ---------- queries ----------

    public List<Home> list(UUID id, String group) {
        Map<String, Home> map = homes.get(id);
        List<Home> out = new ArrayList<>();
        if (map == null) return out;
        for (Home h : map.values()) {
            if (h.group.equals(group)) out.add(h);
        }
        out.sort(Comparator.comparing((Home h) -> !h.favorite)
                .thenComparing(h -> h.name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public Home get(UUID id, String group, String name) {
        Map<String, Home> map = homes.get(id);
        return map == null || name == null ? null : map.get(key(group, name));
    }

    public int count(UUID id, String group) {
        return list(id, group).size();
    }

    public List<Home> publicHomes(String group) {
        List<Home> out = new ArrayList<>();
        for (Map<String, Home> map : homes.values()) {
            for (Home h : map.values()) {
                if (h.publicHome && h.group.equals(group)) out.add(h);
            }
        }
        out.sort(Comparator.comparing((Home h) -> ownerName(h.owner), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(h -> h.name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public String ownerName(UUID id) {
        String n = plugin.getServer().getOfflinePlayer(id).getName();
        if (n == null) n = lastNames.get(id);
        return n == null || n.isEmpty() ? id.toString().substring(0, 8) : n;
    }

    // ---------- changes ----------

    public void touchName(Player p) {
        lastNames.put(p.getUniqueId(), p.getName());
    }

    public void add(Home h) {
        homes.computeIfAbsent(h.owner, k -> new LinkedHashMap<>()).put(key(h.group, h.name), h);
    }

    public void remove(Home h) {
        Map<String, Home> map = homes.get(h.owner);
        if (map != null) map.remove(key(h.group, h.name));
    }

    public void rename(Home h, String newName) {
        remove(h);
        h.name = newName;
        add(h);
    }

    // ---------- saving ----------

    private String serialize() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Home>> e : homes.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String base = "players." + e.getKey();
            yml.set(base + ".name", lastNames.getOrDefault(e.getKey(), ""));
            for (Home h : e.getValue().values()) {
                String p = base + ".homes." + h.group + "." + h.name.toLowerCase();
                yml.set(p + ".name", h.name);
                yml.set(p + ".world", h.world);
                yml.set(p + ".x", h.x);
                yml.set(p + ".y", h.y);
                yml.set(p + ".z", h.z);
                yml.set(p + ".yaw", h.yaw);
                yml.set(p + ".pitch", h.pitch);
                yml.set(p + ".icon", h.icon.name());
                yml.set(p + ".favorite", h.favorite);
                yml.set(p + ".public", h.publicHome);
                yml.set(p + ".created", h.created);
                yml.set(p + ".visits", h.visits);
            }
        }
        return yml.saveToString();
    }

    private void write(String text) {
        synchronized (LOCK) {
            try {
                java.nio.file.Files.writeString(file.toPath(), text);
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not save homes.yml: " + ex.getMessage());
            }
        }
    }

    /** Save without blocking the main thread. */
    public void save() {
        String text = serialize();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(text));
    }

    /** Save right now (used on shutdown). */
    public void saveNow() {
        write(serialize());
    }
}
