package gg.mrpratic;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the configured Practice worlds without deleting or resetting any world.
 * Existing world folders are loaded as-is; new worlds are created only when absent.
 */
public final class WorldBootstrap {
    private WorldBootstrap() {}

    public static Map<String, World> load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "worlds.yml");
        if (!file.exists()) plugin.saveResource("worlds.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, World> worlds = new LinkedHashMap<>();

        for (String key : config.getConfigurationSection("worlds").getKeys(false)) {
            String name = config.getString("worlds." + key + ".name", key);
            World world = Bukkit.getWorld(name);
            if (world == null) {
                world = Bukkit.createWorld(new WorldCreator(name));
            }
            if (world == null) {
                plugin.getLogger().severe("Dünya yüklenemedi: " + name);
                continue;
            }
            world.setAutoSave(true);
            world.setGameRule(GameRule.DO_MOB_SPAWNING,
                    config.getBoolean("worlds." + key + ".mob-spawning", false));
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,
                    config.getBoolean("worlds." + key + ".daylight-cycle", false));
            world.setGameRule(GameRule.DO_WEATHER_CYCLE,
                    config.getBoolean("worlds." + key + ".weather-cycle", false));
            worlds.put(key, world);
            plugin.getLogger().info("Dünya hazır: " + name);
        }
        return worlds;
    }
}
