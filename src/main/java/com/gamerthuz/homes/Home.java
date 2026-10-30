package com.gamerthuz.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

/** One saved home. */
public final class Home {
    public final UUID owner;
    public String name;
    public String group = "";
    public String world = "world";
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public Material icon = Material.RED_BED;
    public boolean favorite;
    public boolean publicHome;
    public long created = System.currentTimeMillis();
    public int visits;

    public Home(UUID owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    public void setLocation(Location l) {
        this.world = l.getWorld().getName();
        this.x = l.getX();
        this.y = l.getY();
        this.z = l.getZ();
        this.yaw = l.getYaw();
        this.pitch = l.getPitch();
    }
}
