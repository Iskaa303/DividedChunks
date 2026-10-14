package net.iskaa303.dividedchunks.platform.neoforge;

//? neoforge {

import net.iskaa303.dividedchunks.DividedChunks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = DividedChunks.MOD_ID, value = Dist.CLIENT)
public class NeoforgeClientEventSubscriber {

	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		DividedChunks.onInitializeClient();
	}
}
//?}
