package net.iskaa303.dividedchunks.gametest;

//? neoforge {

import java.util.List;
import net.iskaa303.dividedchunks.DividedChunks;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = DividedChunks.MOD_ID)
public final class NeoforgeGameTests {

	private NeoforgeGameTests() {
	}

	@SubscribeEvent
	public static void register(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "gametest"),
				new TestEnvironmentDefinition.AllOf(List.of()));
		TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
				environment, Identifier.withDefaultNamespace("empty"), 20, 0, true);
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "block_outside_build_height"),
				new InlineGameTest(data, GameTests::blockOutsideBuildHeight));
	}
}
//?}
