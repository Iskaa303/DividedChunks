package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.iskaa303.dividedchunks.core.world.WorldKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Closes the per-dimension cube stores after the server stops, flushing every region file. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

	/** A vanilla world has no cubes to read, so it is refused instead of being regenerated over the save. */
	@Inject(method = "loadLevel", at = @At("HEAD"))
	private void dividedchunks$refuseVanillaWorld(CallbackInfo ci) {
		MinecraftServer self = (MinecraftServer) (Object) this;
		if (WorldKind.of(self.getWorldPath(LevelResource.ROOT)) == WorldKind.VANILLA) {
			throw new IllegalStateException("This world still uses vanilla region files, convert it first: "
					+ self.getWorldPath(LevelResource.ROOT));
		}
	}

	@Inject(method = "stopServer", at = @At("RETURN"))
	private void dividedchunks$closeStores(CallbackInfo ci) {
		CubeStores.closeAll();
	}
}
