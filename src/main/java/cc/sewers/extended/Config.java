package cc.sewers.extended;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SewersExtended.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Max update interval for the bluemap chunk auto update, in seconds. min: 5 seconds, max: no limit.
    private static final ModConfigSpec.IntValue BLUEMAP_CHUNK_AUTO_UPDATE_MS = BUILDER
            .comment("How often the bluemap chunk auto update should run, in milliseconds.")
            .defineInRange("ftbchunksClaimIntervalMs", 60000, 5000, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int bluemapChunkAutoUpdateMs;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        bluemapChunkAutoUpdateMs = BLUEMAP_CHUNK_AUTO_UPDATE_MS.get();
    }
}
