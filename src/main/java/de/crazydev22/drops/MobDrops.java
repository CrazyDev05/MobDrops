package de.crazydev22.drops;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class MobDrops extends JavaPlugin implements Listener {
    private final Map<EntityType, List<Modifier>> modifiers = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        var modifiers = getConfig().getConfigurationSection("modifiers");
        if (modifiers == null)
            return;
        for (var key : modifiers.getKeys(false)) {
            EntityType entityType;
            try {
                entityType = EntityType.valueOf(key.toUpperCase());
            } catch (Throwable e) {
                getLogger().warning("Invalid modifier: " + key);
                continue;
            }
            var list = new ArrayList<Modifier>();

            int i = 0;
            for (var raw : modifiers.getList(key, List.of())) {
                if (!(raw instanceof Map<?, ?> map))
                    continue;

                try {
                    var chance = (Double) map.get("chance");
                    if (chance == null)
                        throw new IllegalArgumentException("Missing chance");

                    var materialString = (String) map.get("material");
                    if (materialString == null)
                        throw new IllegalArgumentException("Missing material");

                    Material material;
                    try {
                        material = Material.valueOf(materialString.toUpperCase());
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("Invalid material: " + materialString);
                    }

                    list.add(new Modifier(chance, material));
                } catch (Throwable e) {
                    getLogger().warning("Invalid modifier: " + key + "[" + i + "]");
                }
                i++;
            }

            getLogger().info("Loaded " + list.size() + " modifiers for " + entityType);
            this.modifiers.put(entityType, list);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(EntityDeathEvent event) {
        var modifiers = this.modifiers.get(event.getEntityType());
        if (modifiers == null || modifiers.isEmpty())
            return;

        var drops = event.getDrops();
        for (var modifier : modifiers) {
            if (Math.random() < modifier.chance)
                continue;

            drops.removeIf(modifier);
            if (drops.isEmpty())
                return;
        }
    }

    public record Modifier(double chance, Material material) implements Predicate<ItemStack> {

        @Override
        public boolean test(ItemStack itemStack) {
            return material == itemStack.getType();
        }
    }
}
