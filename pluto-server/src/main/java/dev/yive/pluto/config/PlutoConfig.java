package dev.yive.pluto.config;

import com.destroystokyo.paper.util.SneakyThrow;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlutoConfig {
    public static final List<String> CONFIG_HEADER = List.of(
            "Configuration file for Pluto.",
            "A lot of these configuration options are geared towards performance.",
            "Some can change gameplay so make sure to test before applying to production.",
            "",
            "Github: https://github.com/Yive/Pluto",
            "Downloads: https://ci.yive.dev/job/Pluto/",
            ""
    );
    public static final int CURRENT_CONFIG_VERSION = 3;

    private static final Object[] EMPTY = new Object[0];

    private static File configFile;
    public static YamlConfiguration config;
    private static int configVersion;

    public static void init(final File file) {
        PlutoConfig.configFile = file;
        final YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(CONFIG_HEADER);
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

    static void setComments(final String path, final List<String> comments) {
        PlutoConfig.config.setComments(path, comments);
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

        private final String legacyWorldName;
        private final String worldName;
        public ConfigurationSection config;
        ConfigurationSection worldDefaults;

        public WorldConfig(final String legacyWorldName, final Key worldKey) {
            this.legacyWorldName = legacyWorldName;
            this.worldName = worldKey.asString();
            this.init();
        }

        public void init() {
            this.worldDefaults = PlutoConfig.config.getConfigurationSection("world-settings.default");
            if (this.worldDefaults == null) {
                this.worldDefaults = PlutoConfig.config.createSection("world-settings.default");
            }

            if (PlutoConfig.configVersion <= 2) {
                String worldSectionPath = "world-settings.".concat(this.legacyWorldName);
                ConfigurationSection section = PlutoConfig.config.getConfigurationSection(worldSectionPath);
                if (section != null) {
                    PlutoConfig.config.set(worldSectionPath, null);
                    PlutoConfig.config.set("world-settings.".concat(this.worldName), section);
                    Bukkit.getLogger().info("NOTE: Migrated Pluto world config %s -> %s".formatted(this.legacyWorldName, this.worldName));
                }
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

        void setComments(final String path, final List<String> comments) {
            this.worldDefaults.setComments(path, comments);
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
            setComments("misc.use-game-events-cache",
                    List.of(
                            "Makes use of a cache for mojang game events when converting to bukkit based game events",
                            "This will provide a performance boost due to no longer converting the mojang game event to bukkit."
                    )
            );
        }

        public boolean useCustomNameOptimisation = true;
        private void shouldUseCustomNameOptimisation() {
            useCustomNameOptimisation = getBoolean("entities.global.use-custom-name-api-optimisation", useCustomNameOptimisation);
            setComments("entities.global.use-custom-name-api-optimisation",
                    List.of(
                            "Optimises the Bukkit API function Nameable#setCustomName",
                            "This will provide a performance boost due to no longer containing the regex to convert the text into a clickable link."
                    )
            );
        }

        public boolean useStaticGrowthSpeedCrops = false;
        public float staticGrowthSpeedCrops = 1.0F;
        public boolean useStaticGrowthSpeedStems = false;
        public float staticGrowthSpeedStems = 1.0F;
        public boolean useStaticGrowthSpeedPitchers = false;
        public float staticGrowthSpeedPitchers = 1.0F;
        private void shouldUseStaticGrowthSpeed() {
            useStaticGrowthSpeedCrops = getBoolean("blocks.crops.static-growth-speed.enabled", useStaticGrowthSpeedCrops);
            staticGrowthSpeedCrops = (float) getDouble("blocks.crops.static-growth-speed.speed", staticGrowthSpeedCrops);
            setComments("blocks.crops.static-growth-speed",
                    List.of(
                            "Controls the growth speed of crops (wheat, potatoes, etc...)",
                            "This will provide a performance boost, but it will change gameplay.",
                            "Enabling this will prevent the growth speed boost from alternating rows",
                            "See: https://web.archive.org/web/20210602144319/https://twitter.com/Xilefian/status/1400099375939047424"
                    )
            );

            useStaticGrowthSpeedStems = getBoolean("blocks.stems.static-growth-speed.enabled", useStaticGrowthSpeedStems);
            staticGrowthSpeedStems = (float) getDouble("blocks.stems.static-growth-speed.speed", staticGrowthSpeedStems);
            setComments("blocks.stems.static-growth-speed",
                    List.of(
                            "Controls the growth speed of stems (melon/pumpkin)",
                            "Crops comment goes into more detail."
                    )
            );

            useStaticGrowthSpeedPitchers = getBoolean("blocks.pitcher-plant.static-growth-speed.enabled", useStaticGrowthSpeedPitchers);
            staticGrowthSpeedPitchers = (float) getDouble("blocks.pitcher-plant.static-growth-speed.speed", staticGrowthSpeedPitchers);
            setComments("blocks.pitcher-plant.static-growth-speed",
                    List.of(
                            "Controls the growth speed of pitcher plant",
                            "Crops comment goes into more detail."
                    )
            );
        }

        public boolean alwaysMoistFarmland = false;
        public boolean alwaysMoistSugarCane = false;
        private void alwaysMoistBlocks() {
            alwaysMoistFarmland = getBoolean("blocks.farmland.always-moist", alwaysMoistFarmland);
            setComments("blocks.farmland.always-moist",
                    List.of(
                            "Makes all farmland always wet.",
                            "This will provide a performance boost,",
                            "but it means farmland doesn't dry out."
                    )
            );

            alwaysMoistSugarCane = getBoolean("blocks.sugarcane.always-moist", alwaysMoistSugarCane);
            setComments("blocks.sugarcane.always-moist",
                    List.of(
                            "Makes all sugar cane assume they're near water.",
                            "This provides a performance improvement, but it also",
                            "creates a bug allow players to place it on any solid block." // TODO: Fix this bug with a toggle for build servers
                    )
            );
        }

        public boolean cactusCheckSurvivalBeforeGrowth = false;
        private void cactusCheckSurvivalBeforeGrowth() {
            cactusCheckSurvivalBeforeGrowth = getBoolean("blocks.cactus.check-survival-before-growth", cactusCheckSurvivalBeforeGrowth);
            setComments("blocks.cactus.check-survival-before-growth",
                    List.of(
                            "Does an early check for if the cactus will instantly break on growth.",
                            "This will provide a performance boost for servers with cactus farms,",
                            "but it could also increase the output of cacti on the server."
                    )
            );
        }

        public boolean lessRandomDispensing = false;
        private void lessRandomDispensing() {
            lessRandomDispensing = getBoolean("blocks.dispenser.less-random-dispensing", lessRandomDispensing);
            setComments("blocks.dispenser.less-random-dispensing",
                    List.of(
                            "Dispenses the first non-empty slot in a dispenser/dropper.",
                            "This will remove the random aspect of dispensers/droppers."
                    )
            );
        }

        public boolean disableDropperInventoryMoveEvent = false;
        private void disableDropperInventoryMoveEvent() {
            disableDropperInventoryMoveEvent = getBoolean("blocks.dropper.disable-move-event", disableDropperInventoryMoveEvent);
            setComments("blocks.dropper.disable-move-event",
                    List.of(
                            "Prevents the server from firing the InventoryMoveEvent from droppers.",
                            "This will provide a performance boost, but prevent",
                            "plugins from knowing when an item moves from a dropper.",
                            "This is similar to the hopper setting from Paper."
                    )
            );
        }

        public boolean disableAllayGameEventListening = false;
        public boolean disableAllayDuplication = false;
        private void allayConfiguration() {
            disableAllayGameEventListening = getBoolean("entities.allay.disable-game-event-listener", disableAllayGameEventListening);
            setComments("entities.allay.disable-game-event-listener",
                    List.of(
                            "Prevents allys from listening for game events.",
                            "This will provide a performance boost,",
                            "but prevent allays from interacting with jukeboxes.",
                            "Only use this as a last ditch effort in improving allay performance."
                    )
            );

            disableAllayDuplication = getBoolean("entities.allay.disable-duplication", disableAllayDuplication);
            setComments("entities.allay.disable-duplication", List.of("Prevents allays from breeding."));
        }

        public boolean removeExcessMinecarts = false;
        public boolean removeExcessBoats = false;
        public int excessMinecartsLimit = 10;
        public int excessBoatsLimit = 10;

        private void removeExcessVehicles() {
            removeExcessMinecarts = getBoolean("entities.minecart.remove-excess.enabled", removeExcessMinecarts);
            excessMinecartsLimit = getInt("entities.minecart.remove-excess.limit", excessMinecartsLimit);
            setComments("entities.minecart.remove-excess",
                    List.of(
                            "Removes excess amounts of minecarts when a lot are colliding with each other.",
                            "This will provide a performance boost and prevent crash attempts.",
                            "It is unlikely real players will have large clusters of minecarts together."
                    )
            );

            removeExcessBoats = getBoolean("entities.boat.remove-excess.enabled", removeExcessBoats);
            excessBoatsLimit = getInt("entities.boat.remove-excess.limit", excessBoatsLimit);
            setComments("entities.boat.remove-excess",
                    List.of(
                            "Removes excess amounts of boats when a lot are colliding with each other.",
                            "This will provide a performance boost and prevent crash attempts.",
                            "It is unlikely real players will have large clusters of boats together."
                    )
            );
        }

        public boolean disableBlockGenerationFromFluids = false;
        private void disableBlockGenerationFromFluids() {
            disableBlockGenerationFromFluids = getBoolean("blocks.fluids.disable-block-generation", disableBlockGenerationFromFluids);
            setComments("blocks.fluids.disable-block-generation", List.of("Prevents water and lava from generating blocks when touching."));
        }

        public boolean disableNetherPortalGeneration = false;
        private void disableNetherPortalGeneration() {
            disableNetherPortalGeneration = getBoolean("blocks.fire.disable-nether-portal-generation", disableNetherPortalGeneration);
            setComments("blocks.fire.disable-nether-portal-generation", List.of("Prevents activating nether portals with fire."));
        }

        public int entityActivationInterval = 1;
        public boolean disableEntityActivation = false;
        private void entityActivationConfiguration() {
            entityActivationInterval = Math.min(1, getInt("entities.global.activation-interval", entityActivationInterval));
            setComments("entities.global.activation-interval",
                    List.of(
                            "Tick interval for when an entities within activation range will get activated.",
                            "1 = vanilla (every tick)"
                    )
            );

            disableEntityActivation = getBoolean("entities.global.disable-activation", disableEntityActivation);
            setComments("entities.global.disable-activation",
                    List.of(
                            "Prevents entities from ever being activated.",
                            "Could be useful in lobbies or creative servers."
                    )
            );
        }

        public boolean disableItemMergeCheckWhileMoving = false;
        public int itemMergeMovingInterval = 2;
        public int itemMergeStaticInterval = 40;
        private void itemMergeConfiguration() {
            disableItemMergeCheckWhileMoving = getBoolean("entities.item.disable-merge-check-when-moving", disableItemMergeCheckWhileMoving);
            setComments("entities.item.disable-merge-check-when-moving",
                    List.of(
                            "Prevents dropped items searching for other dropped items to merge with whilst moving.",
                            "This can provide a performance improvement, but this depends on the type of bases players have.",
                            "Could cause worse performance if players have dropped items flowing in water loops."
                    )
            );

            itemMergeMovingInterval = getInt("entities.item.merge-check-moving-interval", itemMergeMovingInterval);
            setComments("entities.item.merge-check-moving-interval",
                    List.of(
                            "Tick interval for how often a dropped item will search for other dropped items whilst moving.",
                            "Most servers should tweak this over using disable-merge-check-when-moving",
                            "This will provide a performance improvement for most cases.",
                            "2 = default (vanilla)"
                    )
            );

            itemMergeStaticInterval = getInt("entities.item.merge-check-static-interval", itemMergeStaticInterval);
            setComments("entities.item.merge-check-static-interval",
                    List.of(
                            "Tick interval for how often a dropped item will search for other dropped items whilst not moving.",
                            "This will provide a performance improvement for most cases.",
                            "40 = default (vanilla)"
                    )
            );
        }

        public boolean cropIgnoreLightLevel = false;
        public boolean mushroomIgnoreLightLevel = false;
        public boolean stemIgnoreLightLevel = false;
        public boolean bambooSaplingIgnoreLightLevel = false;
        public boolean bambooStalkIgnoreLightLevel = false;
        public boolean sweetBerryBushIgnoreLightLevel = false;
        public boolean saplingIgnoreLightLevel = false;
        public boolean pitcherCropIgnoreLightLevel = false;
        private void ignoreLightLevels() {
            cropIgnoreLightLevel = getBoolean("blocks.crops.ignore-light-level", cropIgnoreLightLevel);
            setComments("blocks.crops.ignore-light-level",
                    List.of(
                            "Makes crops ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            mushroomIgnoreLightLevel = getBoolean("blocks.mushroom.ignore-light-level", mushroomIgnoreLightLevel);
            setComments("blocks.mushroom.ignore-light-level",
                    List.of(
                            "Makes mushrooms ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            stemIgnoreLightLevel = getBoolean("blocks.stems.ignore-light-level", stemIgnoreLightLevel);
            setComments("blocks.stems.ignore-light-level",
                    List.of(
                            "Makes stems ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            bambooSaplingIgnoreLightLevel = getBoolean("blocks.bamboo-sapling.ignore-light-level", bambooSaplingIgnoreLightLevel);
            setComments("blocks.bamboo-sapling.ignore-light-level",
                    List.of(
                            "Makes bamboo saplings ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            bambooStalkIgnoreLightLevel = getBoolean("blocks.bamboo-stalk.ignore-light-level", bambooStalkIgnoreLightLevel);
            setComments("blocks.bamboo-stalk.ignore-light-level",
                    List.of(
                            "Makes bamboo stalks ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            sweetBerryBushIgnoreLightLevel = getBoolean("blocks.sweet-berry-bush.ignore-light-level", sweetBerryBushIgnoreLightLevel);
            setComments("blocks.sweet-berry-bush.ignore-light-level",
                    List.of(
                            "Makes berry bushes ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            saplingIgnoreLightLevel = getBoolean("blocks.sapling.ignore-light-level", saplingIgnoreLightLevel);
            setComments("blocks.sapling.ignore-light-level",
                    List.of(
                            "Makes saplings ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );

            pitcherCropIgnoreLightLevel = getBoolean("blocks.pitcher-plant.ignore-light-level", pitcherCropIgnoreLightLevel);
            setComments("blocks.pitcher-plant.ignore-light-level",
                    List.of(
                            "Makes pitcher plants ignore light levels and grow no matter how dark the surroundings are",
                            "This might improve performance, but this was more useful in older versions."
                    )
            );
        }

        public boolean disableTargetSelector = false;
        public boolean disableGoalSelector = false;
        private void disableTargetSelector() {
            disableTargetSelector = getBoolean("entities.global.disable-target-selector", disableTargetSelector);
            setComments("entities.global.disable-target-selector",
                    List.of(
                            "Disables the target selector for all entities",
                            "This will improve performance by getting rid of their targeting AI,",
                            "but also makes all entities blind in terms of seeing other entities."
                    )
            );

            disableGoalSelector = getBoolean("entities.global.disable-goal-selector", disableGoalSelector);
            setComments("entities.global.disable-goal-selector",
                    List.of(
                            "Disables the goal selector for all entities",
                            "This will improve performance by getting rid of their goal AI,",
                            "but this means that entities will have no goals in life.",
                            "Just being motionless and only targeting nearby entities"
                    )
            );
        }

        public boolean spawnerSettingsEnabled = false;
        public boolean spawnerCheckForNearbyPlayers = true;
        public boolean spawnerCheckForNearbyEntities = true;
        public boolean spawnerCheckForBlockCollision = true;
        public boolean spawnerDisableBabySpawns = false;
        public boolean spawnerDisableParticles = false;
        public int spawnerMinSpawnDelay = 200;
        public int spawnerMaxSpawnDelay = 800;
        // TODO: Check if Mojang no longer saves/loads the following as shorts.
        public short spawnerSpawnCount = 4;
        public short spawnerMaxNearbyEntities = 6;
        public short spawnerRequiredPlayerRange = 16;
        public short spawnerSpawnRange = 4;
        private void spawnerConfiguration() {
            spawnerSettingsEnabled = getBoolean("blocks.spawner.enable-custom-settings", spawnerSettingsEnabled);
            setComments("blocks.spawner.enable-custom-settings",
                    List.of(
                            "This will allow configuration of default spawner values.",
                            "Obviously this will cause spawners to act differently from vanilla if changed.",
                            "Most of these will improve performance, but it depends on how you configure them.",
                            "Spawners that get modified via the Bukkit API will keep the changes from the API.",
                            "Note: spawner ttl ignores what 'enable-custom-settings' is set to."
                    )
            );

            spawnerCheckForNearbyPlayers = getBoolean("blocks.spawner.check-for-nearby-players", spawnerCheckForNearbyPlayers);
            setComments("blocks.spawner.check-for-nearby-players",
                    List.of(
                            "Controls if the spawner will check for nearby players before spawning an entity.",
                            "This setting is not related to 'enable-custom-settings'",
                            "default = true"
                    )
            );

            spawnerCheckForNearbyEntities = getBoolean("blocks.spawner.check-for-nearby-entities", spawnerCheckForNearbyEntities);
            setComments("blocks.spawner.check-for-nearby-entities",
                    List.of(
                            "Controls if the spawner will check for nearby entities before spawning an entity.",
                            "This setting is not related to 'enable-custom-settings'",
                            "default = true"
                    )
            );

            spawnerCheckForBlockCollision = getBoolean("blocks.spawner.check-for-block-collision", spawnerCheckForBlockCollision);
            setComments("blocks.spawner.check-for-block-collision",
                    List.of(
                            "Controls if the spawner will check for spawn points that have no collision before spawning an entity",
                            "This setting is not related to 'enable-custom-settings'",
                            "default = true"
                    )
            );

            spawnerDisableBabySpawns = getBoolean("blocks.spawner.disable-spawning-babies", spawnerDisableBabySpawns);
            setComments("blocks.spawner.check-for-block-collision",
                    List.of(
                            "Prevents spawners from spawning babies such as zombie jockeys",
                            "This setting is not related to 'enable-custom-settings'",
                            "default = false"
                    )
            );

            spawnerDisableParticles = getBoolean("blocks.spawner.disable-spawn-particles", spawnerDisableParticles);
            setComments("blocks.spawner.disable-spawn-particles",
                    List.of(
                            "Disables the spawn particles",
                            "This setting is not related to 'enable-custom-settings'",
                            "default = false"
                    )
            );

            spawnerMinSpawnDelay = getInt("blocks.spawner.min-spawn-delay", spawnerMinSpawnDelay);
            setComments("blocks.spawner.min-spawn-delay",
                    List.of(
                            "Controls the minimum spawn delay before spawning an entity",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 200"
                    )
            );

            spawnerMaxSpawnDelay = getInt("blocks.spawner.max-spawn-delay", spawnerMaxSpawnDelay);
            setComments("blocks.spawner.max-spawn-delay",
                    List.of(
                            "Controls the minimum spawn delay before spawning an entity",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 800"
                    )
            );

            spawnerSpawnCount = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.spawn-count", spawnerSpawnCount));
            setComments("blocks.spawner.spawn-count",
                    List.of(
                            "Controls the amount of entities that will spawn",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 4"
                    )
            );

            spawnerMaxNearbyEntities = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.max-nearby-entities", spawnerMaxNearbyEntities));
            setComments("blocks.spawner.max-nearby-entities",
                    List.of(
                            "Controls the amount of entities that will prevent spawners from spawning entities",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 6"
                    )
            );

            spawnerRequiredPlayerRange = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.required-player-range", spawnerRequiredPlayerRange));
            setComments("blocks.spawner.required-player-range",
                    List.of(
                            "Controls how close a player needs to be for the spawner to spawn entities",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 16"
                    )
            );

            spawnerSpawnRange = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.spawn-range", spawnerSpawnRange));
            setComments("blocks.spawner.spawn-range",
                    List.of(
                            "Controls how far away entities can spawn from the spawner itself",
                            "This setting requires 'enable-custom-settings' to be set to true.",
                            "vanilla/default = 4"
                    )
            );
        }

        public boolean disableTntChainReaction = false;
        private void disableTntChainReaction() {
            disableTntChainReaction = getBoolean("blocks.tnt.disable-chain-reaction", disableTntChainReaction);
            setComments("blocks.tnt.disable-chain-reaction", List.of("Prevents explosions from igniting TNT."));
        }

        public boolean disableFlapEvents = false;
        private void disableFlapEvents() {
            disableFlapEvents = getBoolean("entities.global.disable-flapping-game-event", false);
            setComments("entities.global.disable-flapping-game-event",
                    List.of(
                            "Prevents flying entities throwing FLAP game events.",
                            "This will provide a huge performance boost for allay clusters,",
                            "it will also provide a minor performance boost for bees.",
                            "Obviously sculk sensors will no longer detect their flaps."
                    )
            );
        }

        public int blockTickingModulo = 1;
        private void blockTicking() {
            blockTickingModulo = getInt("blocks.global.block-ticking-modulo", blockTickingModulo);
            setComments("blocks.global.block-ticking-modulo",
                    List.of(
                            "Allows tick skipping for block ticking.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, -1 = disable block ticking entirely"
                    )
            );
        }

        public int fluidTickingModulo = 1;
        private void fluidTicking() {
            fluidTickingModulo = getInt("blocks.fluids.fluid-ticking-modulo", fluidTickingModulo);
            setComments("blocks.fluids.fluid-ticking-modulo",
                    List.of(
                            "Allows tick skipping for fluid ticking.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, -1 = disable fluid ticking entirely"
                    )
            );
        }

        public int raidTickingModulo = 1;
        private void raidTicking() {
            raidTickingModulo = getInt("misc.raids.raid-ticking-modulo", raidTickingModulo);
            setComments("misc.raids.raid-ticking-modulo",
                    List.of(
                            "Allows tick skipping for raid ticking.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, -1 = disable raid ticking entirely"
                    )
            );
        }

        public int blockEventTickingModulo = 1;
        private void blockEventTicking() {
            blockEventTickingModulo = Math.min(1, getInt("blocks.global.block-event-ticking-modulo", blockEventTickingModulo));
            setComments("blocks.global.block-event-ticking-modulo",
                    List.of(
                            "Allows tick skipping for block event ticking.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, the value can't go below 1"
                    )
            );
        }

        public int entityDespawnCheckModulo = 1;
        private void entityDespawning() {
            entityDespawnCheckModulo = getInt("entities.global.despawn-check-modulo", entityDespawnCheckModulo);
            setComments("entities.global.despawn-check-modulo",
                    List.of(
                            "Allows tick skipping for entity despawn checks.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, -1 = entirely disable entity despawning"
                    )
            );
        }

        public int entityPushingModulo = 1;
        private void entityPushing() {
            entityPushingModulo = getInt("entities.global.pushing-modulo", entityPushingModulo);
            setComments("entities.global.pushing-modulo",
                    List.of(
                            "Allows tick skipping for entity pushing.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "Even though I don't recommend tick skipping, this setting is recommended if you want to disable entity pushing all together.",
                            "1 = vanilla, -1 = entirely disable entity pushing"
                    )
            );
        }

        public int entityInsideBlockCheck = 1;
        private void entityInsideBlockCheck() {
            entityInsideBlockCheck = getInt("entities.global.inside-block-check-modulo", entityInsideBlockCheck);
            setComments("entities.global.inside-block-check-modulo",
                    List.of(
                            "Allows tick skipping for entity inside block checks.",
                            "This can provide a performance improvement, but also can drastically change gameplay.",
                            "Only touch this setting if you know what you're doing when it comes to tick skipping.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, -1 = entirely disable entities checking if they're inside a block"
                    )
            );
        }

        public boolean disableShulkersDroppingContentsWhenDestroyed = false;
        public boolean disableShulkerSplitting = false;
        public boolean disableShulkerTeleporting = false;
        private void shulkerSettings() {
            disableShulkersDroppingContentsWhenDestroyed = getBoolean("entities.item.disable-dropping-shulker-box-contents-when-destroyed", disableShulkersDroppingContentsWhenDestroyed);
            setComments("entities.item.disable-dropping-shulker-box-contents-when-destroyed", List.of("Prevents shulker boxes from dropping items when broken."));

            disableShulkerSplitting = getBoolean("entities.shulker.disable-splitting-from-bullets", disableShulkerSplitting);
            setComments("entities.shulker.disable-splitting-from-bullets",
                    List.of(
                            "Prevents shulkers from splitting when hit by their own bullet.",
                            "This will prevent players from abusing shulker farms"
                    )
            );

            disableShulkerTeleporting = getBoolean("entities.shulker.disable-random-teleports", disableShulkerTeleporting);
            setComments("entities.shulker.disable-random-teleports",
                    List.of(
                            "Prevents shulkers from teleporting.",
                            "This setting will also prevent shulker splitting if set to true."
                    )
            );
        }

        public int entityRainCheckRate = 10;
        private void entityRainCheckModulo() {
            entityRainCheckRate = Math.min(1, getInt("entities.global.rain-check-tick-rate", entityRainCheckRate));
            setComments("entities.global.rain-check-tick-rate",
                    List.of(
                            "Interval for how often entities will check for rain.",
                            "This can provide a performance improvement.",
                            "Though will cause entities that are on fire to have a delay on being extinguished from rain.",
                            "I don't really recommend this setting, but closed source paid server jars always include tick skipping.",
                            "1 = vanilla, 10 = default, the value can't go below 1"
                    )
            );
        }

        public boolean preventInsideBlockXrayExploit = false;
        private void disableInsideBlockXrayExploit() {
            preventInsideBlockXrayExploit = getBoolean("entities.player.prevent-xray-exploits-inside-certain-blocks", preventInsideBlockXrayExploit);
            setComments("entities.player.prevent-xray-exploits-inside-certain-blocks",
                    List.of(
                            "Prevents players from briefly having x-ray when falling blocks land on their head.",
                            "This will only affect players standing in cauldrons or composters",
                            "When enabled, the falling block will act as if it landed on a torch (aka spawns a dropped item)"
                    )
            );
        }

        private void spawnerTTL() {
            // Set a few default ones to create the section
            net.minecraft.world.entity.EntityType.ZOMBIE.spawnerTTL = getInt("blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(net.minecraft.world.entity.EntityType.ZOMBIE).getPath().toLowerCase(java.util.Locale.ROOT), -1);
            net.minecraft.world.entity.EntityType.SNOW_GOLEM.spawnerTTL = getInt("blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(net.minecraft.world.entity.EntityType.SNOW_GOLEM).getPath().toLowerCase(java.util.Locale.ROOT), -1);
            setComments("blocks.spawner.ttl",
                    List.of(
                            "This section will make entities spawned from spawners have a time to live rate in ticks.",
                            "See the resource location column in https://minecraft.wiki/w/Entity#Types_of_entities",
                            "This is case-sensitive so ZOMBIFIED_PIGLIN won't work, but zombified_piglin will.",
                            "-1 will disable the ttl, but deleting the entry will do that too.",
                            "The settings in this section are not related to 'enable-custom-settings'"
                    )
            );

            // These bypass setting the default entries to avoid a stupidly long section.
            for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                String path = "blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(type).getPath().toLowerCase(java.util.Locale.ROOT);
                type.spawnerTTL = this.config.getInt(path, this.worldDefaults.getInt(path, -1));
            }
        }

        public boolean skipFailedDispenseLevelEvent = false;
        private void skipFailedDispenseLevelEvent() {
            skipFailedDispenseLevelEvent = getBoolean("blocks.global.skip-failed-block-dispense-level-event", skipFailedDispenseLevelEvent);
            setComments("blocks.global.skip-failed-block-dispense-level-event",
                    List.of(
                            "Allows for disabling dispensers/droppers from firing the SOUND_DISPENSER_FAIL level event.",
                            "This can provide a performance improvement, but also means dispensers/droppers will only make sounds on successful dispenses"
                    )
            );
        }

        public boolean useFluidPushingOptimisation = true;
        private void useFluidPushingOptimisation() {
            useFluidPushingOptimisation = getBoolean("entities.global.use-fluid-pushing-optimisation", useFluidPushingOptimisation);
            setComments("entities.global.use-fluid-pushing-optimisation",
                    List.of(
                            "Optimises the fluid pushing checks on entities by not doing the same check for lava if the entity is already in water.",
                            "This should provide a performance improvement by doing one less fluid lookup per entity.",
                            "Note: Might cause issues with fluid pushing in general, but I've only been told this by one person using this setting.",
                            "I've never experienced the bug they had despite running this setting for years."
                    )
            );
        }

        public boolean onlyPlayersPushEntities = false;
        private void onlyPlayersPushEntities() {
            onlyPlayersPushEntities = getBoolean("entities.player.only-players-push-entities", onlyPlayersPushEntities);
            setComments("entities.player.only-players-push-entities",
                    List.of(
                            "Makes it so that only players can push entities.",
                            "Will conflict with 'entities.global.pushing-modulo' if that is set to -1"
                    )
            );
        }

        public int sweepingAttackLimit = -1;
        private void sweepingAttackLimit() {
            sweepingAttackLimit = getInt("entities.player.sweeping-attack-entity-limit", sweepingAttackLimit);
            setComments("entities.player.sweeping-attack-entity-limit",
                    List.of(
                            "Limits the amount of mobs sweeping edge can damage at once.",
                            "-1 = no limit (aka vanilla)"
                    )
            );
        }

        public boolean disableSprintParticles = false;
        private void disableSprintParticles() {
            disableSprintParticles = getBoolean("entities.global.particles.disable-sprint-particles", disableSprintParticles);
            setComments("entities.global.particles.disable-sprint-particles", List.of("Disables sprint particles from entities."));
        }

        public boolean entitiesSearchForHoppers = false;
        public int ticksBetweenEntitiesSearchForHoppers = 1;
        private void entitiesSearchForHoppers() {
            entitiesSearchForHoppers = getBoolean("entities.global.search-for-hoppers", entitiesSearchForHoppers);
            setComments("entities.global.search-for-hoppers",
                    List.of(
                            "Disables hoppers searching for dropped items / container entities.",
                            "This will provide a big performance improvement if your server has a lot of hoppers",
                            "It is not recommended though if your server has more dropped items / container entities than hoppers."
                    )
            );

            ticksBetweenEntitiesSearchForHoppers = Math.max(1, getInt("entities.global.ticks-between-hopper-searches", ticksBetweenEntitiesSearchForHoppers));
            setComments("entities.global.ticks-between-hopper-searches",
                    List.of(
                            "Controls how often certain entities will search for hoppers.",
                            "Requires: search-for-hoppers to be set to true"
                    )
            );
        }

        public boolean playerInventoryOnlyTickImportantSlots = false;
        public boolean playerInventoryOnlyTickHandSlots = false;
        public boolean playerInventoryOnlyUpdateImportantSlots = false;
        private void playerInventoryTicking() {
            if (configVersion == 1) {
                playerInventoryOnlyUpdateImportantSlots = this.config.getBoolean("entities.player.only-tick-important-inventory-slots", this.worldDefaults.getBoolean("entities.player.only-tick-important-inventory-slots"));
                remove("entities.player.only-tick-important-inventory-slots");
            }

            playerInventoryOnlyTickImportantSlots = getBoolean("entities.player.inventory-ticking.only-tick-important-slots", playerInventoryOnlyTickImportantSlots);
            setComments("entities.player.inventory-ticking.only-tick-important-slots",
                    List.of(
                            "Only the player's off hand & hot bar will be ticked versus the whole inventory",
                            "Will improve performance due to the ticked slots going from 41 to 10 slots."
                    )
            );

            playerInventoryOnlyTickHandSlots = getBoolean("entities.player.inventory-ticking.only-tick-hand-slots", playerInventoryOnlyTickHandSlots);
            setComments("entities.player.inventory-ticking.only-tick-hand-slots",
                    List.of(
                            "Only the player's hands will be ticked versus the whole inventory",
                            "This will override ticking the hot bar from 'only-tick-important-slots'.",
                            "Will improve performance more due to the ticked slots going from 41 to 2 slots.",
                            "Requires 'only-tick-important-slots' to be set to true."
                    )
            );

            playerInventoryOnlyUpdateImportantSlots = getBoolean("entities.player.inventory-ticking.only-update-important-slots", playerInventoryOnlyUpdateImportantSlots);
            setComments("entities.player.inventory-ticking.only-update-important-slots",
                    List.of(
                            "Only the player's hands will be updated versus the whole inventory",
                            "Will improve performance due to the updated slots going from 41 to 2 slots.",
                            "Note: Currently this only targets hands due to it only expecting maps."
                    )
            );
        }

        public boolean crafterDoSnapshotForTarget = true;
        public boolean crafterDoSnapshotForSelf = true;
        public boolean dropperDoSnapshotForTarget = true;
        public boolean dropperDoSnapshotForSelf = true;
        public boolean brewingStandDoSnapshotForSelf = true;
        private void blocksDoSnapshots() {
            crafterDoSnapshotForTarget = getBoolean("blocks.crafter.generate-snapshot-for-target", crafterDoSnapshotForTarget);
            setComments("blocks.crafter.generate-snapshot-for-target",
                    List.of(
                            "Should the crafter generate a Bukkit block state snapshot when",
                            "doing an inventory owner lookup for the target container. 99% of the time plugins",
                            "don't even need to use block state snapshots so it should be safe to set this to false."
                    )
            );

            crafterDoSnapshotForSelf = getBoolean("blocks.crafter.generate-snapshot-for-self", crafterDoSnapshotForSelf);
            setComments("blocks.crafter.generate-snapshot-for-self",
                    List.of(
                            "Should the crafter generate a Bukkit block state snapshot when",
                            "doing an inventory owner lookup for itself."
                    )
            );

            dropperDoSnapshotForTarget = getBoolean("blocks.dropper.generate-snapshot-for-target", dropperDoSnapshotForTarget);
            setComments("blocks.dropper.generate-snapshot-for-target",
                    List.of(
                            "Should the dropper generate a Bukkit block state snapshot when",
                            "doing an inventory owner lookup for the target container. 99% of the time plugins",
                            "don't even need to use block state snapshots so it should be safe to set this to false."
                    )
            );

            dropperDoSnapshotForSelf = getBoolean("blocks.dropper.generate-snapshot-for-self", dropperDoSnapshotForSelf);
            setComments("blocks.dropper.generate-snapshot-for-self",
                    List.of(
                            "Should the dropper generate a Bukkit block state snapshot when",
                            "doing an inventory owner lookup for itself."
                    )
            );

            brewingStandDoSnapshotForSelf = getBoolean("blocks.brewing-stand.generate-snapshot-for-self", brewingStandDoSnapshotForSelf);
            setComments("blocks.brewing-stand.generate-snapshot-for-self",
                    List.of(
                            "Should the brewing stand generate a Bukkit block state snapshot when",
                            "doing an inventory owner lookup for itself. 99% of the time plugins",
                            "don't even need to use block state snapshots so it should be safe to set this to false."
                    )
            );
        }

        public boolean useVerticalActivationRange = false;
        public int waterVerticalActivationRange = -1;
        public int flyingMonstersVerticalActivationRange = -1;
        public int villagersVerticalActivationRange = -1;
        public int monsterVerticalActivationRange = -1;
        public int animalVerticalActivationRange = -1;
        public int raiderVerticalActivationRange = -1;
        public int miscVerticalActivationRange = -1;
        private void verticalActivationRanges() {
            useVerticalActivationRange = getBoolean("entities.vertical-activation-range.enabled", useVerticalActivationRange);
            setComments("entities.vertical-activation-range.enabled",
                List.of(
                    "Makes the server use the activation range instead of world height for vertical distance"
                )
            );

            waterVerticalActivationRange = getInt("entities.vertical-activation-range.water", waterVerticalActivationRange);
            setComments("entities.vertical-activation-range.water",
                List.of(
                    "Vertical activation range for water based mobs. -1 will use the distance defined in spigot.yml"
                )
            );

            flyingMonstersVerticalActivationRange = getInt("entities.vertical-activation-range.flying-monsters", flyingMonstersVerticalActivationRange);
            setComments("entities.vertical-activation-range.flying-monsters",
                List.of(
                    "Vertical activation range for flying monsters. -1 will use the distance defined in spigot.yml"
                )
            );

            villagersVerticalActivationRange = getInt("entities.vertical-activation-range.villagers", villagersVerticalActivationRange);
            setComments("entities.vertical-activation-range.villagers",
                List.of(
                    "Vertical activation range for villagers. -1 will use the distance defined in spigot.yml"
                )
            );

            monsterVerticalActivationRange = getInt("entities.vertical-activation-range.monster", monsterVerticalActivationRange);
            setComments("entities.vertical-activation-range.monster",
                List.of(
                    "Vertical activation range for monsters. -1 will use the distance defined in spigot.yml"
                )
            );

            animalVerticalActivationRange = getInt("entities.vertical-activation-range.animal", animalVerticalActivationRange);
            setComments("entities.vertical-activation-range.animal",
                List.of(
                    "Vertical activation range for animals. -1 will use the distance defined in spigot.yml"
                )
            );

            raiderVerticalActivationRange = getInt("entities.vertical-activation-range.raider", raiderVerticalActivationRange);
            setComments("entities.vertical-activation-range.raider",
                List.of(
                    "Vertical activation range for raiders. -1 will use the distance defined in spigot.yml"
                )
            );

            miscVerticalActivationRange = getInt("entities.vertical-activation-range.misc", miscVerticalActivationRange);
            setComments("entities.vertical-activation-range.misc",
                List.of(
                    "Vertical activation range for misc entities. -1 will use the distance defined in spigot.yml"
                )
            );
        }

        public boolean useAsyncSpawningChunks = false;
        private void asyncSpawningChunks() {
            useAsyncSpawningChunks = getBoolean("spawning-chunks.use-async", useAsyncSpawningChunks);
            setComments("spawning-chunks.use-async",
                List.of(
                    "Makes the logic for ticking spawning chunks async. Note: When a mob is spawned, the task will be sent to the main thread."
                )
            );
        }

        public boolean playersDoNearbyEntityLookups = true;
        private void playersDoNearbyEntityLookups() {
            playersDoNearbyEntityLookups = getBoolean("entities.player.do-nearby-entity-lookups", playersDoNearbyEntityLookups);
            setComments("entities.player.do-nearby-entity-lookups",
                List.of(
                    "Makes the player no longer do nearby entity lookups.",
                    "This removes the ability for players to pick up / touch entities.",
                    "Likely only useful on limbo or hubs."
                )
            );
        }

        public boolean disablePlayerTracking = false;
        private void disablePlayerTracking() {
            disablePlayerTracking = getBoolean("misc.tracker.disable-player-tracking", disablePlayerTracking);
            setComments("misc.tracker.disable-player-tracking",
                List.of(
                    "Makes players no longer appear visually to other players.",
                    "More efficient this way than using a plugin to hide everyone.",
                    "Only useful on limbo or hubs."
                )
            );
        }

        public boolean disableAllSounds = false;
        private void disableAllSounds() {
            disableAllSounds = getBoolean("misc.networking.disable-all-sound-packets", disableAllSounds);
            setComments("misc.networking.disable-all-sound-packets",
                List.of(
                    "Prevents the server from sending sound packets.",
                    "",
                    "NOTE:",
                    "- Packets sent via packet api plugins might bypass this.",
                    "- Not all sounds are handled by the server. Break breaking for example.",
                    "",
                    "Only useful on limbo or hubs."
                )
            );
        }

        public boolean disableAllParticles = false;
        private void disableAllParticles() {
            disableAllParticles = getBoolean("misc.networking.disable-all-particle-packets", disableAllParticles);
            setComments("misc.networking.disable-all-sound-packets",
                List.of(
                    "Prevents the server from sending particle packets.",
                    "",
                    "NOTE:",
                    "- Packets sent via packet api plugins might bypass this.",
                    "- Not all particles are handled by the server. Break breaking for example.",
                    "",
                    "Only useful on limbo or hubs."
                )
            );
        }

        public boolean useOptimisedBlockEntityTicking = false;
        private void useOptimisedBlockEntityTicking() {
            useOptimisedBlockEntityTicking = getBoolean("blocks.global.optimised-block-entity-ticking", useOptimisedBlockEntityTicking);
            setComments("blocks.global.optimised-block-entity-ticking",
                List.of(
                    "Optimises ticking block entities by doing the following:",
                    "",
                    "- Caches if the whole chunk can be ticked instead of checking per block position.",
                    "- Delays heavier lookups when checking if a block position is tickable.",
                    "",
                    "Considering that most chunks are within the world border and chunks",
                    "that do have block entities tend to have more than one within the chunk.",
                    "",
                    "Note: Does fall back to cacheless version if the whole chunk isn't tickable."
                )
            );
        }

        public boolean useFasterNearbyPlayerDespawnCheck = false;
        private void useFasterNearbyPlayerDespawnCheck() {
            useFasterNearbyPlayerDespawnCheck = getBoolean("entities.global.faster-despawn-nearby-player-check", useFasterNearbyPlayerDespawnCheck);
            setComments("entities.global.faster-despawn-nearby-player-check",
                List.of(
                    "Optimises the nearby player lookup per entity by using",
                    "the simulation distance to find nearby players instead",
                    "of looping over every single player in the entity's world."
                )
            );
        }

        public int itemMergeWithNeighboursLimit = 100;
        private void itemEntityMergeWithNeighboursLimit() {
            itemMergeWithNeighboursLimit = getInt("entities.item.merge-with-neighbours-limit", itemMergeWithNeighboursLimit);
            setComments("entities.item.merge-with-neighbours-limit",
                List.of(
                    "Implements a limit for the merge with neighbours check.",
                    "This might make dealing with 1000s of dropped items in a single spot a bit easier."
                )
            );
        }
    }
}
