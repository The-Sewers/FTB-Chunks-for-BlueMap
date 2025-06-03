package cc.sewers.extended.extended.ftbchunks;

import java.util.*;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class RegionManager {
    private static final Map<String, Map<UUID, List<ClaimRegion>>> regionCache = new HashMap<>();

    public static List<ClaimRegion> groupChunksIntoRegions(List<ClaimedChunk> chunks, UUID teamId, ResourceKey<Level> dimension) {
        if (chunks.isEmpty()) return Collections.emptyList();
        Team team = chunks.getFirst().getTeamData().getTeam();
        String teamName = team.getShortName().split("#")[0];
        int teamColor = FTBChunksUtil.getTeamColor(team);
        Set<ChunkDimPos> unprocessed = new HashSet<>();
        chunks.forEach(chunk -> unprocessed.add(chunk.getPos()));
        List<ClaimRegion> regions = new ArrayList<>();
        String dimensionId = dimension.location().toString();
        while (!unprocessed.isEmpty()) {
            ClaimRegion region = new ClaimRegion(team, dimensionId, teamName, teamColor);
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
            regions.add(region);
        }
        return regions;
    }

    private static void checkAndAddNeighbor(ChunkDimPos pos, int dx, int dz, Set<ChunkDimPos> unprocessed, Queue<ChunkDimPos> queue, ClaimRegion region) {
        ChunkDimPos neighbor = new ChunkDimPos(pos.dimension(), pos.x() + dx, pos.z() + dz);
        if (unprocessed.remove(neighbor)) {
            queue.add(neighbor);
            region.addChunk(neighbor);
        }
    }
}
