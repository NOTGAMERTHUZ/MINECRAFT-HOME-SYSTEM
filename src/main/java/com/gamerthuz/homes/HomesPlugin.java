package com.gamerthuz.homes;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HomesPlugin extends JavaPlugin {
    private Messages messages;
    private EconomyHook economy;
    private HomeStore store;
    private HomeManager homes;
    private PromptManager prompts;
    private WorldGroups groups;
    private BukkitTask animation;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Brand.banner(this);

        groups = WorldGroups.load(this);
        messages = new Messages(this);
        economy = new EconomyHook(this);
        store = new HomeStore(this);
        homes = new HomeManager(this);
        prompts = new PromptManager(this);

        getServer().getPluginManager().registerEvents(new HomeListener(this), this);

        HomeCommand handler = new HomeCommand(this);
        for (String name : new String[]{"sethome", "home", "delhome", "renamehome", "homes"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(handler);
                cmd.setTabCompleter(handler);
            }
        }
        startAnimation();
    }

    @Override
    public void onDisable() {
        if (animation != null) animation.cancel();
        if (store != null) store.saveNow();
    }

    public void reloadAll() {
        reloadConfig();
        groups = WorldGroups.load(this);
        startAnimation();
    }

    private void startAnimation() {
        if (animation != null) animation.cancel();
        animation = null;
        if (!getConfig().getBoolean("settings.animated-gui", true)) return;
        animation = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof MenuHolder h) {
                    h.animate();
                }
            }
        }, 3L, 3L);
    }

    /** Run on the next tick (safe for opening menus from click events). */
    public void sync(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    public WorldGroups groups() { return groups; }

    public Messages messages() { return messages; }

    public EconomyHook economy() { return economy; }

    public HomeStore store() { return store; }

    public HomeManager homes() { return homes; }

    public PromptManager prompts() { return prompts; }
}
