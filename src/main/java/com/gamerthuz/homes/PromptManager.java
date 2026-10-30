package com.gamerthuz.homes;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.gamerthuz.homes.Messages.ph;

/** Lets a player type a value in chat (used for names). */
public final class PromptManager {
    private record Prompt(Consumer<String> handler, BukkitTask expiry) {}

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final HomesPlugin plugin;
    private final Map<UUID, Prompt> prompts = new HashMap<>();

    public PromptManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String messageKey, Consumer<String> handler) {
        UUID id = p.getUniqueId();
        cancel(id);
        int secs = Math.max(5, plugin.getConfig().getInt("settings.prompt-timeout", 30));
        BukkitTask expiry = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (prompts.remove(id) != null && p.isOnline()) plugin.messages().send(p, "prompt-expired");
        }, secs * 20L);
        prompts.put(id, new Prompt(handler, expiry));
        plugin.messages().send(p, messageKey, ph("seconds", String.valueOf(secs)));
        p.showTitle(Title.title(
                MM.deserialize("<gradient:#00f5ff:#7b2ff7><bold>TYPE IN CHAT</bold></gradient>"),
                MM.deserialize("<gray>or type <white>cancel"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(4), Duration.ofMillis(400))));
        Gui.sound(p, "block.note_block.bell", 1f, 1.6f);
    }

    public boolean has(UUID id) {
        return prompts.containsKey(id);
    }

    public Consumer<String> take(UUID id) {
        Prompt pr = prompts.remove(id);
        if (pr == null) return null;
        pr.expiry().cancel();
        return pr.handler();
    }

    public void cancel(UUID id) {
        Prompt pr = prompts.remove(id);
        if (pr != null) pr.expiry().cancel();
    }
}
