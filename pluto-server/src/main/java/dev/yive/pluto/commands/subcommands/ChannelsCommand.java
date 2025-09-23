package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ChannelsCommand implements PlutoSubcommand {
    private static final List<String> COMPLETIONS = List.of("outgoing", "incoming");
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Use /pluto channels <outgoing|incoming>", NamedTextColor.RED));
            return false;
        }

        boolean outgoing = "outgoing".equalsIgnoreCase(args[0]);
        Map<String, Set<String>> pluginChannels = new HashMap<>();
        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            pluginChannels.compute(plugin.getName(), (s, strings) -> {
                final Set<String> set = outgoing ? Bukkit.getMessenger().getOutgoingChannels(plugin) : Bukkit.getMessenger().getIncomingChannels(plugin);
                if (set.isEmpty()) return strings;

                if (strings == null) strings = new HashSet<>();
                strings.addAll(set);
                return strings;
            });
        }

        final Iterator<Map.Entry<String, Set<String>>> iterator = pluginChannels.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Set<String>> entry = iterator.next();
            if (!entry.getValue().isEmpty()) continue;
            iterator.remove();
        }

        sender.sendMessage(Component.text(outgoing ? "Outgoing channels: " : "Incoming channels: ", NamedTextColor.GRAY).append(Component.text(pluginChannels.size(), NamedTextColor.GOLD)));

        for (Map.Entry<String, Set<String>> entry : pluginChannels.entrySet()) {
            Set<String> channels = entry.getValue();
            if (channels.isEmpty()) continue;

            sender.sendMessage(Component.text(entry.getKey() + ":", NamedTextColor.YELLOW));
            for (final String channel : channels) {
                sender.sendMessage(Component.text("- ", NamedTextColor.GRAY).append(Component.text(channel, NamedTextColor.GOLD)));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return args.length == 1 ? COMPLETIONS : List.of();
    }
}
