package dev.yive.pluto;

import com.destroystokyo.paper.util.SneakyThrow;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;

public class PlutoConfig {
    public static final int CURRENT_CONFIG_VERSION = 1;

    private static final Object[] EMPTY = new Object[0];

    private static File configFile;
    public static YamlConfiguration config;
    private static int configVersion;

    public static void init(final File file) {
        PlutoConfig.configFile = file;
        final YamlConfiguration config = new YamlConfiguration();
        config.options().copyDefaults(true);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failure to create pluto config", ex);
            }
        } else {
            try {
                config.load(file);
            } catch (final Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failure to load pluto config", ex);
                SneakyThrow.sneaky(ex);
                throw new RuntimeException(ex);
            }
        }

        PlutoConfig.load(config);
    }

    public static void load(final YamlConfiguration config) {
        PlutoConfig.config = config;
        PlutoConfig.configVersion = PlutoConfig.getInt("config-version-do-not-modify", CURRENT_CONFIG_VERSION);
        PlutoConfig.set("config-version-do-not-modify", CURRENT_CONFIG_VERSION);

        for (final Method method : PlutoConfig.class.getDeclaredMethods()) {
            if (method.getReturnType() != void.class || method.getParameterCount() != 0 ||
                    !Modifier.isPrivate(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(null, EMPTY);
            } catch (final Exception ex) {
                SneakyThrow.sneaky(ex);
                throw new RuntimeException(ex);
            }
        }

        try {
            config.save(PlutoConfig.configFile);
        } catch (final Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Unable to save pluto config", ex);
        }
    }

    static void remove(final String path) {
        PlutoConfig.config.set(path, null);
    }

    static void set(final String path, final Object value) {
        PlutoConfig.config.set(path, value);
    }

    static String getString(final String path, final String dfl) {
        PlutoConfig.config.addDefault(path, dfl);
        return PlutoConfig.config.getString(path, dfl);
    }

    static boolean getBoolean(final String path, final boolean dfl) {
        PlutoConfig.config.addDefault(path, dfl);
        return PlutoConfig.config.getBoolean(path, dfl);
    }

    static int getInt(final String path, final int dfl) {
        PlutoConfig.config.addDefault(path, dfl);
        return PlutoConfig.config.getInt(path, dfl);
    }

    static long getLong(final String path, final long dfl) {
        PlutoConfig.config.addDefault(path, dfl);
        return PlutoConfig.config.getLong(path, dfl);
    }

    static double getDouble(final String path, final double dfl) {
        PlutoConfig.config.addDefault(path, dfl);
        return PlutoConfig.config.getDouble(path, dfl);
    }

    static <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return (List<T>) config.getList(path, config.getList(path));
    }

    public static final class WorldConfig {

        public final String worldName;
        public ConfigurationSection config;
        ConfigurationSection worldDefaults;

        public WorldConfig(final String worldName) {
            this.worldName = worldName;
            this.init();
        }

        public void init() {
            this.worldDefaults = PlutoConfig.config.getConfigurationSection("world-settings.default");
            if (this.worldDefaults == null) {
                this.worldDefaults = PlutoConfig.config.createSection("world-settings.default");
            }

            String worldSectionPath = "world-settings.".concat(this.worldName);
            ConfigurationSection section = PlutoConfig.config.getConfigurationSection(worldSectionPath);
            if (section == null) {
                section = PlutoConfig.config.createSection(worldSectionPath);
            }
            PlutoConfig.config.set(worldSectionPath, section);

            this.load(section);
        }

        public void load(final ConfigurationSection config) {
            this.config = config;

            for (final Method method : WorldConfig.class.getDeclaredMethods()) {
                if (method.getReturnType() != void.class || method.getParameterCount() != 0 ||
                        !Modifier.isPrivate(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(this, EMPTY);
                } catch (final Exception ex) {
                    SneakyThrow.sneaky(ex);
                    throw new RuntimeException(ex);
                }
            }

            try {
                PlutoConfig.config.save(PlutoConfig.configFile);
            } catch (final Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Unable to save pluto config", ex);
            }
        }

        void remove(final String path) {
            this.worldDefaults.set(path, null);
            this.config.set(path, null);
        }

        void set(final String path, final Object val) {
            this.worldDefaults.set(path, val);
            if (this.config.get(path) != null) {
                this.config.set(path, val);
            }
        }

        boolean getBoolean(final String path, final boolean dfl) {
            this.worldDefaults.addDefault(path, Boolean.valueOf(dfl));
            return this.config.getBoolean(path, this.worldDefaults.getBoolean(path));
        }

        int getInt(final String path, final int dfl) {
            this.worldDefaults.addDefault(path, Integer.valueOf(dfl));
            return this.config.getInt(path, this.worldDefaults.getInt(path));
        }

        long getLong(final String path, final long dfl) {
            this.worldDefaults.addDefault(path, Long.valueOf(dfl));
            return this.config.getLong(path, this.worldDefaults.getLong(path));
        }

        double getDouble(final String path, final double dfl) {
            this.worldDefaults.addDefault(path, Double.valueOf(dfl));
            return this.config.getDouble(path, this.worldDefaults.getDouble(path));
        }

        public boolean useGameEventCache = true;
        private void shouldUseGameEventCache() {
            useGameEventCache = getBoolean("misc.use-game-events-cache", useGameEventCache);
        }

        public boolean useCustomNameOptimisation = true;
        private void shouldUseCustomNameOptimisation() {
            useCustomNameOptimisation = getBoolean("entities.global.use-custom-name-api-optimisation", useCustomNameOptimisation);
        }
    }
}