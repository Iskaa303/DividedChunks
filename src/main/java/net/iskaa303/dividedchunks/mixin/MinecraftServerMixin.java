package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Closes the per-dimension cube stores after the server stops, flushing every region file. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

	@Inject(method = "stopServer", at = @At("RETURN"))
	private void dividedchunks$closeStores(CallbackInfo ci) {
		CubeStores.closeAll();
	}
}
