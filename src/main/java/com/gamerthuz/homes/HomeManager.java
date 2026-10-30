package com.gamerthuz.homes;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.gamerthuz.homes.Messages.ph;

/** All home actions. Everything is scoped to the player's current world group. */
public final class HomeManager {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    private final HomesPlugin plugin;
    private final Map<UUID, BukkitTask> warmups = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public HomeManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- world groups ----------

    public WorldGroups.Group active(Player p) {
        return plugin.groups().active(p.getWorld());
    }

    /** Tells the player and returns false when homes are disabled in their current world. */
    public boolean requireActive(Player p) {
        if (active(p) == null) {
            plugin.messages().send(p, "world-disabled");
            return false;
        }
        return true;
    }

    public String groupName(Player p) {
        WorldGroups.Group g = plugin.groups().of(p.getWorld());
        return g == null ? "-" : g.display();
    }

    /** Homes of the player in the group they are standing in. */
    public List<Home> homesIn(Player p) {
        WorldGroups.Group g = active(p);
        return g == null ? new ArrayList<>() : plugin.store().list(p.getUniqueId(), g.id());
    }

    public List<Home> publicIn(Player p) {
        WorldGroups.Group g = active(p);
        return g == null ? new ArrayList<>() : plugin.store().publicHomes(g.id());
    }

    public Home find(Player p, String name) {
        WorldGroups.Group g = active(p);
        return g == null ? null : plugin.store().get(p.getUniqueId(), g.id(), name);
    }

    public Home favorite(Player p) {
        for (Home h : homesIn(p)) {
            if (h.favorite) return h;
        }
        return null;
    }

    // ---------- helpers ----------

    public int limit(Player p) {
        if (p.hasPermission("gamerthuz.homes.unlimited")) return Integer.MAX_VALUE;
        int best = Math.max(0, plugin.getConfig().getInt("settings.default-limit", 3));
        for (int n = 1; n <= 100; n++) {
            if (n > best && p.hasPermission("gamerthuz.homes.limit." + n)) best = n;
        }
        return best;
    }

    public String limitText(Player p) {
        int l = limit(p);
        return l == Integer.MAX_VALUE ? "∞" : String.valueOf(l);
    }

    public static boolean validName(String s) {
        return s != null && NAME.matcher(s).matches();
    }

    public long cooldownLeft(Player p) {
        if (p.hasPermission("gamerthuz.homes.bypass.cooldown")) return 0;
        long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    private double cost(String key, Player p) {
        double c = plugin.getConfig().getDouble("cost." + key, 0);
        if (c <= 0 || p.hasPermission("gamerthuz.homes.bypass.cost") || !plugin.economy().available()) return 0;
        return c;
    }

    private static Material defaultIcon(World w) {
        return switch (w.getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.RED_BED;
        };
    }

    // ---------- create / change ----------

    public boolean set(Player p, String name) {
        Messages m = plugin.messages();
        UUID id = p.getUniqueId();
        HomeStore store = plugin.store();

        WorldGroups.Group g = active(p);
        if (g == null) {
            m.send(p, "world-disabled");
            return false;
        }
        if (!validName(name)) {
            m.send(p, "invalid-name");
            return false;
        }
        Home existing = store.get(id, g.id(), name);
        if (existing != null) {
            existing.setLocation(p.getLocation());
            store.save();
            m.send(p, "home-moved", ph("home", existing.name));
            Gui.sound(p, "block.respawn_anchor.set_spawn", 0.8f, 1.4f);
            return true;
        }
        int used = store.count(id, g.id());
        if (used >= limit(p)) {
            m.send(p, "limit-reached", ph("limit", limitText(p)));
            return false;
        }
        double cost = cost("set", p);
        if (cost > 0) {
            if (!plugin.economy().withdraw(p, cost)) {
                m.send(p, "not-enough-money", ph("cost", plugin.economy().format(cost)));
                return false;
            }
            m.send(p, "cost-charged", ph("cost", plugin.economy().format(cost)));
        }
        Home h = new Home(id, name);
        h.group = g.id();
        h.setLocation(p.getLocation());
        h.icon = defaultIcon(p.getWorld());
        h.favorite = used == 0;
        store.add(h);
        store.touchName(p);
        store.save();
        m.send(p, "home-set", ph("home", h.name));
        Gui.sound(p, "block.respawn_anchor.set_spawn", 0.8f, 1.4f);
        return true;
    }

    public void relocate(Player p, Home h) {
        WorldGroups.Group g = active(p);
        if (g == null) {
            plugin.messages().send(p, "world-disabled");
            return;
        }
        if (!g.id().equals(h.group)) {
            plugin.messages().send(p, "cross-world-blocked");
            return;
        }
        h.setLocation(p.getLocation());
        plugin.store().save();
        plugin.messages().send(p, "home-moved", ph("home", h.name));
        Gui.sound(p, "block.respawn_anchor.set_spawn", 0.8f, 1.4f);
    }

    public void delete(Player p, Home h) {
        plugin.store().remove(h);
        plugin.store().save();
        plugin.messages().send(p, "home-deleted", ph("home", h.name));
        Gui.sound(p, "block.anvil.destroy", 0.5f, 1.5f);
    }

    public boolean rename(Player p, Home h, String newName) {
        Messages m = plugin.messages();
        if (!validName(newName)) {
            m.send(p, "invalid-name");
            return false;
        }
        Home other = plugin.store().get(h.owner, h.group, newName);
        if (other != null && other != h) {
            m.send(p, "name-taken", ph("home", other.name));
            return false;
        }
        String old = h.name;
        plugin.store().rename(h, newName);
        plugin.store().save();
        m.send(p, "home-renamed", ph("old", old), ph("new", newName));
        return true;
    }

    public void toggleFavorite(Player p, Home h) {
        boolean on = !h.favorite;
        if (on) {
            for (Home other : plugin.store().list(h.owner, h.group)) other.favorite = false;
        }
        h.favorite = on;
        plugin.store().save();
        plugin.messages().send(p, on ? "favorite-on" : "favorite-off", ph("home", h.name));
        Gui.sound(p, "entity.experience_orb.pickup", 0.8f, on ? 1.6f : 1.0f);
    }

    public void togglePublic(Player p, Home h) {
        h.publicHome = !h.publicHome;
        plugin.store().save();
        plugin.messages().send(p, h.publicHome ? "public-on" : "public-off", ph("home", h.name));
        Gui.sound(p, "block.beacon.activate", 0.6f, h.publicHome ? 1.6f : 1.0f);
    }

    public void setIcon(Player p, Home h, Material icon) {
        h.icon = icon;
        plugin.store().save();
        plugin.messages().send(p, "icon-changed");
        Gui.sound(p, "ui.button.click", 0.8f, 1.4f);
    }

    // ---------- teleport ----------

    public void teleport(Player p, Home h) {
        Messages m = plugin.messages();
        UUID id = p.getUniqueId();
        Location loc = h.location();
        if (loc == null) {
            m.send(p, "world-missing");
            return;
        }
        WorldGroups.Group here = active(p);
        if (here == null) {
            m.send(p, "world-disabled");
            return;
        }
        WorldGroups.Group there = plugin.groups().of(h.world);
        if (there == null || !there.id().equals(here.id())) {
            m.send(p, "cross-world-blocked");
            return;
        }
        boolean own = h.owner.equals(id);
        if (!own && !h.publicHome && !p.hasPermission("gamerthuz.homes.admin")) {
            m.send(p, "not-public");
            return;
        }
        if (warmups.containsKey(id)) {
            m.send(p, "busy");
            return;
        }
        long left = cooldownLeft(p);
        if (left > 0) {
            m.send(p, "cooldown", ph("seconds", String.valueOf(left)));
            return;
        }
        double cost = cost("teleport", p);
        if (cost > 0 && !plugin.economy().has(p, cost)) {
            m.send(p, "not-enough-money", ph("cost", plugin.economy().format(cost)));
            return;
        }
        int warmup = p.hasPermission("gamerthuz.homes.bypass.warmup")
                ? 0 : Math.max(0, plugin.getConfig().getInt("settings.warmup", 3));
        if (warmup <= 0) {
            go(p, h, loc, cost);
            return;
        }
        m.send(p, "warmup-start", ph("seconds", String.valueOf(warmup)));

        BukkitTask task = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    warmups.remove(id);
                    cancel();
                    return;
                }
                if (t >= warmup * 10) {
                    warmups.remove(id);
                    cancel();
                    go(p, h, loc, cost);
                    return;
                }
                if (t % 10 == 0) {
                    p.showTitle(Title.title(
                            MM.deserialize("<gradient:#00f5ff:#7b2ff7><bold>" + (warmup - t / 10) + "</bold></gradient>"),
                            MM.deserialize("<gray>Locking coordinates... <white>" + h.name),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(100))));
                    Gui.sound(p, "block.note_block.chime", 0.8f, 0.8f + (t / 10) * 0.2f);
                }
                // double helix around the player
                Location pl = p.getLocation();
                double a = t * 0.6;
                double y = (t % 20) * 0.1;
                for (int k = 0; k < 2; k++) {
                    double ang = a + k * Math.PI;
                    pl.getWorld().spawnParticle(Particle.END_ROD,
                            pl.getX() + Math.cos(ang) * 0.9, pl.getY() + y, pl.getZ() + Math.sin(ang) * 0.9,
                            1, 0, 0, 0, 0);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        warmups.put(id, task);
    }

    private void go(Player p, Home h, Location loc, double cost) {
        Messages m = plugin.messages();
        // the player may have changed world during the warmup
        if (!plugin.groups().allowed(p.getWorld()) || !plugin.groups().sameGroup(p.getWorld(), loc.getWorld())) {
            m.send(p, "cross-world-blocked");
            return;
        }
        if (cost > 0) {
            if (!plugin.economy().withdraw(p, cost)) {
                m.send(p, "not-enough-money", ph("cost", plugin.economy().format(cost)));
                return;
            }
            m.send(p, "cost-charged", ph("cost", plugin.economy().format(cost)));
        }
        Location from = p.getLocation().clone();
        p.teleportAsync(loc).thenAccept(ok -> plugin.sync(() -> {
            if (!Boolean.TRUE.equals(ok)) {
                m.send(p, "teleport-failed");
                return;
            }
            long cd = Math.max(0, plugin.getConfig().getLong("settings.cooldown", 5));
            cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000);
            h.visits++;
            plugin.store().save();

            burst(from);
            burst(p.getLocation());
            Gui.sound(p, "entity.enderman.teleport", 1f, 1.2f);
            p.showTitle(Title.title(
                    MM.deserialize("<gradient:#00f5ff:#7b2ff7><bold>ARRIVED</bold></gradient>"),
                    MM.deserialize("<gray>" + h.name),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(500))));
            m.send(p, "teleported", ph("home", h.name));
        }));
    }

    private void burst(Location l) {
        World w = l.getWorld();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12;
            w.spawnParticle(Particle.END_ROD, l.getX() + Math.cos(a), l.getY() + 0.1, l.getZ() + Math.sin(a),
                    1, 0, 0.6, 0, 0.02);
        }
        w.spawnParticle(Particle.REVERSE_PORTAL, l.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.2);
    }

    public void cancelWarmup(Player p, boolean notify) {
        BukkitTask t = warmups.remove(p.getUniqueId());
        if (t != null) {
            t.cancel();
            if (notify) {
                plugin.messages().send(p, "warmup-cancelled");
                p.clearTitle();
            }
        }
    }

    public boolean isWarming(UUID id) {
        return warmups.containsKey(id);
    }

    public void cleanup(UUID id) {
        BukkitTask t = warmups.remove(id);
        if (t != null) t.cancel();
    }
}
