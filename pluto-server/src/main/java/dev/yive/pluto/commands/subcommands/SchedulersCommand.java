package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import io.papermc.paper.threadedregions.scheduler.FoliaAsyncScheduler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

public class SchedulersCommand implements PlutoSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        Map<String, Integer> activeWorkers = new HashMap<>(),
            pendingTasks = new HashMap<>(),
            foliaTasks = Bukkit.getAsyncScheduler() instanceof FoliaAsyncScheduler asyncScheduler ? asyncScheduler.getTaskCount() : Map.of();
        Set<String> plugins = new HashSet<>(foliaTasks.keySet());

        for (final BukkitWorker activeWorker : Bukkit.getScheduler().getActiveWorkers()) {
            final String plugin = activeWorker.getOwner().getName();
            plugins.add(plugin);
            activeWorkers.compute(plugin, (s, integer) -> integer == null ? 1 : integer + 1);
        }

        for (final BukkitTask pendingTask : Bukkit.getScheduler().getPendingTasks()) {
            final String plugin = pendingTask.getOwner().getName();
            plugins.add(plugin);
            pendingTasks.compute(plugin, (s, integer) -> integer == null ? 1 : integer + 1);
        }

        sender.sendMessage(Component.text("Schedulers:", NamedTextColor.GRAY));
        for (final String plugin : plugins) {
            final Component hover = Component.join(
                JoinConfiguration.newlines(),
                Component.text(activeWorkers.getOrDefault(plugin, 0) + " Active",
                    NamedTextColor.AQUA),
                Component.text(pendingTasks.getOrDefault(plugin, 0) + " Pending",
                    NamedTextColor.AQUA),
                Component.text(foliaTasks.getOrDefault(plugin, 0) + " Folia",
                    NamedTextColor.AQUA)
            );

            sender.sendMessage(
                Component.text()
                    .content(" - " + plugin)
                    .color(NamedTextColor.GOLD)
                    .hoverEvent(hover)
                    .build()
            );
        }
        return true;
    }
}
