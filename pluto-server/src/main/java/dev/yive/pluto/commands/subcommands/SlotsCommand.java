package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class SlotsCommand implements PlutoSubcommand {
    private static final List<String> COMPLETIONS = List.of("get", "100", "125", "150", "175", "200", "225", "250", "275", "300");
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Use /pluto slots <number|get>", NamedTextColor.RED));
            return false;
        }

        String slotCount = args[0].toLowerCase(Locale.ENGLISH);
        if (slotCount.equalsIgnoreCase("get")) {
            sender.sendMessage(
                Component.join(
                    JoinConfiguration.noSeparators(),
                    Component.text("Current slot limit: ", NamedTextColor.GRAY),
                    Component.text(MinecraftServer.getServer().getPlayerCount(), NamedTextColor.WHITE),
                    Component.text("/", NamedTextColor.GRAY),
                    Component.text(MinecraftServer.getServer().getMaxPlayers(), NamedTextColor.WHITE)
                )
            );
            return true;
        }

        try {
            int newSlotCap = Integer.parseInt(slotCount);
            if (newSlotCap <= 0) {
                sender.sendMessage(Component.text("Error: Max slots must be 1 or higher.", NamedTextColor.RED));
                return false;
            }

            Bukkit.setMaxPlayers(newSlotCap);
            sender.sendMessage(
                Component.join(
                    JoinConfiguration.noSeparators(),
                    Component.text("New slot limit: ", NamedTextColor.GRAY),
                    Component.text(MinecraftServer.getServer().getPlayerCount(), NamedTextColor.WHITE),
                    Component.text("/", NamedTextColor.GRAY),
                    Component.text(MinecraftServer.getServer().getMaxPlayers(), NamedTextColor.WHITE)
                )
            );
        } catch (Exception e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return args.length == 1 ? COMPLETIONS : List.of();
    }
}
