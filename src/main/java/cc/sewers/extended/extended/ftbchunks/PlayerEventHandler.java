package cc.sewers.extended.extended.ftbchunks;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, UUID> playerTeamCache = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<Team> team = FTBChunksUtil.getTeamManager().getPlayerTeamForPlayerID(player.getUUID());
        if (team.isEmpty()) {
            LOGGER.warn("Player {} logged in without a team, skipping claim update", player.getGameProfile().getName());
            return;
        }

        UUID teamId = team.get().getTeamId();
        playerTeamCache.put(player.getUUID(), teamId);

        LOGGER.debug("Player {} logged in, updating team {} claims", player.getGameProfile().getName(), teamId);

        for (ResourceKey<Level> dimension : Objects.requireNonNull(player.getServer()).levelKeys()) {
            FTBChunksBlueMapCompat.updateTeamClaims(dimension, teamId);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        UUID teamId = playerTeamCache.remove(playerId);

        if (teamId == null) {
            Optional<Team> team = FTBChunksUtil.getTeamManager().getPlayerTeamForPlayerID(playerId);
            if (team.isEmpty()) {
                LOGGER.warn("Player {} logged out without a cached team, skipping claim update", player.getGameProfile().getName());
                return;
            }
            teamId = team.get().getTeamId();
        }

        LOGGER.debug("Player {} logged out, updating team {} claims", player.getGameProfile().getName(), teamId);

        for (ResourceKey<Level> dimension : player.getServer().levelKeys()) {
            FTBChunksBlueMapCompat.updateTeamClaims(dimension, teamId);
        }
    }
}
