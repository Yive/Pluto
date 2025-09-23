package dev.yive.pluto.commands.subcommands;

import dev.yive.pluto.commands.PlutoSubcommand;
import io.papermc.paper.threadedregions.scheduler.FoliaAsyncScheduler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

public class SchedulersCommand implements PlutoSubcommand {
    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        Set<String> plugins = new HashSet<>();
        Map<String, Integer> activeWorkers = new HashMap<>();
        Map<String, Integer> pendingTasks = new HashMap<>();
        Map<String, Integer> foliaTasks = Bukkit.getAsyncScheduler() instanceof FoliaAsyncScheduler asyncScheduler ? asyncScheduler.getTaskCount() : Map.of();
        for (final BukkitWorker activeWorker : Bukkit.getScheduler().getActiveWorkers()) {
            plugins.add(activeWorker.getOwner().getName());
            activeWorkers.compute(activeWorker.getOwner().getName(), (s, integer) -> integer == null ? 1 : integer + 1);
        }
        for (final BukkitTask pendingTask : Bukkit.getScheduler().getPendingTasks()) {
            plugins.add(pendingTask.getOwner().getName());
            pendingTasks.compute(pendingTask.getOwner().getName(), (s, integer) -> integer == null ? 1 : integer + 1);
        }

        sender.sendMessage(Component.text("Schedulers: ", NamedTextColor.YELLOW));
        for (final String plugin : plugins) {
            sender.sendMessage(
                Component.text(
                    plugin + ": " + activeWorkers.getOrDefault(plugin, 0) + " Active, " +
                        pendingTasks.getOrDefault(plugin, 0) + " Pending, " +
                        foliaTasks.getOrDefault(plugin, 0) + " Folia", NamedTextColor.GOLD
                )
            );
        }
        return true;
    }
}
