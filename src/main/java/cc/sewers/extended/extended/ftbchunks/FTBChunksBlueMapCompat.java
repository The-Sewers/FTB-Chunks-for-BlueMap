package cc.sewers.extended.extended.ftbchunks;

import cc.sewers.extended.Config;
import cc.sewers.extended.util.Cheese;
import com.flowpowered.math.vector.Vector2i;
import com.mojang.logging.LogUtils;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FTBChunksBlueMapCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARKER = "ftbchunks.claims";
    private static final Map<String, Map<UUID, Set<String>>> activeRegions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> future;
    private static MinecraftServer server;

    public static void init(MinecraftServer minecraftServer) {
        LOGGER.info("Initializing FTB Chunks BlueMap compatibility...");

        if (Config.bluemapFtbChunksEnabled) {
            server = minecraftServer;
            activeRegions.clear();
            ClaimRegion.resetRegionCounters();
            start();
        } else {
            LOGGER.info("FTB Chunks BlueMap compatibility is disabled in the config.");
        }
    }

    public static void start() {
        Runnable task = () -> {
            Optional<BlueMapAPI> apiOptional = BlueMapAPI.getInstance();
            apiOptional.ifPresent(api -> {
                try {
                    if (server != null) {
                        for (ServerLevel level : server.getAllLevels()) {
                            ResourceKey<Level> dimension = level.dimension();
                            String dimensionId = dimension.location().toString();
                            api.getWorld(level).ifPresent(world -> world.getMaps().forEach(map -> {
                                MarkerSet markerSet = map.getMarkerSets().get(MARKER);
                                if (markerSet == null) {
                                    markerSet = MarkerSet.builder().label("Claims").build();
                                    map.getMarkerSets().put(MARKER, markerSet);
                                }
                            }));
                            activeRegions.put(dimensionId, new ConcurrentHashMap<>());
                            updateDimensionClaims(level);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error updating FTB Chunks claims: {}", e.getMessage());
                }
            });
        };

        // Schedule the task to run periodically
        future = scheduler.scheduleAtFixedRate(task, 0, Config.bluemapChunkAutoUpdateMs, TimeUnit.MILLISECONDS);
        LOGGER.info("FTBChunksBlueMap initialized successfully.");
    }

    public static void stop() {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            LOGGER.info("Stopped FTB Chunks BlueMap watcher.");
        }
    }

    public static void updateDimensionClaims(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        String dimensionId = dimension.location().toString();
        LOGGER.debug("Updating claims for dimension: {}", dimensionId);
        Map<UUID, List<ClaimedChunk>> claimStorage = FTBChunksUtil.getClaimedChunks(dimension);
        Map<UUID, Set<String>> dimensionRegions = activeRegions.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());

        claimStorage.forEach((teamId, claims) ->
                updateTeamClaims(dimension, teamId, claims)
        );

        Set<UUID> currentTeams = claimStorage.keySet();
        Set<UUID> storedTeams = new HashSet<>(dimensionRegions.keySet());
        storedTeams.forEach(teamId -> {
            if (!currentTeams.contains(teamId)) {
                removeAllTeamMarkers(dimension, teamId);
                dimensionRegions.remove(teamId);
            }
        });
    }

    public static void updateTeamClaims(ResourceKey<Level> dimension, UUID teamId, List<ClaimedChunk> claims) {
        String dimensionId = dimension.location().toString();
        LOGGER.debug("Updating team {} claims in dimension {}", teamId, dimensionId);
        removeAllTeamMarkers(dimension, teamId);
        if (claims.isEmpty()) {
            Map<UUID, Set<String>> dimensionRegions = activeRegions.get(dimensionId);
            if (dimensionRegions != null) {
                dimensionRegions.remove(teamId);
            }
            return;
        }

        List<ClaimRegion> regions = RegionManager.groupChunksIntoRegions(claims, teamId, dimension);
        Set<String> newRegionIds = new HashSet<>();

        for (ClaimRegion region : regions) {
            displayRegion(region);
            newRegionIds.add(region.getMarkerId());
        }

        Map<UUID, Set<String>> dimensionRegions = activeRegions.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
        dimensionRegions.put(teamId, newRegionIds);
        LOGGER.debug("Updated {} regions for team {} in dimension {}", regions.size(), teamId, dimensionId);
    }

    public static void updateTeamClaims(ResourceKey<Level> dimension, UUID teamId) {
        List<ClaimedChunk> claims = FTBChunksUtil.getClaimedChunksForTeam(dimension, teamId);
        updateTeamClaims(dimension, teamId, claims);
    }

    private static void displayRegion(ClaimRegion region) {
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(region.getDimension())).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                MarkerSet markerSet = map.getMarkerSets().get(MARKER);
                if (markerSet == null) {
                    LOGGER.error("3D Marker sets not found for map: {}", map.getId());
                    continue;
                }

                Vector2i[] chunkCoordinates = region.getChunkCoordinates();
                Cheese cheese = Cheese.createSingleFromChunks(chunkCoordinates);
                Color fillColor = new Color(region.getTeamColor(), 0.2f);
                Color lineColor = new Color(region.getTeamColor(), 0.6f);

                ExtrudeMarker extrudeMarker = new ExtrudeMarker.Builder()
                        .label(region.getLabel())
                        .detail(region.getDetail())
                        .shape(cheese.getShape(), -64, 320)
                        .holes(cheese.getHoles().toArray(Shape[]::new))
                        .fillColor(fillColor)
                        .lineColor(lineColor)
                        .build();

                markerSet.put(region.getMarkerId(), extrudeMarker);
            }
        });
    }

    public static void removeAllTeamMarkers(ResourceKey<Level> dimension, UUID teamId) {
        String dimensionId = dimension.location().toString();
        Map<UUID, Set<String>> dimensionRegions = activeRegions.get(dimensionId);

        Set<String> regionIds = dimensionRegions != null ? dimensionRegions.getOrDefault(teamId, Collections.emptySet()) : Collections.emptySet();

        LOGGER.debug("Removing {} 2D and {} 3D markers for team {} in dimension {}",
                regionIds.size(), regionIds.size(), teamId, dimensionId);

        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(dimension)).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                MarkerSet markerSet = map.getMarkerSets().get(MARKER);
                if (markerSet != null) {
                    for (String markerId : regionIds) {
                        markerSet.remove(markerId);
                    }
                }
            }
        });
    }
}
