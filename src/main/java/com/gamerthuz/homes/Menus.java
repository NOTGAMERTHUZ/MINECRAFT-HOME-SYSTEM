package com.gamerthuz.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Every GUI screen of the plugin. */
public final class Menus {
    private static final String TITLE = "<gradient:#00f5ff:#7b2ff7><bold>HOMES</bold></gradient> <dark_gray>» <white>";
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private Menus() {}

    // ---------- shared pieces ----------

    private static String worldLabel(Home h) {
        World w = Bukkit.getWorld(h.world);
        if (w == null) return "<red>" + h.world + " (unloaded)";
        return switch (w.getEnvironment()) {
            case NORMAL -> "<green>" + w.getName();
            case NETHER -> "<red>" + w.getName();
            case THE_END -> "<light_purple>" + w.getName();
            default -> "<white>" + w.getName();
        };
    }

    /** Arrow pointing to the target relative to where the viewer is looking. */
    private static String arrow(Player p, Location target) {
        double dx = target.getX() - p.getLocation().getX();
        double dz = target.getZ() - p.getLocation().getZ();
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = (bearing - p.getLocation().getYaw()) % 360;
        if (rel < 0) rel += 360;
        return ARROWS[(int) Math.round(rel / 45.0) % 8];
    }

    private static ItemStack homeItem(HomesPlugin plugin, Player viewer, Home h, boolean showOwner, List<String> extra) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (showOwner) lore.add("<#00f5ff>◆ <gray>Owner: <white>" + plugin.store().ownerName(h.owner));
        lore.add("<#00f5ff>◆ <gray>World: " + worldLabel(h));
        lore.add("<#00f5ff>◆ <gray>Coords: <white>" + (int) h.x + ", " + (int) h.y + ", " + (int) h.z);
        Location loc = h.location();
        if (loc == null) {
            lore.add("<#00f5ff>◆ <gray>Distance: <dark_gray>unknown");
        } else if (loc.getWorld().equals(viewer.getWorld())) {
            int dist = (int) viewer.getLocation().distance(loc);
            lore.add("<#00f5ff>◆ <gray>Distance: <white>" + dist + " blocks <#00f5ff>" + arrow(viewer, loc));
        } else {
            lore.add("<#00f5ff>◆ <gray>Distance: <dark_gray>other dimension");
        }
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(h.created), ZoneId.systemDefault());
        lore.add("<#00f5ff>◆ <gray>Visits: <white>" + h.visits + " <dark_gray>• <gray>Created <white>" + date);
        lore.add("<#00f5ff>◆ <gray>Visibility: " + (h.publicHome ? "<green>Public" : "<gray>Private"));
        if (!extra.isEmpty()) {
            lore.add("");
            lore.addAll(extra);
        }
        String name = (h.favorite ? "<gold>★ " : "") + "<#00f5ff><bold>" + h.name;
        return Gui.item(h.icon, name, lore, h.favorite);
    }

    private static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase().split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // ===================== MAIN: MY HOMES =====================

    public static void main(HomesPlugin plugin, Player p, int page) {
        HomeManager hm = plugin.homes();
        if (!hm.requireActive(p)) return;
        List<Home> list = hm.homesIn(p);
        List<Integer> slots = Gui.innerSlots(6);
        int per = slots.size();
        int pages = Math.max(1, (list.size() + per - 1) / per);
        final int pg = Math.max(0, Math.min(page, pages - 1));
        int used = list.size();

        MenuHolder h = Gui.create(TITLE + "My Homes <gray>(" + used + "/" + hm.limitText(p) + ")", 6);
        Gui.frame(plugin, h, 6);

        // profile
        Home fav = hm.favorite(p);
        int totalVisits = 0;
        for (Home x : list) totalVisits += x.visits;
        List<String> info = new ArrayList<>();
        info.add("");
        info.add("<#00f5ff>◆ <gray>Zone: <white>" + hm.groupName(p));
        info.add("<#00f5ff>◆ <gray>Homes: <white>" + used + "<gray>/<white>" + hm.limitText(p));
        info.add("<#00f5ff>◆ <gray>Favorite: " + (fav != null ? "<gold>★ <white>" + fav.name : "<dark_gray>none"));
        info.add("<#00f5ff>◆ <gray>Total visits: <white>" + totalVisits);
        if (plugin.economy().available()) {
            info.add("<#00f5ff>◆ <gray>Balance: <green>" + plugin.economy().format(plugin.economy().balance(p)));
        }
        long cd = hm.cooldownLeft(p);
        info.add("<#00f5ff>◆ <gray>Cooldown: " + (cd > 0 ? "<red>" + cd + "s" : "<green>ready"));
        h.set(4, Gui.head(p, "<gradient:#00f5ff:#7b2ff7><bold>" + p.getName(), info), null);

        if (list.isEmpty()) {
            h.set(22, Gui.item(Material.BARRIER, "<red><bold>No homes yet",
                    List.of("<gray>Click <green>+ New Home <gray>below", "<gray>or use <white>/sethome <name>"), false), null);
        }
        for (int i = 0; i < per; i++) {
            int idx = pg * per + i;
            if (idx >= list.size()) break;
            Home home = list.get(idx);
            List<String> extra = List.of(
                    "<yellow>Left-click <gray>» <white>teleport",
                    "<yellow>Right-click <gray>» <white>manage",
                    "<yellow>Shift-click <gray>» <white>toggle favorite");
            h.set(slots.get(i), homeItem(plugin, p, home, false, extra), e -> {
                if (e.isShiftClick()) {
                    hm.toggleFavorite(p, home);
                    plugin.sync(() -> main(plugin, p, pg));
                } else if (e.isRightClick()) {
                    plugin.sync(() -> manage(plugin, p, home));
                } else {
                    p.closeInventory();
                    hm.teleport(p, home);
                }
            });
        }

        // bottom controls
        if (used < hm.limit(p)) {
            h.set(46, Gui.item(Material.EMERALD, "<green><bold>+ New Home",
                    List.of("", "<gray>Create a home at your", "<gray>current location.", "", "<yellow>Click, then type a name"), false),
                    e -> {
                        p.closeInventory();
                        plugin.prompts().ask(p, "prompt-new", text -> {
                            hm.set(p, text);
                            main(plugin, p, 0);
                        });
                    });
        } else {
            h.set(46, Gui.item(Material.BARRIER, "<red><bold>Home limit reached",
                    List.of("", "<gray>Delete a home or get a", "<gray>higher rank for more slots."), false), null);
        }
        h.set(47, Gui.item(Material.BEACON, "<#00f5ff><bold>Public Homes",
                List.of("", "<gray>Visit homes other players", "<gray>made public.", "", "<yellow>Click to browse"), false),
                e -> plugin.sync(() -> publicHomes(plugin, p, 0)));
        if (pg > 0) {
            h.set(48, Gui.item(Material.ARROW, "<yellow><bold>« Previous page", List.of(), false),
                    e -> plugin.sync(() -> main(plugin, p, pg - 1)));
        }
        if (pg < pages - 1) {
            h.set(50, Gui.item(Material.ARROW, "<yellow><bold>Next page »", List.of(), false),
                    e -> plugin.sync(() -> main(plugin, p, pg + 1)));
        }
        h.set(52, Gui.item(Material.OAK_DOOR, "<red><bold>Close", List.of(), false), e -> p.closeInventory());

        p.openInventory(h.getInventory());
        Gui.sound(p, "block.beacon.power_select", 0.5f, 1.8f);
    }

    // ===================== MANAGE ONE HOME =====================

    public static void manage(HomesPlugin plugin, Player p, Home home) {
        HomeManager hm = plugin.homes();
        MenuHolder h = Gui.create(TITLE + "Manage <#00f5ff>" + home.name, 5);
        Gui.frame(plugin, h, 5);

        h.set(4, homeItem(plugin, p, home, false, List.of()), null);

        List<Integer> s = Gui.centered(7, 22);

        h.set(s.get(0), Gui.item(Material.ENDER_PEARL, "<#00f5ff><bold>Teleport",
                List.of("", "<gray>Go to this home.", "", "<yellow>Click"), false), e -> {
                    p.closeInventory();
                    hm.teleport(p, home);
                });
        h.set(s.get(1), Gui.item(Material.NAME_TAG, "<white><bold>Rename",
                List.of("", "<gray>Current: <white>" + home.name, "", "<yellow>Click, then type the new name"), false), e -> {
                    p.closeInventory();
                    plugin.prompts().ask(p, "prompt-rename", text -> {
                        hm.rename(p, home, text);
                        manage(plugin, p, home);
                    });
                });
        h.set(s.get(2), Gui.item(Material.ITEM_FRAME, "<white><bold>Change icon",
                List.of("", "<gray>Current: <white>" + pretty(home.icon), "", "<yellow>Click to choose"), false),
                e -> plugin.sync(() -> icons(plugin, p, home)));
        h.set(s.get(3), Gui.item(Material.COMPASS, "<white><bold>Move here",
                List.of("", "<gray>Set this home to your", "<gray>current location.", "", "<yellow>Click"), false), e -> {
                    hm.relocate(p, home);
                    plugin.sync(() -> manage(plugin, p, home));
                });
        h.set(s.get(4), Gui.item(Material.NETHER_STAR,
                "<gold><bold>Favorite: " + (home.favorite ? "<green>ON" : "<red>OFF"),
                List.of("", "<gray>Your favorite is used by", "<gray>plain <white>/home<gray>.", "", "<yellow>Click to toggle"), home.favorite),
                e -> {
                    hm.toggleFavorite(p, home);
                    plugin.sync(() -> manage(plugin, p, home));
                });
        h.set(s.get(5), Gui.item(home.publicHome ? Material.SEA_LANTERN : Material.REDSTONE_LAMP,
                "<white><bold>Public: " + (home.publicHome ? "<green>ON" : "<red>OFF"),
                List.of("", "<gray>Public homes appear in the", "<gray>browser and work with", "<white>/home <you>:" + home.name, "", "<yellow>Click to toggle"),
                home.publicHome), e -> {
                    hm.togglePublic(p, home);
                    plugin.sync(() -> manage(plugin, p, home));
                });
        h.set(s.get(6), Gui.item(Material.TNT, "<red><bold>Delete",
                List.of("", "<gray>Remove this home forever.", "", "<yellow>Click to confirm"), false),
                e -> plugin.sync(() -> confirmDelete(plugin, p, home)));

        h.set(39, Gui.item(Material.ARROW, "<yellow><bold>« Back", List.of(), false),
                e -> plugin.sync(() -> main(plugin, p, 0)));
        h.set(41, Gui.item(Material.OAK_DOOR, "<red><bold>Close", List.of(), false), e -> p.closeInventory());

        p.openInventory(h.getInventory());
        Gui.sound(p, "ui.button.click", 0.7f, 1.2f);
    }

    // ===================== ICON PICKER =====================

    public static void icons(HomesPlugin plugin, Player p, Home home) {
        MenuHolder h = Gui.create(TITLE + "Choose Icon", 6);
        Gui.frame(plugin, h, 6);

        List<Integer> slots = Gui.innerSlots(6);
        List<String> names = plugin.getConfig().getStringList("icons");
        int placed = 0;
        for (String name : names) {
            if (placed >= slots.size()) break;
            Material m = Material.matchMaterial(name);
            if (m == null || !m.isItem()) continue;
            boolean current = m == home.icon;
            h.set(slots.get(placed++), Gui.item(m, "<white><bold>" + pretty(m),
                    List.of("", current ? "<green>✔ Selected" : "<yellow>Click to select"), current), e -> {
                        plugin.homes().setIcon(p, home, m);
                        plugin.sync(() -> manage(plugin, p, home));
                    });
        }
        h.set(48, Gui.item(Material.ARROW, "<yellow><bold>« Back", List.of(), false),
                e -> plugin.sync(() -> manage(plugin, p, home)));

        p.openInventory(h.getInventory());
    }

    // ===================== DELETE CONFIRM =====================

    public static void confirmDelete(HomesPlugin plugin, Player p, Home home) {
        MenuHolder h = Gui.create(TITLE + "Delete <red>" + home.name + "<white>?", 3);
        Gui.frame(plugin, h, 3);

        h.set(13, homeItem(plugin, p, home, false, List.of()), null);
        h.set(11, Gui.item(Material.LIME_CONCRETE, "<green><bold>CONFIRM DELETE",
                List.of("", "<gray>This cannot be undone.", "", "<yellow>Click"), false), e -> {
                    plugin.homes().delete(p, home);
                    plugin.sync(() -> main(plugin, p, 0));
                });
        h.set(15, Gui.item(Material.RED_CONCRETE, "<red><bold>CANCEL",
                List.of("", "<yellow>Click"), false), e -> plugin.sync(() -> manage(plugin, p, home)));

        p.openInventory(h.getInventory());
    }

    // ===================== PUBLIC HOMES =====================

    public static void publicHomes(HomesPlugin plugin, Player p, int page) {
        if (!plugin.homes().requireActive(p)) return;
        List<Home> list = plugin.homes().publicIn(p);
        List<Integer> slots = Gui.innerSlots(6);
        int per = slots.size();
        int pages = Math.max(1, (list.size() + per - 1) / per);
        final int pg = Math.max(0, Math.min(page, pages - 1));

        MenuHolder h = Gui.create(TITLE + "Public Homes <gray>(" + (pg + 1) + "/" + pages + ")", 6);
        Gui.frame(plugin, h, 6);

        if (list.isEmpty()) {
            h.set(22, Gui.item(Material.BARRIER, "<red><bold>No public homes yet",
                    List.of("<gray>Make one of your homes public", "<gray>from its manage menu."), false), null);
        }
        for (int i = 0; i < per; i++) {
            int idx = pg * per + i;
            if (idx >= list.size()) break;
            Home home = list.get(idx);
            boolean yours = home.owner.equals(p.getUniqueId());
            h.set(slots.get(i), homeItem(plugin, p, home, true,
                    List.of(yours ? "<gray>(this is your home)" : "<yellow>Click <gray>» <white>teleport")), e -> {
                        p.closeInventory();
                        plugin.homes().teleport(p, home);
                    });
        }

        h.set(46, Gui.item(Material.ARROW, "<yellow><bold>« My homes", List.of(), false),
                e -> plugin.sync(() -> main(plugin, p, 0)));
        if (pg > 0) {
            h.set(48, Gui.item(Material.ARROW, "<yellow><bold>« Previous page", List.of(), false),
                    e -> plugin.sync(() -> publicHomes(plugin, p, pg - 1)));
        }
        if (pg < pages - 1) {
            h.set(50, Gui.item(Material.ARROW, "<yellow><bold>Next page »", List.of(), false),
                    e -> plugin.sync(() -> publicHomes(plugin, p, pg + 1)));
        }
        h.set(52, Gui.item(Material.OAK_DOOR, "<red><bold>Close", List.of(), false), e -> p.closeInventory());

        p.openInventory(h.getInventory());
        Gui.sound(p, "block.beacon.power_select", 0.5f, 1.8f);
    }
}
