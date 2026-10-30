package com.gamerthuz.homes;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MenuHolder implements InventoryHolder {
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private final List<Integer> perimeter = new ArrayList<>();
    private final Map<Integer, Material> last = new HashMap<>();
    private int phase;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void setPerimeter(List<Integer> slots) {
        perimeter.clear();
        perimeter.addAll(slots);
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        perimeter.remove(Integer.valueOf(slot));
        last.remove(slot);
        if (handler != null) handlers.put(slot, handler);
        else handlers.remove(slot);
    }

    public void click(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> h = handlers.get(e.getSlot());
        if (h != null) h.accept(e);
    }

    /** Moves the neon scanner one step around the border. */
    public void animate() {
        int n = perimeter.size();
        if (n == 0 || inventory == null) return;
        phase = (phase + 1) % n;
        for (int i = 0; i < n; i++) {
            int slot = perimeter.get(i);
            int d1 = Math.floorMod(phase - i, n);
            int d2 = Math.floorMod(phase + n / 2 - i, n);
            Material m = Gui.scan(d1, d2);
            if (last.get(slot) != m) {
                last.put(slot, m);
                inventory.setItem(slot, Gui.paneStack(m));
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
