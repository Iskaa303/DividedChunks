package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.network.CubeSync;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/** Sends the cubes edited during a tick at its end, one payload per section instead of per block. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Inject(method = "tick", at = @At("RETURN"))
	private void dividedchunks$flushCubes(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		CubeSync.flush((ServerLevel) (Object) this);
	}
}
