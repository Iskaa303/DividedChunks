package net.iskaa303.dividedchunks.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * A {@link GameTestInstance} that runs an in-code body instead of a registered test function.
 */
public final class InlineGameTest extends GameTestInstance {

	private static final MapCodec<InlineGameTest> CODEC = MapCodec.unit((InlineGameTest) null);

	private final Consumer<GameTestHelper> body;

	public InlineGameTest(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> body) {
		super(info);
		this.body = body;
	}

	@Override
	public void run(GameTestHelper helper) {
		this.body.accept(helper);
	}

	@Override
	public MapCodec<? extends GameTestInstance> codec() {
		return CODEC;
	}

	@Override
	protected MutableComponent typeDescription() {
		return Component.literal("dividedchunks:inline");
	}
}
