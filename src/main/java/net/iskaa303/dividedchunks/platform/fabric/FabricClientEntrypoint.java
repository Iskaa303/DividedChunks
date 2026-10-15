package net.iskaa303.dividedchunks.platform.fabric;

//? fabric {

/*import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.network.ClientCubeReceiver;
import net.iskaa303.dividedchunks.network.CubePayload;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		DividedChunks.onInitializeClient();
		ClientPlayNetworking.registerGlobalReceiver(CubePayload.TYPE, (payload, context) -> ClientCubeReceiver.handle(payload));
	}
}
*///?}
