package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingWritesTest {

	private static final CubePos KEY = new CubePos(1, 2, 3);

	/** Runs queued work only when the test says so, so ordering is observed instead of raced. */
	private static final class ManualExecutor implements Executor {
		private final Queue<Runnable> tasks = new ArrayDeque<>();

		@Override
		public void execute(Runnable command) {
			this.tasks.add(command);
		}

		void runAll() {
			while (!this.tasks.isEmpty()) {
				this.tasks.poll().run();
			}
		}
	}

	@Test
	void writesOfOneCubeLandInOrder() {
		ManualExecutor executor = new ManualExecutor();
		PendingWrites pending = new PendingWrites(executor);
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			int value = i;
			pending.write(KEY, () -> order.add(value));
		}
		executor.runAll();
		assertEquals(List.of(0, 1, 2, 3, 4), order);
	}

	@Test
	void aReadWaitsForTheWritesQueuedBeforeIt() {
		ManualExecutor executor = new ManualExecutor();
		PendingWrites pending = new PendingWrites(executor);
		boolean[] written = {false};
		pending.write(KEY, () -> written[0] = true);
		CompletableFuture<Boolean> read = pending.readAfter(KEY, () -> written[0]);
		assertFalse(read.isDone(), "the read ran before the write it queued behind");
		executor.runAll();
		assertTrue(read.join(), "the read saw the store before the write landed");
	}

	@Test
	void aReadWithoutWritesRunsStraightAway() {
		ManualExecutor executor = new ManualExecutor();
		PendingWrites pending = new PendingWrites(executor);
		CompletableFuture<String> read = pending.readAfter(KEY, () -> "value");
		executor.runAll();
		assertEquals("value", read.join());
	}

	@Test
	void aFailedWriteDoesNotBlockTheNextOnes() {
		ManualExecutor executor = new ManualExecutor();
		PendingWrites pending = new PendingWrites(executor);
		CompletableFuture<Void> failed = pending.write(KEY, () -> {
			throw new IllegalStateException("disk full");
		});
		executor.runAll();
		assertTrue(failed.isCompletedExceptionally());
		boolean[] ran = {false};
		pending.write(KEY, () -> ran[0] = true);
		executor.runAll();
		assertTrue(ran[0], "the write queued behind a failed one never ran");
	}

	@Test
	void awaitAllWaitsForEveryCube() {
		ManualExecutor executor = new ManualExecutor();
		PendingWrites pending = new PendingWrites(executor);
		CompletableFuture<Void> gate = new CompletableFuture<>();
		pending.write(new CubePos(0, 0, 0), () -> gate.join());
		pending.write(new CubePos(9, 9, 9), () -> gate.join());
		CompletableFuture<Void> awaited = CompletableFuture.runAsync(pending::awaitAll);
		assertFalse(awaited.isDone(), "awaitAll returned while writes were still queued");
		gate.complete(null);
		executor.runAll();
		awaited.join();
	}
}
