package com.gamerthuz.homes;

import org.bukkit.OfflinePlayer;

/** Safe wrapper: the plugin works even if Vault is not installed. */
public final class EconomyHook {

    public interface Bridge {
        boolean has(OfflinePlayer p, double amount);

        boolean withdraw(OfflinePlayer p, double amount);

        double balance(OfflinePlayer p);

        String format(double amount);
    }

    private final HomesPlugin plugin;
    private Bridge bridge;

    public EconomyHook(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        if (bridge != null) return true;
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        try {
            bridge = VaultBridge.create(plugin);
        } catch (Throwable t) {
            bridge = null;
        }
        return bridge != null;
    }

    public boolean has(OfflinePlayer p, double amount) {
        return available() && bridge.has(p, amount);
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        return available() && bridge.withdraw(p, amount);
    }

    public double balance(OfflinePlayer p) {
        return available() ? bridge.balance(p) : 0;
    }

    public String format(double amount) {
        return available() ? bridge.format(amount) : String.valueOf(amount);
    }
}
