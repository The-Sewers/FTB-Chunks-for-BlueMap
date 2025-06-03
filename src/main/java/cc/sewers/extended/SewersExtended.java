package cc.sewers.extended;

import cc.sewers.extended.extended.bluemap.BlueMapUtil;
import cc.sewers.extended.extended.ftbchunks.ChunkClaimCache;
import cc.sewers.extended.extended.ftbchunks.PlayerEventHandler;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SewersExtended.MODID)
public class SewersExtended {
    public static final String MODID = "ftbchunksbluemap";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ChunkClaimCache claimCache = new ChunkClaimCache();

    public SewersExtended(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(PlayerEventHandler.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Initializing FTBChunksBlueMap");
        BlueMapUtil.init(event.getServer());

        if (Config.bluemapFtbChunksEnabled) {
            ClaimedChunkEvent.AFTER_CLAIM.register((CommandSourceStack var1, ClaimedChunk chunk) -> claimCache.addClaim(chunk));
            ClaimedChunkEvent.AFTER_UNCLAIM.register((CommandSourceStack var1, ClaimedChunk chunk) -> claimCache.removeClaim(chunk));
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Shutting down SewersExtended");
        BlueMapUtil.reset();
        claimCache.clear();
    }
}
