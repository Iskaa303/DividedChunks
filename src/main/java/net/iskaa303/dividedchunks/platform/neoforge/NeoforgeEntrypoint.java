package net.iskaa303.dividedchunks.platform.neoforge;

//? neoforge {

import net.iskaa303.dividedchunks.DividedChunks;
import net.neoforged.fml.common.Mod;

@Mod(DividedChunks.MOD_ID)
public class NeoforgeEntrypoint {

	public NeoforgeEntrypoint() {
		DividedChunks.onInitialize();
	}
}
//?}
