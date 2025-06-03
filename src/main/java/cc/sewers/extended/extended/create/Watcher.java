package cc.sewers.extended.extended.create;

import cc.sewers.extended.Config;
import com.mojang.logging.LogUtils;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Watcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> future;
    private static MinecraftServer server;

    public static void init(MinecraftServer minecraftServer) {
        LOGGER.info("Initializing Create Bluemap watcher...");
        if (Config.bluemapCreateTrainsEnabled) {
            start();
            Watcher.server = minecraftServer;
        } else {
            LOGGER.info("Create Bluemap watcher is disabled in the config.");
        }
    }

    public static void start() {
        LOGGER.info("Starting Create Bluemap watcher...");
        Runnable task = () -> {
            Optional<BlueMapAPI> apiOptional = BlueMapAPI.getInstance();
            apiOptional.ifPresent(api -> {
                try {
                    Trains.update(api);
                    Tracks.update(api);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            });
        };
        future = scheduler.scheduleAtFixedRate(task, 0, Config.bluemapCreateIntervalMs, TimeUnit.MILLISECONDS);

    }

    public static void stop() {
        future.cancel(false);
    }
}
