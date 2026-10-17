package net.iskaa303.dividedchunks.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carries the full block Y over the network. Vanilla packs a {@link BlockPos} into a long with a
 * 12-bit Y ({@code PACKED_Y_LENGTH}), so anything outside ±2048 arrives wrapped — a click on y=10089
 * reached the server as y=1897 and was dropped as out of reach, which is why only command placement
 * worked. Every block position on the wire goes through these two methods, so this is the one seam.
 *
 * <p>Vanilla and unmodified mods cannot read this format; the protocol-mismatch rejection in
 * docs/IMPLEMENTATION_PLAN.md §4.6 is what keeps them from connecting and misreading positions.
 */
@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {

	@Inject(
			method = "writeBlockPos(Lio/netty/buffer/ByteBuf;Lnet/minecraft/core/BlockPos;)V",
			at = @At("HEAD"),
			cancellable = true)
	private static void dividedchunks$writeBlockPos(ByteBuf output, BlockPos pos, CallbackInfo ci) {
		output.writeInt(pos.getX());
		output.writeInt(pos.getY());
		output.writeInt(pos.getZ());
		ci.cancel();
	}

	@Inject(
			method = "readBlockPos(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/core/BlockPos;",
			at = @At("HEAD"),
			cancellable = true)
	private static void dividedchunks$readBlockPos(ByteBuf input, CallbackInfoReturnable<BlockPos> cir) {
		cir.setReturnValue(new BlockPos(input.readInt(), input.readInt(), input.readInt()));
	}
}
