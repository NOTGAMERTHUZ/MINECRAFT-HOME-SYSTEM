package com.gamerthuz.homes;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.function.Consumer;

public final class HomeListener implements Listener {
    private final HomesPlugin plugin;

    public HomeListener(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        holder.click(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MenuHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!plugin.homes().isWarming(p.getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("settings.cancel-on-move", true)) return;
        Location a = e.getFrom();
        Location b = e.getTo();
        if (b == null) return;
        if (a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ()) {
            return;
        }
        plugin.homes().cancelWarmup(p, true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.homes().isWarming(p.getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("settings.cancel-on-damage", true)) return;
        plugin.homes().cancelWarmup(p, true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.prompts().has(p.getUniqueId())) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        plugin.sync(() -> {
            Consumer<String> handler = plugin.prompts().take(p.getUniqueId());
            if (handler == null) return;
            if (text.equalsIgnoreCase("cancel")) {
                plugin.messages().send(p, "prompt-cancelled");
                return;
            }
            handler.accept(text);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.store().touchName(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.homes().cleanup(e.getPlayer().getUniqueId());
        plugin.prompts().cancel(e.getPlayer().getUniqueId());
    }
}
