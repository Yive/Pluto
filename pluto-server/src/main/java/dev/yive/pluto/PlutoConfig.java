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

        public boolean useStaticGrowthSpeedStems = false;
        public float staticGrowthSpeedStems = 1.0F;
        public boolean useStaticGrowthSpeedCrops = false;
        public float staticGrowthSpeedCrops = 1.0F;
        public boolean useStaticGrowthSpeedPitchers = false;
        public float staticGrowthSpeedPitchers = 1.0F;
        private void shouldUseStaticGrowthSpeed() {
            useStaticGrowthSpeedStems = getBoolean("blocks.stems.static-growth-speed.enabled", useStaticGrowthSpeedStems);
            staticGrowthSpeedStems = (float) getDouble("blocks.stems.static-growth-speed.speed", staticGrowthSpeedStems);
            useStaticGrowthSpeedCrops = getBoolean("blocks.crops.static-growth-speed.enabled", useStaticGrowthSpeedCrops);
            staticGrowthSpeedCrops = (float) getDouble("blocks.crops.static-growth-speed.speed", staticGrowthSpeedCrops);
            useStaticGrowthSpeedPitchers = getBoolean("blocks.pitcher-plant.static-growth-speed.enabled", useStaticGrowthSpeedPitchers);
            staticGrowthSpeedPitchers = (float) getDouble("blocks.pitcher-plant.static-growth-speed.speed", staticGrowthSpeedPitchers);
        }

        public boolean alwaysMoistFarmland = false;
        public boolean alwaysMoistSugarCane = false;
        private void alwaysMoistBlocks() {
            alwaysMoistFarmland = getBoolean("blocks.farmland.always-moist", alwaysMoistFarmland);
            alwaysMoistSugarCane = getBoolean("blocks.sugarcane.always-moist", alwaysMoistSugarCane);
        }

        public boolean cactusCheckSurvivalBeforeGrowth = false;
        private void cactusCheckSurvivalBeforeGrowth() {
            cactusCheckSurvivalBeforeGrowth = getBoolean("blocks.cactus.check-survival-before-growth", cactusCheckSurvivalBeforeGrowth);
        }

        public boolean lessRandomDispensing = false;
        private void lessRandomDispensing() {
            lessRandomDispensing = getBoolean("blocks.dispenser.less-random-dispensing", lessRandomDispensing);
        }

        public boolean disableDropperInventoryMoveEvent = false;
        private void disableDropperInventoryMoveEvent() {
            disableDropperInventoryMoveEvent = getBoolean("blocks.dropper.disable-move-event", disableDropperInventoryMoveEvent);
        }

        public boolean disableAllayGameEventListening = false;
        public boolean disableAllayDuplication = false;
        private void allayConfiguration() {
            disableAllayGameEventListening = getBoolean("entities.allay.disable-game-event-listener", disableAllayGameEventListening);
            disableAllayDuplication = getBoolean("entities.allay.disable-duplication", disableAllayDuplication);
        }

        public boolean removeExcessMinecarts = false;
        public boolean removeExcessBoats = false;
        public int excessMinecartsLimit = 10;
        public int excessBoatsLimit = 10;

        private void removeExcessVehicles() {
            removeExcessMinecarts = getBoolean("entities.minecart.remove-excess.enabled", removeExcessMinecarts);
            removeExcessBoats = getBoolean("entities.boat.remove-excess.enabled", removeExcessBoats);
            excessMinecartsLimit = getInt("entities.minecart.remove-excess.limit", excessMinecartsLimit);
            excessBoatsLimit = getInt("entities.boat.remove-excess.limit", excessBoatsLimit);
        }

        public boolean disableBlockGenerationFromFluids = false;
        private void disableBlockGenerationFromFluids() {
            disableBlockGenerationFromFluids = getBoolean("blocks.fluids.disable-block-generation", disableBlockGenerationFromFluids);
        }

        public boolean disableNetherPortalGeneration = false;
        private void disableNetherPortalGeneration() {
            disableNetherPortalGeneration = getBoolean("blocks.fire.disable-nether-portal-generation", disableNetherPortalGeneration);
        }

        public int entityActivationInterval = 1;
        public boolean disableEntityActivation = false;
        private void entityActivationConfiguration() {
            entityActivationInterval = Math.min(1, getInt("entities.global.activation-interval", entityActivationInterval));
            disableEntityActivation = getBoolean("entities.global.disable-activation", disableEntityActivation);
        }

        public boolean disableItemMergeCheckWhileMoving = false;
        public int itemMergeMovingInterval = 2;
        public int itemMergeStaticInterval = 40;
        private void itemMergeConfiguration() {
            disableItemMergeCheckWhileMoving = getBoolean("entities.item.disable-merge-check-when-moving", disableItemMergeCheckWhileMoving);
            itemMergeMovingInterval = getInt("entities.item.merge-check-moving-interval", itemMergeMovingInterval);
            itemMergeStaticInterval = getInt("entities.item.merge-check-static-interval", itemMergeStaticInterval);
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
            mushroomIgnoreLightLevel = getBoolean("blocks.mushroom.ignore-light-level", mushroomIgnoreLightLevel);
            stemIgnoreLightLevel = getBoolean("blocks.stems.ignore-light-level", stemIgnoreLightLevel);
            bambooSaplingIgnoreLightLevel = getBoolean("blocks.bamboo-sapling.ignore-light-level", bambooSaplingIgnoreLightLevel);
            bambooStalkIgnoreLightLevel = getBoolean("blocks.bamboo-stalk.ignore-light-level", bambooStalkIgnoreLightLevel);
            sweetBerryBushIgnoreLightLevel = getBoolean("blocks.sweet-berry-bush.ignore-light-level", sweetBerryBushIgnoreLightLevel);
            saplingIgnoreLightLevel = getBoolean("blocks.sapling.ignore-light-level", saplingIgnoreLightLevel);
            pitcherCropIgnoreLightLevel = getBoolean("blocks.pitcher-plant.ignore-light-level", pitcherCropIgnoreLightLevel);
        }

        public boolean disableTargetSelector = false;
        public boolean disableGoalSelector = false;
        private void disableTargetSelector() {
            disableTargetSelector = getBoolean("entities.global.disable-target-selector", disableTargetSelector);
            disableGoalSelector = getBoolean("entities.global.disable-goal-selector", disableGoalSelector);
        }

        public boolean spawnerSettingsEnabled = false;
        public boolean spawnerCheckForNearbyPlayers = true;
        public boolean spawnerCheckForNearbyEntities = true;
        public boolean spawnerCheckForBlockCollision = true;
        public boolean spawnerDisableBabySpawns = false;
        public boolean spawnerDisableParticles = false;
        public int spawnerMinSpawnDelay = 200;
        public int spawnerMaxSpawnDelay = 800;
        public short spawnerSpawnCount = 4;
        public short spawnerMaxNearbyEntities = 6;
        public short spawnerRequiredPlayerRange = 16;
        public short spawnerSpawnRange = 4;
        private void spawnerConfiguration() {
            spawnerSettingsEnabled = getBoolean("blocks.spawner.enable-custom-settings", spawnerSettingsEnabled);
            spawnerCheckForNearbyPlayers = getBoolean("blocks.spawner.check-for-nearby-players", spawnerCheckForNearbyPlayers);
            spawnerCheckForNearbyEntities = getBoolean("blocks.spawner.check-for-nearby-entities", spawnerCheckForNearbyEntities);
            spawnerCheckForBlockCollision = getBoolean("blocks.spawner.check-for-block-collision", spawnerCheckForBlockCollision);
            spawnerDisableBabySpawns = getBoolean("blocks.spawner.disable-spawning-babies", spawnerDisableBabySpawns);
            spawnerDisableParticles = getBoolean("blocks.spawner.disable-spawn-particles", spawnerDisableParticles);
            spawnerMinSpawnDelay = getInt("blocks.spawner.min-spawn-delay", spawnerMinSpawnDelay);
            spawnerMaxSpawnDelay = getInt("blocks.spawner.max-spawn-delay", spawnerMaxSpawnDelay);
            spawnerSpawnCount = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.spawn-count", spawnerSpawnCount));
            spawnerMaxNearbyEntities = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.max-nearby-entities", spawnerMaxNearbyEntities));
            spawnerRequiredPlayerRange = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.required-player-range", spawnerRequiredPlayerRange));
            spawnerSpawnRange = (short) Math.min(Short.MAX_VALUE, getInt("blocks.spawner.spawn-range", spawnerSpawnRange));
        }

        public boolean disableTntChainReaction = false;
        private void disableTntChainReaction() {
            disableTntChainReaction = getBoolean("blocks.tnt.disable-chain-reaction", disableTntChainReaction);
        }

        public boolean disableFlapEvents = false;
        private void disableFlapEvents() {
            disableFlapEvents = getBoolean("entities.global.disable-flapping-game-event", false);
        }

        public int blockTickingModulo = 1;
        private void blockTicking() {
            blockTickingModulo = getInt("blocks.global.block-ticking-modulo", blockTickingModulo);
        }

        public int fluidTickingModulo = 1;
        private void fluidTicking() {
            fluidTickingModulo = getInt("blocks.fluids.fluid-ticking-modulo", fluidTickingModulo);
        }

        public int raidTickingModulo = 1;
        private void raidTicking() {
            raidTickingModulo = getInt("misc.raids.raid-ticking-modulo", raidTickingModulo);
        }

        public int blockEventTickingModulo = 1;
        private void blockEventTicking() {
            // TODO: Allow for disabling block event ticking
            blockEventTickingModulo = Math.min(1, getInt("blocks.global.block-event-ticking-modulo", blockEventTickingModulo));
        }

        public int entityDespawnCheckModulo = 1;
        private void entityDespawning() {
            entityDespawnCheckModulo = getInt("entities.global.despawn-check-modulo", entityDespawnCheckModulo);
        }

        public boolean disableShulkersDroppingContentsWhenDestroyed = false;
        public boolean disableShulkerSplitting = false;
        public boolean disableShulkerTeleporting = false;
        private void shulkerSettings() {
            disableShulkersDroppingContentsWhenDestroyed = getBoolean("entities.item.disable-dropping-shulker-box-contents-when-destroyed", disableShulkersDroppingContentsWhenDestroyed);
            disableShulkerSplitting = getBoolean("entities.shulker.disable-splitting-from-bullets", disableShulkerSplitting);
            disableShulkerTeleporting = getBoolean("entities.shulker.disable-random-teleports", disableShulkerTeleporting);
        }

        public int entityPushingModulo = 1;
        private void entityPushing() {
            entityPushingModulo = getInt("entities.global.pushing-modulo", entityPushingModulo);
        }

        public int entityInsideBlockCheck = 1;
        private void entityInsideBlockCheck() {
            entityInsideBlockCheck = getInt("entities.global.inside-block-check-modulo", entityInsideBlockCheck);
        }

        public int entityRainCheckRate = 10;
        private void entityRainCheckModulo() {
            entityRainCheckRate = Math.min(1, getInt("entities.global.rain-check-tick-rate", entityRainCheckRate));
        }

        public boolean preventInsideBlockXrayExploit = false;
        private void disableInsideBlockXrayExploit() {
            preventInsideBlockXrayExploit = getBoolean("entities.player.prevent-xray-exploits-inside-certain-blocks", preventInsideBlockXrayExploit);
        }

        private void spawnerTTL() {
            // Set a few default ones to create the section
            net.minecraft.world.entity.EntityType.ZOMBIE.spawnerTTL = getInt("blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(net.minecraft.world.entity.EntityType.ZOMBIE).getPath().toLowerCase(java.util.Locale.ROOT), -1);
            net.minecraft.world.entity.EntityType.SNOW_GOLEM.spawnerTTL = getInt("blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(net.minecraft.world.entity.EntityType.SNOW_GOLEM).getPath().toLowerCase(java.util.Locale.ROOT), -1);

            // These bypass setting the default entries to avoid a stupidly long section.
            for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                String path = "blocks.spawner.ttl." + net.minecraft.world.entity.EntityType.getKey(type).getPath().toLowerCase(java.util.Locale.ROOT);
                type.spawnerTTL = this.config.getInt(path, this.worldDefaults.getInt(path, -1));
            }
        }

        public boolean skipFailedDispenseLevelEvent = false;
        private void skipFailedDispenseLevelEvent() {
            skipFailedDispenseLevelEvent = getBoolean("blocks.global.skip-failed-block-dispense-level-event", skipFailedDispenseLevelEvent);
        }

        public boolean useFluidPushingOptimisation = true;
        private void useFluidPushingOptimisation() {
            useFluidPushingOptimisation = getBoolean("entities.global.use-fluid-pushing-optimisation", useFluidPushingOptimisation);
        }

        public boolean onlyPlayersPushEntities = false;
        private void onlyPlayersPushEntities() {
            onlyPlayersPushEntities = getBoolean("entities.player.only-players-push-entities", onlyPlayersPushEntities);
        }
    }
}