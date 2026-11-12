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
		// Only the game test server opts in; a normal run must not add an unserializable test_instance.
		if (!Boolean.getBoolean("dividedchunks.gametest")) {
			return;
		}
		Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "gametest"),
				new TestEnvironmentDefinition.AllOf(List.of()));
		TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
				environment, Identifier.withDefaultNamespace("empty"), 20, 0, true);
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "block_outside_build_height"),
				new InlineGameTest(data, GameTests::blockOutsideBuildHeight));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "sky_light_under_cover"),
				new InlineGameTest(data, GameTests::skyLightUnderCover));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "sky_light_falls_under_cover"),
				new InlineGameTest(data, GameTests::skyLightFallsUnderCover));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "block_light_under_cover"),
				new InlineGameTest(data, GameTests::blockLightUnderCover));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "sky_light_falls_deep"),
				new InlineGameTest(data, GameTests::skyLightFallsDeep));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "band_section_survives_cube_removal"),
				new InlineGameTest(data, GameTests::bandSectionSurvivesCubeRemoval));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "save_stores_whole_column"),
				new InlineGameTest(data, GameTests::saveStoresWholeColumn));
		event.registerTest(
				Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "save_stores_entities_and_poi"),
				new InlineGameTest(data, GameTests::saveStoresEntitiesAndPoi));
	}
}
//?}
