package cc.sewers.extended.extended.ftbchunks;

import java.util.*;
import java.util.stream.Collectors;

import cc.sewers.extended.extended.bluemap.BlueMapUtil;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ChunkClaimCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ResourceLocation, Map<UUID, Set<ChunkDimPos>>> claimCache = new HashMap<>();
    private final Map<UUID, TeamData> teamDataCache = new HashMap<>();

    private static class TeamData {
        final String teamName;
        final int teamColor;
        TeamData(String teamName, int teamColor) {
            this.teamName = teamName;
            this.teamColor = teamColor;
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TeamData other)) return false;
            return this.teamColor == other.teamColor && Objects.equals(this.teamName, other.teamName);
        }
        @Override
        public int hashCode() {
            return Objects.hash(teamName, teamColor);
        }
    }

    public void updateAndProcessChanges(MinecraftServer server) {
        LOGGER.debug("Checking for claim changes...");
        Set<ServerLevel> changedDimensions = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimension = level.dimension();
            ResourceLocation dimensionId = dimension.location();
            boolean dimensionChanged = false;

            Map<UUID, List<ClaimedChunk>> currentClaims = FTBChunksUtil.getClaimedChunks(dimension);

            Map<UUID, Set<ChunkDimPos>> currentClaimPositions = new HashMap<>();
            Map<UUID, TeamData> currentTeamData = new HashMap<>();
            currentClaims.forEach((teamId, chunks) -> {
                Set<ChunkDimPos> positions = chunks.stream().map(ClaimedChunk::getPos).collect(Collectors.toSet());
                currentClaimPositions.put(teamId, positions);
                if (!chunks.isEmpty()) {
                    ClaimedChunk firstChunk = chunks.getFirst();
                    Team team = firstChunk.getTeamData().getTeam();
                    String teamName = team.getShortName().split("#")[0];
                    int teamColor = FTBChunksUtil.getTeamColor(team);
                    currentTeamData.put(teamId, new TeamData(teamName, teamColor));
                }
            });

            Map<UUID, Set<ChunkDimPos>> cachedClaimsForDimension = claimCache.getOrDefault(dimensionId, new HashMap<>());

            Set<UUID> changedTeams = findChangedTeams(currentClaimPositions, cachedClaimsForDimension, currentTeamData);

            if (!changedTeams.isEmpty()) {
                LOGGER.info("Found {} teams with claim or data changes in dimension {}", changedTeams.size(), dimensionId);
                dimensionChanged = true;
                for (UUID teamId : changedTeams) {
                    List<ClaimedChunk> teamClaims = currentClaims.getOrDefault(teamId, Collections.emptyList());
                    updateTeamMap(dimension, teamId, teamClaims);
                }
            } else {
                LOGGER.debug("No changes found for dimension {}", dimensionId);
            }

            claimCache.put(dimensionId, new HashMap<>(currentClaimPositions));

            teamDataCache.putAll(currentTeamData);

            if (dimensionChanged) {
                changedDimensions.add(level);
            }
        }

        if (!changedDimensions.isEmpty()) {
            LOGGER.info("Updated claims in {} dimensions", changedDimensions.size());
        }
    }

    private Set<UUID> findChangedTeams(Map<UUID, Set<ChunkDimPos>> currentClaims, Map<UUID, Set<ChunkDimPos>> cachedClaims, Map<UUID, TeamData> currentTeamData) {
        Set<UUID> changedTeams = new HashSet<>();

        for (UUID teamId : currentClaims.keySet()) {
            Set<ChunkDimPos> teamCurrentClaims = currentClaims.get(teamId);
            Set<ChunkDimPos> teamCachedClaims = cachedClaims.getOrDefault(teamId, new HashSet<>());

            if (teamCurrentClaims.size() != teamCachedClaims.size() ||
                    !teamCurrentClaims.containsAll(teamCachedClaims) ||
                    !teamCachedClaims.containsAll(teamCurrentClaims)) {
                LOGGER.debug("Team {} has claim changes: current={}, cached={}", teamId, teamCurrentClaims.size(), teamCachedClaims.size());
                changedTeams.add(teamId);
                continue;
            }

            TeamData currentData = currentTeamData.get(teamId);
            TeamData cachedData = teamDataCache.get(teamId);
            if (currentData != null && (!currentData.equals(cachedData))) {
                LOGGER.debug("Team data changed for team {}", teamId);
                changedTeams.add(teamId);
            }
        }

        for (UUID teamId : cachedClaims.keySet()) {
            if (!currentClaims.containsKey(teamId)) {
                LOGGER.debug("Team {} was removed from claims", teamId);
                changedTeams.add(teamId);
            }
        }

        return changedTeams;
    }

    public void addClaim(ClaimedChunk chunk) {
        LOGGER.debug("Adding claim for chunk at {} in dimension {}", chunk.getPos(), chunk.getPos().dimension().location());
        ResourceLocation dimensionId = chunk.getPos().dimension().location();
        UUID teamId = chunk.getTeamData().getTeam().getTeamId();

        claimCache.computeIfAbsent(dimensionId, k -> new HashMap<>())
                .computeIfAbsent(teamId, k -> new HashSet<>())
                .add(chunk.getPos());

        Team team = chunk.getTeamData().getTeam();
        String teamName = team.getShortName().split("#")[0];
        int teamColor = FTBChunksUtil.getTeamColor(team);
        teamDataCache.put(teamId, new TeamData(teamName, teamColor));

        try {
            ResourceKey<Level> dimension = chunk.getPos().dimension();
            List<ClaimedChunk> teamClaims = FTBChunksUtil.getClaimedChunks(dimension).getOrDefault(teamId, Collections.emptyList());
            updateTeamMap(dimension, teamId, teamClaims);
        } catch (Exception e) {
            LOGGER.debug("Could not immediately update team map, will be updated during next periodic check");
        }
    }

    public void removeClaim(ClaimedChunk chunk) {
        LOGGER.debug("Removing claim for chunk at {} in dimension {}", chunk.getPos(), chunk.getPos().dimension().location());
        ResourceLocation dimensionId = chunk.getPos().dimension().location();
        UUID teamId = chunk.getTeamData().getTeam().getTeamId();

        Map<UUID, Set<ChunkDimPos>> teamClaims = claimCache.get(dimensionId);
        if (teamClaims != null) {
            Set<ChunkDimPos> claims = teamClaims.get(teamId);
            if (claims != null) {
                claims.remove(chunk.getPos());
                if (claims.isEmpty()) {
                    teamClaims.remove(teamId);
                }
            }
        }

        try {
            ResourceKey<Level> dimension = chunk.getPos().dimension();
            List<ClaimedChunk> remainingClaims = FTBChunksUtil.getClaimedChunks(dimension).getOrDefault(teamId, Collections.emptyList());
            updateTeamMap(dimension, teamId, remainingClaims);
        } catch (Exception e) {
            LOGGER.debug("Could not immediately update team map, will be updated during next periodic check");
        }
    }

    public void clear() {
        claimCache.clear();
        teamDataCache.clear();
    }

    private void updateTeamMap(ResourceKey<Level> dimension, UUID teamId, List<ClaimedChunk> teamClaims) {
        LOGGER.debug("Updating team {} claims in dimension {}", teamId, dimension.location());

        FTBChunksBlueMapCompat.updateTeamClaims(dimension, teamId, teamClaims);

        LOGGER.debug("Updated {} claims for team {} in dimension {}",
                teamClaims.size(), 
                teamId, 
                dimension.location());
    }
}
