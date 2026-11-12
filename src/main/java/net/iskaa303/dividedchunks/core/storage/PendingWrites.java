package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Writes still in flight, keyed by cube. Writes of one cube run in the order they were queued and a
 * read of that cube waits for them, which is what vanilla's single-threaded {@code IOWorker} gave the
 * region files it replaces: without it a column could be read while its own payloads land, or land
 * out of order.
 */
public final class PendingWrites {

	private final Map<CubePos, CompletableFuture<Void>> inflight = new ConcurrentHashMap<>();
	private final Executor executor;

	public PendingWrites() {
		this(Util.backgroundExecutor());
	}

	PendingWrites(Executor executor) {
		this.executor = executor;
	}

	/** Queues {@code body} after the writes already queued for {@code key}. */
	public CompletableFuture<Void> write(CubePos key, Runnable body) {
		CompletableFuture<Void> queued = this.dividedchunks$settled(this.inflight.get(key))
				.thenRunAsync(body, this.executor);
		this.inflight.put(key, queued);
		queued.whenComplete((ignored, error) -> this.inflight.remove(key, queued));
		return queued;
	}

	/** Runs {@code read} once the writes queued for {@code key} so far have landed. */
	public <T> CompletableFuture<T> readAfter(CubePos key, Supplier<T> read) {
		return this.dividedchunks$settled(this.inflight.get(key)).thenApplyAsync(ignored -> read.get(), this.executor);
	}

	/** Blocks until every queued write has landed. */
	public void awaitAll() {
		CompletableFuture.allOf(this.inflight.values().toArray(CompletableFuture[]::new)).join();
	}

	/** A failed write must not stop the writes and reads queued behind it. */
	private CompletableFuture<Void> dividedchunks$settled(CompletableFuture<Void> write) {
		return write == null ? CompletableFuture.completedFuture(null) : write.exceptionally(ignored -> null);
	}
}
