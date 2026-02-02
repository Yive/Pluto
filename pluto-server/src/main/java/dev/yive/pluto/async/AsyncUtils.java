package dev.yive.pluto.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public class AsyncUtils {
    public static CompletableFuture<Void> completeOnMain(Runnable runnable) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftServer.getServer().scheduleOnMain(() -> {
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
