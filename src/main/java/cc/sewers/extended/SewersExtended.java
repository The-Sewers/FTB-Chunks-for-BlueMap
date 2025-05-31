package cc.sewers.extended;

import cc.sewers.extended.extended.bluemap.BlueMapUtil;
import cc.sewers.extended.extended.ftbchunks.ChunkClaimCache;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SewersExtended.MODID)
public class SewersExtended {
    public static final String MODID = "sewersextended";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ChunkClaimCache claimCache = new ChunkClaimCache();
    private long updateIntervalTicks;
    private long lastUpdateTime = 0;
    private MinecraftServer server;

    public SewersExtended(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Initializing SewersExtended");
        this.server = event.getServer();
        BlueMapUtil.init(event.getServer());
        ClaimedChunkEvent.AFTER_CLAIM.register((CommandSourceStack var1, ClaimedChunk chunk) -> claimCache.addClaim(chunk));
        ClaimedChunkEvent.AFTER_UNCLAIM.register((CommandSourceStack var1, ClaimedChunk chunk) -> claimCache.removeClaim(chunk));

        // Convert config value from milliseconds to ticks (20 ticks = 1 second)
        updateIntervalTicks = Math.max(600, Config.bluemapChunkAutoUpdateMs / 50); // Minimum 30 seconds (600 ticks)
        LOGGER.info("SewersExtended timer initialized - checking for claim changes every {} seconds", updateIntervalTicks / 20);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!event.hasTime() || server == null) return;
        long currentTime = server.getTickCount();
        if (currentTime - lastUpdateTime > updateIntervalTicks) {
            lastUpdateTime = currentTime;
            LOGGER.debug("Running periodic claim check...");
            updateChunkClaimsFromCache();
        }
    }

    private void updateChunkClaimsFromCache() {
        if (server == null) return;
        try {
            claimCache.updateAndProcessChanges(server);
        } catch (Exception e) {
            LOGGER.error("Error updating chunk claims", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Shutting down SewersExtended");
        claimCache.clear();
    }
}
