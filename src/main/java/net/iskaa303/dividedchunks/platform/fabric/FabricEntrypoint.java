package net.iskaa303.dividedchunks.platform.fabric;

//? fabric {

/*import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.network.CubePayload;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(CubePayload.TYPE, CubePayload.STREAM_CODEC);
		DividedChunks.onInitialize();
	}
}
*///?}
