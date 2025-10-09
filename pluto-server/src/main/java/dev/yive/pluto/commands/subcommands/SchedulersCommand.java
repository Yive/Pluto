package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import io.papermc.paper.threadedregions.scheduler.FoliaAsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

public class SchedulersCommand implements PlutoSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        Map<String, Integer> activeWorkers = new HashMap<>(), pendingTasks = new HashMap<>();
        Map<String, Map<ScheduledTask.ExecutionState, Integer>> foliaTasks = Bukkit.getAsyncScheduler() instanceof FoliaAsyncScheduler asyncScheduler ? asyncScheduler.pluto$getTaskCount() : Map.of();
        Set<String> plugins = new HashSet<>(foliaTasks.keySet());

        for (final BukkitWorker activeWorker : Bukkit.getScheduler().getActiveWorkers()) {
            final String plugin = activeWorker.getOwner().namespace();
            plugins.add(plugin);
            activeWorkers.compute(plugin, (s, integer) -> integer == null ? 1 : integer + 1);
        }

        for (final BukkitTask pendingTask : Bukkit.getScheduler().getPendingTasks()) {
            final String plugin = pendingTask.getOwner().namespace();
            plugins.add(plugin);
            pendingTasks.compute(plugin, (s, integer) -> integer == null ? 1 : integer + 1);
        }

        final Component threadHover = Component.join(
            JoinConfiguration.newlines(),
            Component.text("Folia Async Scheduler Threads: " + (Bukkit.getAsyncScheduler() instanceof FoliaAsyncScheduler scheduler ? scheduler.pluto$getThreadCount() : 0),
                NamedTextColor.AQUA),
            Component.text("Bukkit Async Scheduler Threads: " + (Bukkit.getScheduler() instanceof CraftScheduler scheduler ? scheduler.pluto$getThreadCount() : 0),
                NamedTextColor.AQUA)
        );
        sender.sendMessage(
            Component.text()
                .content("Schedulers:")
                .color(NamedTextColor.GRAY)
                .hoverEvent(threadHover)
                .build()
        );
        for (final String plugin : plugins) {
            final Component hover = Component.join(
                JoinConfiguration.newlines(),
                createFoliaComponent(foliaTasks.getOrDefault(plugin, Map.of())),
                createBukkitComponent(activeWorkers.getOrDefault(plugin, 0), pendingTasks.getOrDefault(plugin, 0))
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

    private Component createFoliaComponent(final Map<ScheduledTask.ExecutionState, Integer> states) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Folia:", NamedTextColor.AQUA));
        if (states.isEmpty()) {
            lines.add(Component.text("- Nothing", NamedTextColor.AQUA));
        } else {
            for (final ScheduledTask.ExecutionState state : ScheduledTask.ExecutionState.values()) {
                final int count = states.getOrDefault(state, 0);
                if (count == 0) continue;
                lines.add(Component.text(" - " + state + ": " + count,
                    NamedTextColor.AQUA));
            }
        }

        return Component.join(
            JoinConfiguration.newlines(),
            lines.toArray(Component[]::new)
        );
    }

    private Component createBukkitComponent(final int active, final int pending) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Bukkit:", NamedTextColor.AQUA));
        if (active == 0 && pending == 0) {
            lines.add(Component.text("- Nothing", NamedTextColor.AQUA));
        } else {
            if (active != 0)
                lines.add(Component.text(" - Active: " + active, NamedTextColor.AQUA));
            if (pending != 0)
                lines.add(Component.text(" - Pending: " + pending, NamedTextColor.AQUA));
        }

        return Component.join(
            JoinConfiguration.newlines(),
            lines.toArray(Component[]::new)
        );
    }
}
