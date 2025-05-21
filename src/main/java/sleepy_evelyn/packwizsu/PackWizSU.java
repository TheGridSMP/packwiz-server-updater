package sleepy_evelyn.packwizsu;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sleepy_evelyn.packwizsu.command.Commands;
import sleepy_evelyn.packwizsu.config.Config;
import sleepy_evelyn.packwizsu.async.TaskScheduler;

import java.nio.file.Path;

public class PackWizSU implements DedicatedServerModInitializer {

    public static final String MOD_ID = "packwizsu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();

    public static Config CONFIG;

    @Override
    public void onInitializeServer() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(Config.class).getConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env)
                -> Commands.register(dispatcher));

        ServerTickEvents.START_SERVER_TICK.register(
                s -> TaskScheduler.poll());
    }
}
