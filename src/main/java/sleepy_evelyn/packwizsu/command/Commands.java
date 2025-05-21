package sleepy_evelyn.packwizsu.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sleepy_evelyn.packwizsu.async.AbstractAsyncTask;
import sleepy_evelyn.packwizsu.async.TaskScheduler;
import sleepy_evelyn.packwizsu.util.*;

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static sleepy_evelyn.packwizsu.PackWizSU.*;

public final class Commands {

    static final SimpleCommandExceptionType FILE_UPDATE_FAILED = new SimpleCommandExceptionType(Text.literal("Failed to update the packwiz-server-updater.properties file within the root directory of the server"));
    static final SimpleCommandExceptionType UPDATE_IN_PROGRESS_ERROR = new SimpleCommandExceptionType(Text.literal("Packwiz update is already in progress"));
    static final SimpleCommandExceptionType NO_PACK_TOML = new SimpleCommandExceptionType(Text.literal("There is no pack.toml link to update from. Add this using /packwizsu link [url]"));
    static final SimpleCommandExceptionType DIRECTORY_SECURITY_ERROR = new SimpleCommandExceptionType(Text.literal("Packwiz server updater does not have permission to access the game directory and modify files"));
    static final SimpleCommandExceptionType DIRECTORY_BLANK_ERROR = new SimpleCommandExceptionType(Text.literal("Failed to update Packwiz. Game directory doesn't exist or has changed since server startup. Check the server console for details"));

    private static final MutableText UPDATE_START = Text.literal("Updating modpack. This may take a while...").formatted(Formatting.GRAY);
    private static final MutableText UPDATE_START_NO_BOOTSTRAP = Text.literal("Downloading the Packwiz Bootstrap and updating the modpack. This may take a while...").formatted(Formatting.GRAY);
    private static final MutableText UPDATED_TOML_LINK = Text.literal("Successfully linked a Packwiz modpack. Use /packwiz update for the changes to take effect.").formatted(Formatting.GREEN);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("packwiz")
                .then(literal("link")
                        .then(argument("url", StringArgumentType.greedyString())
                                .executes(Commands::setTomlLink)
                        )
                )
                .then(literal("update")
                        .executes(Commands::restartAndUpdate)
                )
                .requires(source -> source.hasPermissionLevel(4))
        );
    }

    private static int setTomlLink(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        try {
            URL url = testPackTomlLink(StringArgumentType.getString(ctx, "url"));

            CONFIG.packLink = url.toExternalForm();
            CONFIG.save();

            ctx.getSource().sendMessage(UPDATED_TOML_LINK);
            return Command.SINGLE_SUCCESS;
        } catch (PackTomlURLException ptue) {
            throw new SimpleCommandExceptionType(Text.literal(ptue.getMessage())).create();
        } catch (Exception e) {
            LOGGER.error("Encountered an error while updating pack url", e);
            throw FILE_UPDATE_FAILED.create();
        }
    }

    private static int restartAndUpdate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        try {
            if (Files.notExists(GAME_DIR))
                throw DIRECTORY_BLANK_ERROR.create();
        } catch (SecurityException se) {
            throw DIRECTORY_SECURITY_ERROR.create();
        }

        String packTomlLink = CONFIG.packLink;
        
        if (!packTomlLink.contains("pack.toml"))
            throw NO_PACK_TOML.create();
        
        if (TaskScheduler.hasTask(TaskScheduler.UPDATE_PACKWIZ_TASK))
            throw UPDATE_IN_PROGRESS_ERROR.create();

        boolean hasBootstrap = Files.exists(Path.of(CONFIG.bootstrapJarName));
        ctx.getSource().sendMessage(hasBootstrap ? UPDATE_START : UPDATE_START_NO_BOOTSTRAP);

        tryUpdatePackwiz(ctx, packTomlLink, hasBootstrap);
        return Command.SINGLE_SUCCESS;
    }

    private static void tryUpdatePackwiz(CommandContext<ServerCommandSource> ctx, @NotNull final String packTomllink, final boolean hasBootstrap) {
        final String[] command = new String[] { JavaUtil.getFullJavaPath(), "-jar", CONFIG.bootstrapJarName, "-g", "-s", "server", packTomllink };

        TaskScheduler.submit(TaskScheduler.UPDATE_PACKWIZ_TASK, () ->
                new AsyncCommandTask(ctx, CompletableFuture.runAsync(() -> {
            try {
                if (!hasBootstrap) {
                    Path bootstrapPath = GAME_DIR.resolve(CONFIG.bootstrapJarName);
                    HashedFileDownloader downloader = new HashedFileDownloader(CONFIG.bootstrapUrl, CONFIG.bootstrapHash, bootstrapPath);

                    downloader.download();
                    downloader.assertHashMatches();
                }

                testPackTomlLink(packTomllink);

                Process process = new ProcessBuilder(command)
                        .inheritIO().start();

                try (BufferedReader bufferedReader = process.inputReader()) {
                    bufferedReader.lines().forEach(LOGGER::info);
                }

                int exitCode = process.waitFor();

                if (exitCode != 0)
                    throw new ProcessExitCodeException("Process failed with exit code: " + exitCode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }), 20));
    }

    private static @NotNull URL testPackTomlLink(@NotNull final String packTomllink) throws PackTomlURLException {
        try {
            return new URL(packTomllink);
        } catch (MalformedURLException mue) {
            throw new PackTomlURLException("The link submitted is not a valid URL");
        } catch (IllegalStateException ise) {
            throw new PackTomlURLException("The file contains invalid data");
        }
    }

    private static class AsyncCommandTask extends AbstractAsyncTask {

        private final ServerCommandSource source;

        public AsyncCommandTask(CommandContext<ServerCommandSource> ctx, CompletableFuture<Void> future, int pollTicks) {
            super(future, pollTicks);

            this.source = ctx.getSource();
        }

        @Override
        public void sendMessage(@Nullable Text text) {
            if (text != null)
                this.source.sendMessage(text);
        }
    }
}
