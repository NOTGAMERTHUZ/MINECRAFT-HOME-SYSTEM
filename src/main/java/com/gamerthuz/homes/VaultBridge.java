package com.gamerthuz.homes;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Only loaded when Vault is present. */
final class VaultBridge implements EconomyHook.Bridge {
    private final Economy eco;

    private VaultBridge(Economy eco) {
        this.eco = eco;
    }

    static VaultBridge create(HomesPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new VaultBridge(rsp.getProvider());
    }

    @Override
    public boolean has(OfflinePlayer p, double amount) {
        return eco.has(p, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer p, double amount) {
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    @Override
    public double balance(OfflinePlayer p) {
        return eco.getBalance(p);
    }

    @Override
    public String format(double amount) {
        return eco.format(amount);
    }
}
