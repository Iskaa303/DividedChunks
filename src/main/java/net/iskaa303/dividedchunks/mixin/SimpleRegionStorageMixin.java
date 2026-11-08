package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ChunkTags;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Serves the chunk metadata through {@code .dcr} instead of a region file, in the same column records
 * as the sections, so no {@code .mca} is read or written for chunk data.
 */
@Mixin(SimpleRegionStorage.class)
public abstract class SimpleRegionStorageMixin {

	@Inject(method = "read", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$readChunkTag(ChunkPos pos,
			CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
		if (!((Object) this instanceof ChunkMapAccess access)) {
			return;
		}
		ServerLevel level = access.dividedchunks$level();
		byte[] data = CubeStores.get(level).getChunkTag(pos.x(), pos.z()).orElse(null);
		cir.setReturnValue(CompletableFuture.completedFuture(
				data == null ? Optional.empty() : Optional.of(ChunkTags.fromBytes(data))));
	}

	@Inject(method = "write(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
			at = @At("HEAD"), cancellable = true)
	private void dividedchunks$writeChunkTag(ChunkPos pos, Supplier<CompoundTag> supplier,
			CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		if (!((Object) this instanceof ChunkMapAccess access)) {
			return;
		}
		ServerLevel level = access.dividedchunks$level();
		cir.setReturnValue(CompletableFuture.supplyAsync(supplier, Util.backgroundExecutor()).thenAccept(tag -> {
			CubeRegionStorage store = CubeStores.get(level);
			store.putChunkTag(pos.x(), pos.z(), ChunkTags.toBytes(tag));
		}));
	}

	@Inject(method = "isOldChunkAround", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$noOldChunks(ChunkPos pos, int range, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ChunkMapAccess) {
			cir.setReturnValue(false);
		}
	}
}
