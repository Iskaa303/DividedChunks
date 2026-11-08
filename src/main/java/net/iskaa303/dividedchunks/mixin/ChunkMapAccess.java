package net.iskaa303.dividedchunks.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes {@link ChunkMap}'s level so the chunk storage can reach that dimension's {@code .dcr} store. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccess {

	@Accessor("level")
	ServerLevel dividedchunks$level();
}
