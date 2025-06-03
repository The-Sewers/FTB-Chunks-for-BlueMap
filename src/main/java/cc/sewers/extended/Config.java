package cc.sewers.extended;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SewersExtended.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue BLUEMAP_FTBCHUNKS_ENABLED = BUILDER
            .comment("Whether to enable FTBChunks compatibility for BlueMap.")
            .define("ftbchunksEnabled", true);
    private static final ModConfigSpec.IntValue BLUEMAP_CHUNK_AUTO_UPDATE_MS = BUILDER
            .comment("How often the bluemap chunk auto update should run, in milliseconds.")
            .defineInRange("ftbchunksClaimIntervalMs", 60000, 5000, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue BLUEMAP_CREATE_TRAINS_ENABLED = BUILDER
            .comment("Whether to enable Create trains compatibility for BlueMap.")
            .define("createTrainsEnabled", true);
    private static final ModConfigSpec.IntValue BLUEMAP_CREATE_INTERVAL_MS = BUILDER
            .comment("How often the bluemap create trains should update, in milliseconds.")
            .defineInRange("bluemapCreateIntervalMs", 5000, 1000, Integer.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue BLUEMAP_RENDER_CARRIAGES = BUILDER
            .comment("Whether to render carriages in bluemap.")
            .define("bluemapRenderCarriages", true);
    private static final ModConfigSpec.BooleanValue BLUEMAP_RENDER_TRACKS = BUILDER
            .comment("Whether to render tracks in bluemap.")
            .define("bluemapRenderTracks", true);
    private static final ModConfigSpec.BooleanValue BLUEMAP_RENDER_TRAINS = BUILDER
            .comment("Whether to render trains in bluemap.")
            .define("bluemapRenderTrains", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean bluemapFtbChunksEnabled;
    public static int bluemapChunkAutoUpdateMs;
    public static boolean bluemapCreateTrainsEnabled;
    public static int bluemapCreateIntervalMs;
    public static boolean bluemapRenderCarriages;
    public static boolean bluemapRenderTracks;
    public static boolean bluemapRenderTrains;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        bluemapFtbChunksEnabled = BLUEMAP_FTBCHUNKS_ENABLED.get();
        bluemapChunkAutoUpdateMs = BLUEMAP_CHUNK_AUTO_UPDATE_MS.get();

        bluemapCreateTrainsEnabled = BLUEMAP_CREATE_TRAINS_ENABLED.get();
        bluemapCreateIntervalMs = BLUEMAP_CREATE_INTERVAL_MS.get();
        bluemapRenderCarriages = BLUEMAP_RENDER_CARRIAGES.get();
        bluemapRenderTracks = BLUEMAP_RENDER_TRACKS.get();
        bluemapRenderTrains = BLUEMAP_RENDER_TRAINS.get();
    }
}
