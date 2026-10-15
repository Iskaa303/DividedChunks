package net.iskaa303.dividedchunks.platform.neoforge;

//? neoforge {

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.network.ClientCubeReceiver;
import net.iskaa303.dividedchunks.network.CubePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the cube sync payload and its client handler on the mod bus. */
@EventBusSubscriber(modid = DividedChunks.MOD_ID)
public class NeoforgePayloads {

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToClient(CubePayload.TYPE, CubePayload.STREAM_CODEC,
				(payload, context) -> ClientCubeReceiver.handle(payload));
	}
}
//?}
