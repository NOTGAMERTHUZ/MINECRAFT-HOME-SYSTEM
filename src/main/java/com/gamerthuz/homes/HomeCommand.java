package com.gamerthuz.homes;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.gamerthuz.homes.Messages.ph;

public final class HomeCommand implements CommandExecutor, TabCompleter {
    private final HomesPlugin plugin;

    public HomeCommand(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        Messages m = plugin.messages();
        String name = cmd.getName().toLowerCase();

        if (name.equals("homes") && args.length > 0) {
            String a0 = args[0].toLowerCase();
            if (a0.equals("reload")) {
                if (!s.hasPermission("gamerthuz.homes.admin")) {
                    m.send(s, "no-permission");
                } else {
                    plugin.reloadAll();
                    m.send(s, "reloaded");
                }
                return true;
            }
            if (a0.equals("about")) {
                s.sendMessage(Brand.prefix().append(Component.text(
                        plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion())));
                s.sendMessage(Brand.footer());
                return true;
            }
        }

        if (!(s instanceof Player p)) {
            m.send(s, "player-only");
            return true;
        }
        HomeManager hm = plugin.homes();
        if (!hm.requireActive(p)) return true;

        switch (name) {
            case "sethome" -> hm.set(p, args.length > 0 ? args[0]
                    : plugin.getConfig().getString("settings.default-home-name", "home"));
            case "home" -> home(p, args);
            case "delhome" -> {
                if (args.length == 0) {
                    m.send(p, "usage-delhome");
                    return true;
                }
                Home h = hm.find(p, args[0]);
                if (h == null) m.send(p, "not-found", ph("home", args[0]));
                else hm.delete(p, h);
            }
            case "renamehome" -> {
                if (args.length < 2) {
                    m.send(p, "usage-renamehome");
                    return true;
                }
                Home h = hm.find(p, args[0]);
                if (h == null) m.send(p, "not-found", ph("home", args[0]));
                else hm.rename(p, h, args[1]);
            }
            case "homes" -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("public")) Menus.publicHomes(plugin, p, 0);
                else Menus.main(plugin, p, 0);
            }
            default -> { }
        }
        return true;
    }

    private void home(Player p, String[] args) {
        Messages m = plugin.messages();
        HomeManager hm = plugin.homes();

        if (args.length == 0) {
            List<Home> list = hm.homesIn(p);
            if (list.isEmpty()) {
                m.send(p, "no-homes");
                return;
            }
            Home target = hm.favorite(p);
            if (target == null) target = hm.find(p, plugin.getConfig().getString("settings.default-home-name", "home"));
            if (target == null && list.size() == 1) target = list.get(0);
            if (target != null) hm.teleport(p, target);
            else Menus.main(plugin, p, 0);
            return;
        }

        String arg = args[0];
        if (arg.contains(":")) {
            String[] parts = arg.split(":", 2);
            OfflinePlayer owner = Bukkit.getOfflinePlayerIfCached(parts[0]);
            if (owner == null) {
                m.send(p, "player-not-found");
                return;
            }
            WorldGroups.Group g = hm.active(p);
            Home h = g == null ? null : plugin.store().get(owner.getUniqueId(), g.id(), parts[1]);
            if (h == null) m.send(p, "not-found", ph("home", parts[1]));
            else hm.teleport(p, h);
            return;
        }
        Home h = hm.find(p, arg);
        if (h == null) m.send(p, "not-found", ph("home", arg));
        else hm.teleport(p, h);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase();
        if (args.length == 1 && s instanceof Player p) {
            if (name.equals("home") || name.equals("delhome") || name.equals("renamehome")) {
                for (Home h : plugin.homes().homesIn(p)) out.add(h.name);
            } else if (name.equals("homes")) {
                out.add("public");
                out.add("about");
                if (s.hasPermission("gamerthuz.homes.admin")) out.add("reload");
            }
            String prefix = args[0].toLowerCase();
            out.removeIf(x -> !x.toLowerCase().startsWith(prefix));
        }
        return out;
    }
}
