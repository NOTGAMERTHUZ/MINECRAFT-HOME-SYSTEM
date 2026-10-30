package com.gamerthuz.homes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Helpers for the futuristic neon GUI. */
public final class Gui {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Map<Material, ItemStack> PANES = new EnumMap<>(Material.class);

    private Gui() {}

    public static Component c(String s) {
        return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack item(Material m, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        if (glow) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack head(Player p, String name, List<String> lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(p);
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack paneStack(Material m) {
        return PANES.computeIfAbsent(m, k -> {
            ItemStack it = new ItemStack(k);
            ItemMeta meta = it.getItemMeta();
            meta.displayName(Component.text(" "));
            meta.setHideTooltip(true);
            it.setItemMeta(meta);
            return it;
        }).clone();
    }

    /** Color of a border slot: d1 = distance behind the cyan scanner, d2 = behind the purple one. */
    public static Material scan(int d1, int d2) {
        if (d1 == 0) return Material.WHITE_STAINED_GLASS_PANE;
        if (d1 == 1) return Material.CYAN_STAINED_GLASS_PANE;
        if (d1 == 2) return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        if (d1 == 3) return Material.BLUE_STAINED_GLASS_PANE;
        if (d2 == 0) return Material.MAGENTA_STAINED_GLASS_PANE;
        if (d2 == 1) return Material.PURPLE_STAINED_GLASS_PANE;
        if (d2 == 2) return Material.BLUE_STAINED_GLASS_PANE;
        return Material.BLACK_STAINED_GLASS_PANE;
    }

    public static MenuHolder create(String title, int rows) {
        MenuHolder h = new MenuHolder();
        Inventory inv = Bukkit.createInventory(h, rows * 9, MM.deserialize(title));
        h.setInventory(inv);
        return h;
    }

    public static List<Integer> innerSlots(int rows) {
        List<Integer> out = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) out.add(row * 9 + col);
        }
        return out;
    }

    public static List<Integer> centered(int n, int center) {
        n = Math.max(0, Math.min(n, 7));
        int spacing = n <= 4 ? 2 : 1;
        int start = center - ((n - 1) * spacing) / 2;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(start + i * spacing);
        return out;
    }

    /** Dark interior, neon border (animated), Gamerthuz star at bottom center. */
    public static void frame(HomesPlugin plugin, MenuHolder h, int rows) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
            Material m = !edge ? Material.BLACK_STAINED_GLASS_PANE
                    : (row == 0 ? Material.CYAN_STAINED_GLASS_PANE
                    : (row == rows - 1 ? Material.PURPLE_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE));
            h.set(i, paneStack(m), null);
        }
        ItemStack star = item(Material.NETHER_STAR,
                "<gradient:#ff9933:#138808><bold>" + Brand.AUTHOR + "</bold></gradient>",
                List.of("<gray>Developed by <white>" + Brand.AUTHOR,
                        "<dark_gray>" + plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion(),
                        "",
                        "<yellow>Click to visit GitHub"),
                true);
        int starSlot = size - 5;
        h.set(starSlot, star, e -> {
            Player p = (Player) e.getWhoClicked();
            p.closeInventory();
            Brand.sendGithub(p);
        });

        List<Integer> ring = new ArrayList<>();
        for (int c = 0; c < 9; c++) ring.add(c);
        for (int r = 1; r < rows - 1; r++) ring.add(r * 9 + 8);
        for (int c = 8; c >= 0; c--) ring.add((rows - 1) * 9 + c);
        for (int r = rows - 2; r >= 1; r--) ring.add(r * 9);
        ring.remove(Integer.valueOf(starSlot));
        h.setPerimeter(ring);
    }

    public static void sound(Player p, String name, float volume, float pitch) {
        try {
            Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(name));
            if (s != null) p.playSound(p.getLocation(), s, volume, pitch);
        } catch (Exception ignored) {
            // bad sound name
        }
    }
}
