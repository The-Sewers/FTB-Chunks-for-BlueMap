package cc.sewers.extended.extended.bluemap;

import cc.sewers.extended.extended.ftbchunks.FTBChunksUtil;
import com.flowpowered.math.vector.Vector2i;
import cc.sewers.extended.util.Cheese;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlueMapUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARKER = "ftbchunks.claims";
    private static final Map<String, Map<UUID, Set<String>>> activeRegions = new ConcurrentHashMap<>();

    public static void init(MinecraftServer server) {
        activeRegions.clear();
        BlueMapAPI.onEnable(api -> {
            for (ServerLevel level : server.getAllLevels()) {
                ResourceKey<Level> dimension = level.dimension();
                String dimensionId = dimension.location().toString();
                api.getWorld(level).ifPresent(world -> world.getMaps().forEach(map -> {
                    MarkerSet markerSet = map.getMarkerSets().get(MARKER);
                    if (markerSet == null) {
                        markerSet = MarkerSet.builder().label("Claims (2D)").build();
                        map.getMarkerSets().put(MARKER, markerSet);
                    }
                    markerSet.getMarkers().clear();
                }));
                activeRegions.put(dimensionId, new ConcurrentHashMap<>());
                updateDimensionClaims(level);
            }
            LOGGER.info("SewersExtended initialized successfully.");
        });
    }

    public static void updateDimensionClaims(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        String dimensionId = dimension.location().toString();
        LOGGER.debug("Updating claims for dimension: {}", dimensionId);
        Map<UUID, List<ClaimedChunk>> claimStorage = FTBChunksUtil.getClaimedChunks(dimension);
        Map<UUID, Set<String>> dimensionRegions = activeRegions.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
        claimStorage.forEach((teamId, claims) -> updateTeamClaims(dimension, teamId, claims, dimensionRegions.getOrDefault(teamId, new HashSet<>())));
        Set<UUID> currentTeams = claimStorage.keySet();
        Set<UUID> storedTeams = new HashSet<>(dimensionRegions.keySet());
        storedTeams.forEach(teamId -> {
            if (!currentTeams.contains(teamId)) {
                removeAllTeamMarkers(dimension, teamId);
                dimensionRegions.remove(teamId);
            }
        });
    }

    public static void updateTeamClaims(ResourceKey<Level> dimension, UUID teamId, List<ClaimedChunk> claims, Set<String> _oldRegionIds) {
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
        List<ClaimRegion> regions = buildRegions(claims, teamId, dimension);
        Set<String> newRegionIds = new HashSet<>();
        for (ClaimRegion region : regions) {
            displayRegion(region);
            newRegionIds.add(region.getMarkerId());
        }
        Map<UUID, Set<String>> dimensionRegions = activeRegions.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
        dimensionRegions.put(teamId, newRegionIds);
        LOGGER.debug("Updated {} regions for team {} in dimension {}", regions.size(), teamId, dimensionId);
    }

    private static List<ClaimRegion> buildRegions(List<ClaimedChunk> claims, UUID teamId, ResourceKey<Level> dimension) {
        if (claims.isEmpty()) return Collections.emptyList();
        Team team = claims.getFirst().getTeamData().getTeam();
        String teamName = team.getShortName().split("#")[0];
        int teamColor = FTBChunksUtil.getTeamColor(team);
        Set<ChunkDimPos> unprocessed = new HashSet<>();
        claims.forEach(claim -> unprocessed.add(claim.getPos()));
        List<ClaimRegion> connectedRegions = new ArrayList<>();
        String dimensionId = dimension.location().toString();
        while (!unprocessed.isEmpty()) {
            ClaimRegion region = new ClaimRegion(teamId, dimensionId, teamName, teamColor);
            ChunkDimPos seed = unprocessed.iterator().next();
            unprocessed.remove(seed);
            Queue<ChunkDimPos> queue = new LinkedList<>();
            queue.add(seed);
            region.addChunk(seed);
            while (!queue.isEmpty()) {
                ChunkDimPos current = queue.poll();
                checkAndAddNeighbor(current, 1, 0, unprocessed, queue, region);
                checkAndAddNeighbor(current, -1, 0, unprocessed, queue, region);
                checkAndAddNeighbor(current, 0, 1, unprocessed, queue, region);
                checkAndAddNeighbor(current, 0, -1, unprocessed, queue, region);
            }
            connectedRegions.add(region);
        }
        List<ClaimRegion> finalRegions = new ArrayList<>(connectedRegions);
        LOGGER.debug("Built {} final rectangular regions from {} original connected regions", finalRegions.size(), connectedRegions.size());
        return finalRegions;
    }

    private static void checkAndAddNeighbor(ChunkDimPos pos, int dx, int dz, Set<ChunkDimPos> unprocessed, Queue<ChunkDimPos> queue, ClaimRegion region) {
        ChunkDimPos neighbor = new ChunkDimPos(pos.dimension(), pos.x() + dx, pos.z() + dz);
        if (unprocessed.remove(neighbor)) {
            queue.add(neighbor);
            region.addChunk(neighbor);
        }
    }

    private static void displayRegion(ClaimRegion region) {
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(region.getDimension())).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                MarkerSet markerSet = map.getMarkerSets().get(MARKER);
                if (markerSet == null) {
                    LOGGER.error("Marker sets not found for map: {}", map.getId());
                    continue;
                }

                Vector2i[] chunkCoordinates = region.getChunkCoordinates();
                Cheese cheese = Cheese.createSingleFromChunks(chunkCoordinates);
                Color fillColor = new Color(region.getTeamColor(), 0.5f);
                Color lineColor = new Color(region.getTeamColor(), 1.0f);

                ShapeMarker chunkMarker = new ShapeMarker.Builder()
                        .label(region.getLabel())
                        .shape(cheese.getShape(), 80)
                        .holes(cheese.getHoles().toArray(Shape[]::new))
                        .fillColor(fillColor)
                        .lineColor(lineColor)
                        .build();
                markerSet.put(region.getMarkerId(), chunkMarker);
            }
        });
    }

    public static void removeAllTeamMarkers(ResourceKey<Level> dimension, UUID teamId) {
        String dimensionId = dimension.location().toString();
        Map<UUID, Set<String>> dimensionRegions = activeRegions.get(dimensionId);
        Set<String> regionIds = dimensionRegions != null ? dimensionRegions.getOrDefault(teamId, Collections.emptySet()) : Collections.emptySet();
        LOGGER.debug("Removing {} markers for team {} in dimension {}", regionIds.size(), teamId, dimensionId);
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(dimension)).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                MarkerSet markerSet2D = map.getMarkerSets().get(MARKER);
                if (markerSet2D != null) {
                    for (String markerId : regionIds) {
                        markerSet2D.remove(markerId);
                    }
                }
            }
        });
    }
}
