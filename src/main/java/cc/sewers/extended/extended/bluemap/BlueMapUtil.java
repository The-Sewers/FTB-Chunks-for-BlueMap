package cc.sewers.extended.extended.bluemap;

import cc.sewers.extended.extended.ftbchunks.FTBChunksBlueMapCompat;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlueMapUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Consumer<MinecraftServer>> blueMapPlugins = new ArrayList<>();
    private static boolean initialized = false;

    public static void register() {
        registerPlugin(FTBChunksBlueMapCompat::init);

        LOGGER.info("Registered BlueMap compatibility plugins");
    }

    public static void registerPlugin(Consumer<MinecraftServer> plugin) {
        blueMapPlugins.add(plugin);
        LOGGER.debug("Registered BlueMap plugin: {}", plugin.getClass().getName());
    }

    public static void init(MinecraftServer server) {
        if (initialized) {
            LOGGER.warn("BlueMap plugins already initialized");
            return;
        }

        LOGGER.info("Initializing BlueMap plugins...");
        register();

        LOGGER.info("Initializing {} BlueMap compatibility plugins", blueMapPlugins.size());
        for (Consumer<MinecraftServer> plugin : blueMapPlugins) {
            try {
                plugin.accept(server);
            } catch (Exception e) {
                LOGGER.error("Error initializing BlueMap plugin: {}", e.getMessage(), e);
            }
        }

        initialized = true;
    }

    public static void reset() {
        initialized = false;
        LOGGER.debug("BlueMap plugin initialization reset");
    }
}
