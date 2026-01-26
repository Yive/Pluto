package dev.yive.pluto.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public class AsyncUtils {
    public static java.util.concurrent.CompletableFuture<Void> completeOnMain(Runnable runnable) {
        final java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        net.minecraft.server.MinecraftServer.getServer().scheduleOnMain(() -> {
            runnable.run();
            future.complete(null);
        });
        return future;
    }

    public static <V> CompletableFuture<V> completeOnMain(Supplier<V> supplier) {
        final CompletableFuture<V> future = new CompletableFuture<>();
        MinecraftServer.getServer().scheduleOnMain(() -> future.complete(supplier.get()));
        return future;
    }
}
