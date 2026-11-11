package net.iskaa303.dividedchunks.mixin;

import com.mojang.datafixers.DataFixer;
import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ChunkTags;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Keeps every {@link SimpleRegionStorage} payload - chunk metadata, entities, points of interest -
 * in the {@code .dcr} store, so nothing writes {@code .mca}.
 */
@Mixin(SimpleRegionStorage.class)
public abstract class SimpleRegionStorageMixin {

	@Unique
	private Path dividedchunks$folder;

	@Unique
	private String dividedchunks$type;

	/** Writes still in flight, so a synchronize or close does not return before they land. */
	@Unique
	private final Set<CompletableFuture<Void>> dividedchunks$pending = ConcurrentHashMap.newKeySet();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void dividedchunks$capture(RegionStorageInfo info, Path folder, DataFixer fixerUpper,
			boolean syncWrites, DataFixTypes dataFixType, CallbackInfo ci) {
		this.dividedchunks$folder = folder;
		this.dividedchunks$type = info.type();
	}

	@Inject(method = "read", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$read(ChunkPos pos,
			CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
		Integer tagY = this.dividedchunks$tagY();
		if (tagY == null) {
			return;
		}
		CubeRegionStorage store = this.dividedchunks$store();
		cir.setReturnValue(CompletableFuture.supplyAsync(() -> store.getTag(tagY, pos.x(), pos.z())
				.<Optional<CompoundTag>>map(tag -> Optional.of(ChunkTags.fromBytes(tag)))
				.orElseGet(Optional::empty), Util.backgroundExecutor()));
	}

	@Inject(method = "write(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
			at = @At("HEAD"), cancellable = true)
	private void dividedchunks$write(ChunkPos pos, Supplier<CompoundTag> supplier,
			CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		Integer tagY = this.dividedchunks$tagY();
		if (tagY == null) {
			return;
		}
		CubeRegionStorage store = this.dividedchunks$store();
		CubePos key = new CubePos(pos.x(), tagY, pos.z());
		CompletableFuture<Void> write = CompletableFuture
				.supplyAsync(supplier, Util.backgroundExecutor())
				.thenAccept(tag -> {
					if (tag == null) {
						store.remove(key);
					} else {
						store.put(key, ChunkTags.toBytes(tag));
					}
				});
		this.dividedchunks$pending.add(write);
		write.whenComplete((ignored, error) -> this.dividedchunks$pending.remove(write));
		cir.setReturnValue(write);
	}

	@Inject(method = "synchronize", at = @At("HEAD"))
	private void dividedchunks$awaitWrites(boolean flush, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		if (this.dividedchunks$tagY() == null) {
			return;
		}
		CompletableFuture.allOf(this.dividedchunks$pending.toArray(CompletableFuture[]::new)).join();
		if (flush) {
			this.dividedchunks$store().flush();
		}
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void dividedchunks$closePending(CallbackInfo ci) {
		CompletableFuture.allOf(this.dividedchunks$pending.toArray(CompletableFuture[]::new)).join();
	}

	@Inject(method = "isOldChunkAround", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$noOldChunks(ChunkPos pos, int range, CallbackInfoReturnable<Boolean> cir) {
		if (this.dividedchunks$tagY() != null) {
			cir.setReturnValue(false);
		}
	}

	/** Record Y the payload of this storage lives at, or {@code null} for a storage we leave alone. */
	@Unique
	private Integer dividedchunks$tagY() {
		if (this.dividedchunks$type == null) {
			return null;
		}
		return switch (this.dividedchunks$type) {
			case "chunk" -> CubeRegionStorage.CHUNK_TAG_Y;
			case "entities" -> CubeRegionStorage.ENTITY_TAG_Y;
			case "poi" -> CubeRegionStorage.POI_TAG_Y;
			default -> null;
		};
	}

	@Unique
	private CubeRegionStorage dividedchunks$store() {
		return CubeStores.get(this.dividedchunks$folder.getParent());
	}
}
