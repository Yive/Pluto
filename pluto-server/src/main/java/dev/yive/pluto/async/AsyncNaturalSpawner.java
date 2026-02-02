package dev.yive.pluto.async;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jspecify.annotations.Nullable;

// TODO: Look into moving this into real time based instead of tick based.
public class AsyncNaturalSpawner {
    public static void spawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, List<MobCategory> spawningCategories) {
        for (MobCategory mobCategory : spawningCategories) {
            int maxSpawns;
            if (level.paperConfig().entities.spawning.perPlayerMobSpawns) {
                int limit = mobCategory.getMaxInstancesPerChunk();
                SpawnCategory spawnCategory = CraftSpawnCategory.toBukkit(mobCategory);
                if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                    limit = level.getWorld().getSpawnLimit(spawnCategory);
                }

                int minDiff = Integer.MAX_VALUE;
                final ReferenceList<ServerPlayer> inRange =
                    level.moonrise$getNearbyPlayers().getPlayers(
                        chunk.getPos(),
                        NearbyPlayers.NearbyMapType.TICK_VIEW_DISTANCE
                    );
                if (inRange != null) {
                    final ServerPlayer[] backingSet = inRange.getRawDataUnchecked();
                    for (int index = 0, len = inRange.size(); index < len; index++) {
                        try {
                            final ServerPlayer player = backingSet[index];
                            if (player == null) continue;

                            minDiff = Math.min(limit - level.getChunkSource().chunkMap.getMobCountNear(player, mobCategory), minDiff);
                        } catch (Exception unused) {
                            // likely out-of-bounds
                            break;
                        }
                    }
                }

                maxSpawns = (minDiff == Integer.MAX_VALUE) ? 0 : minDiff;
            } else {
                maxSpawns = state.canSpawnForCategoryLocal(mobCategory, chunk.getPos()) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            if (maxSpawns <= 0) continue;

            spawnCategoryForChunk(mobCategory, level, chunk, state::canSpawn, state::afterSpawn, maxSpawns, level.paperConfig().entities.spawning.perPlayerMobSpawns ? level.getChunkSource().chunkMap::updatePlayerMobTypeMap : null);
        }
    }

    private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) {
        final ChunkPos pos = chunk.getPos();
        final int x = pos.getMinBlockX() + level.threadSafeRandom.nextInt(16);
        final int z = pos.getMinBlockZ() + level.threadSafeRandom.nextInt(16);
        final int topEmptyY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        final int y = Mth.randomBetweenInclusive(level.threadSafeRandom, level.getMinY(), topEmptyY);
        return new BlockPos(x, y, z);
    }

    private static void spawnCategoryForChunk(MobCategory mobCategory, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate extraTest, NaturalSpawner.AfterSpawnCallback spawnCallback, final int maxSpawns, final Consumer<Entity> trackEntity) {
        final BlockPos start = getRandomPosWithin(level, chunk);
        if (start.getY() < level.getMinY() + 1) return;
        spawnCategoryForPosition(mobCategory, level, chunk, start, extraTest, spawnCallback, maxSpawns, trackEntity);
    }

    private static void spawnCategoryForPosition(
        MobCategory mobCategory,
        ServerLevel level,
        ChunkAccess chunk,
        BlockPos start,
        NaturalSpawner.SpawnPredicate extraTest,
        NaturalSpawner.AfterSpawnCallback spawnCallback,
        final int maxSpawns,
        final @Nullable Consumer<Entity> trackEntity
    ) {
        // Paper end - Optional per player mob spawns
        StructureManager structureManager = level.structureManager();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        int yStart = start.getY();
        BlockState state = level.getBlockStateIfLoadedAndInBounds(start); // Paper - don't load chunks for mob spawn
        // TODO: Find out if this can be ran async without any problems.
        if (state == null || !AsyncUtils.completeOnMain(() -> state.isRedstoneConductor(chunk, start)).join()) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int clusterSize = 0;

        for (int groupCount = 0; groupCount < 3; groupCount++) {
            int x = start.getX();
            int z = start.getZ();
            MobSpawnSettings.SpawnerData currentSpawnData = null;
            SpawnGroupData groupData = null;
            int max = Mth.ceil(level.threadSafeRandom.nextFloat() * 4.0F);
            int groupSize = 0;

            for (int ll = 0; ll < max; ll++) {
                x += level.threadSafeRandom.nextInt(6) - level.threadSafeRandom.nextInt(6);
                z += level.threadSafeRandom.nextInt(6) - level.threadSafeRandom.nextInt(6);
                pos.set(x, yStart, z);
                double xx = x + 0.5;
                double zz = z + 0.5;
                Player nearestPlayer = getNearestPlayer(level, xx, yStart, zz, -1.0, EntitySelector.NO_SPECTATORS);
                if (nearestPlayer != null) {
                    double nearestPlayerDistanceSqr = nearestPlayer.distanceToSqr(xx, yStart, zz);
                    if (level.isLoadedAndInBounds(pos) && NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint(level, chunk, pos, nearestPlayerDistanceSqr)) { // Paper - don't load chunks for mob spawn
                        if (currentSpawnData == null) {
                            Optional<MobSpawnSettings.SpawnerData> nextSpawnData = NaturalSpawner.getRandomSpawnMobAt(
                                level, structureManager, generator, mobCategory, level.threadSafeRandom, pos
                            );
                            if (nextSpawnData.isEmpty()) {
                                break;
                            }

                            currentSpawnData = nextSpawnData.get();
                            max = currentSpawnData.minCount() + level.threadSafeRandom.nextInt(1 + currentSpawnData.maxCount() - currentSpawnData.minCount());
                        }

                        // Paper start - PreCreatureSpawnEvent
                        NaturalSpawner.PreSpawnStatus doSpawning = isValidSpawnPositionForType(level, mobCategory, structureManager, generator, currentSpawnData, pos, nearestPlayerDistanceSqr);
                        // Paper start - per player mob count backoff
                        if (doSpawning == NaturalSpawner.PreSpawnStatus.ABORT || doSpawning == NaturalSpawner.PreSpawnStatus.CANCELLED) {
                            level.getChunkSource().chunkMap.updateFailurePlayerMobTypeMap(pos.getX() >> 4, pos.getZ() >> 4, mobCategory);
                        }
                        // Paper end - per player mob count backoff
                        if (doSpawning == NaturalSpawner.PreSpawnStatus.ABORT) {
                            return;
                        }
                        if (doSpawning != NaturalSpawner.PreSpawnStatus.SUCCESS || !extraTest.test(currentSpawnData.type(), pos, chunk)) continue;
                        Mob mob = NaturalSpawner.getMobForSpawn(level, currentSpawnData.type());
                        if (mob == null) {
                            return;
                        }

                        mob.snapTo(xx, yStart, zz, level.threadSafeRandom.nextFloat() * 360.0F, 0.0F);
                        if (NaturalSpawner.isValidPositionForMob(level, mob, nearestPlayerDistanceSqr)) {
                            final SpawnGroupData finalGroupData = groupData;
                            final CreatureSpawnEvent.SpawnReason spawnReason =
                                (mob instanceof Ocelot
                                    && !((Ageable) mob.getBukkitEntity()).isAdult())
                                    ? CreatureSpawnEvent.SpawnReason.OCELOT_BABY
                                    : CreatureSpawnEvent.SpawnReason.NATURAL;
                            groupData = AsyncUtils.completeOnMain(() -> {
                                final SpawnGroupData spawn = mob.finalizeSpawn(
                                    level, level.getCurrentDifficultyAt(mob.blockPosition()),
                                    EntitySpawnReason.NATURAL,
                                    finalGroupData
                                );
                                level.addFreshEntityWithPassengers(mob,
                                    spawnReason
                                );
                                return spawn;
                            }).join();
                            if (!mob.isRemoved()) {
                                ++clusterSize;
                                ++groupSize;
                                spawnCallback.run(mob, chunk);
                                // Paper start - Optional per player mob spawns
                                if (trackEntity != null) {
                                    trackEntity.accept(mob);
                                }
                                // Paper end - Optional per player mob spawns
                            }
                            // CraftBukkit end
                            if (clusterSize >= mob.getMaxSpawnClusterSize() || clusterSize >= maxSpawns) { // Paper - Optional per player mob spawns
                                return;
                            }

                            if (mob.isMaxGroupSizeReached(groupSize)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static @Nullable Player getNearestPlayer(ServerLevel level, double x, double y, double z, double distance, @Nullable Predicate<Entity> predicate) {
        double best = -1.0;
        Player result = null;

        final ReferenceList<ServerPlayer> players = level.moonrise$getNearbyPlayers().getPlayers(
            BlockPos.containing(x, y, z),
            NearbyPlayers.NearbyMapType.TICK_VIEW_DISTANCE
        );
        if (players == null) return null;

        // This isn't safe, but it should be fine when inside a try-catch statement.
        final ServerPlayer[] array = players.getRawDataUnchecked();
        for (int index = 0; index < array.length; index++) {
            try {
                final ServerPlayer player = array[index];
                if (player == null || predicate != null && !predicate.test(player)) continue;

                double dist = player.distanceToSqr(x, y, z);
                if ((!(distance < 0.0) && !(dist < distance * distance)) || (best != -1.0 && !(dist < best))) continue;

                best = dist;
                result = player;
            } catch (Exception unused) {
                // Likely out-of-bounds error.
                break;
            }
        }

        return result;
    }

    private static NaturalSpawner.PreSpawnStatus isValidSpawnPositionForType(
        ServerLevel level,
        MobCategory mobCategory,
        StructureManager structureManager,
        ChunkGenerator generator,
        MobSpawnSettings.SpawnerData currentSpawnData,
        BlockPos.MutableBlockPos pos,
        double nearestPlayerDistanceSqr
    ) {
        EntityType<?> type = currentSpawnData.type();
        // Most plugins nowadays probably wouldn't shit the bed if this was async, but I'm sure some will.
        final NaturalSpawner.PreSpawnStatus eventResult = PreCreatureSpawnEvent.getHandlerList().getRegisteredListeners().length > 1 ? AsyncUtils.completeOnMain(() -> {
            PreCreatureSpawnEvent event = new PreCreatureSpawnEvent(
                CraftLocation.toBukkit(pos, level),
                CraftEntityType.minecraftToBukkit(type),
                CreatureSpawnEvent.SpawnReason.NATURAL
            );
            if (!event.callEvent()) {
                if (event.shouldAbortSpawn()) {
                    return NaturalSpawner.PreSpawnStatus.ABORT;
                }
                return NaturalSpawner.PreSpawnStatus.CANCELLED;
            }
            return null;
        }).join() : null;
        if (eventResult != null)
            return eventResult;
        final boolean success = type.getCategory() != MobCategory.MISC
            && (type.canSpawnFarFromPlayer() || !(nearestPlayerDistanceSqr > type.getCategory().getDespawnDistance() * type.getCategory().getDespawnDistance()))
            && type.canSummon()
            && NaturalSpawner.canSpawnMobAt(level, structureManager, generator, mobCategory, currentSpawnData, pos)
            && SpawnPlacements.isSpawnPositionOk(type, level, pos)
            && SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, level.threadSafeRandom)
            && level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        return success ? NaturalSpawner.PreSpawnStatus.SUCCESS : NaturalSpawner.PreSpawnStatus.FAIL;
    }
}
