package cc.sewers.extended.extended.ftbchunks;

import java.util.*;
import java.util.stream.Collectors;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import net.minecraft.network.chat.TextColor;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class FTBChunksUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FTBChunksAPI.API ftbChunksAPI = FTBChunksAPI.api();
    private static final TeamManager teamManager = FTBTeamsAPI.api().getManager();

    public static FTBChunksAPI.API getFtbChunksAPI() {
        return ftbChunksAPI;
    }
    public static TeamManager getTeamManager() {
        return teamManager;
    }

    public static Map<UUID, List<ClaimedChunk>> getClaimedChunks(ResourceKey<Level> dimension) {
        Collection<ClaimedChunk> claimedChunks = (Collection<ClaimedChunk>) ftbChunksAPI.getManager().getAllClaimedChunks();
        return claimedChunks.stream()
                .filter(chunk -> chunk.getPos().dimension().equals(dimension))
                .collect(Collectors.groupingBy(
                        chunk -> chunk.getTeamData().getTeam().getTeamId(),
                        Collectors.toList()
                ));
    }

    public static List<ClaimedChunk> getClaimedChunksForTeam(ResourceKey<Level> dimension, UUID teamId) {
        Collection<ClaimedChunk> claimedChunks = (Collection<ClaimedChunk>) ftbChunksAPI.getManager().getAllClaimedChunks();
        return claimedChunks.stream()
                .filter(chunk -> chunk.getPos().dimension().equals(dimension))
                .filter(chunk -> chunk.getTeamData().getTeam().getTeamId().equals(teamId))
                .collect(Collectors.toList());
    }

    public static int getTeamColor(Team team) {
        TextColor color = team.getColoredName().getStyle().getColor();
        if (color != null) {
            return color.getValue();
        }
        return 0xFFFFFF;
    }
}
