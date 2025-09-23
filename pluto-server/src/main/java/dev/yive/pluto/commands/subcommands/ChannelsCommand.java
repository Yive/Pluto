package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ChannelsCommand implements PlutoSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        Map<String, Map<String, Set<String>>> pluginChannels = new HashMap<>();
        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            pluginChannels.compute(plugin.getName(), (s, map) -> {
                final Set<String> outgoing = Bukkit.getMessenger()
                    .getOutgoingChannels(plugin);
                final Set<String> incoming = Bukkit.getMessenger()
                    .getIncomingChannels(plugin);
                if (outgoing.isEmpty() && incoming.isEmpty()) return map;

                map = map == null ? new HashMap<>() : map;

                if (!outgoing.isEmpty())
                    map.put("outgoing", outgoing);

                if (!incoming.isEmpty())
                    map.put("incoming", incoming);
                return map;
            });
        }

        sender.sendMessage(Component.text("Plugin Channels:", NamedTextColor.GRAY));

        for (final Map.Entry<String, Map<String, Set<String>>> entry : pluginChannels.entrySet()) {
            final Map<String, Set<String>> map = entry.getValue();
            List<Component> components = new ArrayList<>();

            final Set<String> incoming = map.getOrDefault("incoming", Set.of());
            if (!incoming.isEmpty()) {
                components.add(Component.text("Incoming Channels:", NamedTextColor.GRAY));
                for (String channel : incoming) {
                    components.add(Component.text(channel, NamedTextColor.AQUA));
                }
            }

            final Set<String> outgoing = map.getOrDefault("outgoing", Set.of());
            if (!outgoing.isEmpty()) {
                components.add(Component.text("Outgoing Channels:", NamedTextColor.GRAY));
                for (String channel : outgoing) {
                    components.add(Component.text(channel, NamedTextColor.AQUA));
                }
            }

            if (components.isEmpty()) continue;

            final Component hover = Component.join(JoinConfiguration.newlines(), components);

            sender.sendMessage(
                Component.text()
                    .content(" - " + entry.getKey())
                    .color(NamedTextColor.GOLD)
                    .hoverEvent(hover)
                    .build()
            );
        }

        return true;
    }
}
