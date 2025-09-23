package dev.yive.pluto.commands;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class PlutoCommands {

    private PlutoCommands() {
    }

    private static final Map<String, Command> COMMANDS = new HashMap<>();

    public static void registerCommands(final MinecraftServer server) {
        COMMANDS.put("pluto", new PlutoCommand("pluto"));

        COMMANDS.forEach((s, command) -> {
            server.server.getCommandMap().register(s, "Pluto", command);
        });
    }
}
