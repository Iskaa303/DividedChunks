package net.iskaa303.dividedchunks.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CubePayloadTest {

	@Test
	void roundTripsSection() {
		CubePayload payload = new CubePayload(-3, 1_000_000, 7, new byte[]{1, 2, 3});
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		CubePayload.STREAM_CODEC.encode(buffer, payload);
		CubePayload decoded = CubePayload.STREAM_CODEC.decode(buffer);
		assertEquals(payload.chunkX(), decoded.chunkX());
		assertEquals(payload.sectionY(), decoded.sectionY());
		assertEquals(payload.chunkZ(), decoded.chunkZ());
		assertArrayEquals(payload.data(), decoded.data());
	}

	@Test
	void roundTripsRemoval() {
		CubePayload payload = new CubePayload(1, -2_000_000, 3, new byte[0]);
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		CubePayload.STREAM_CODEC.encode(buffer, payload);
		CubePayload decoded = CubePayload.STREAM_CODEC.decode(buffer);
		assertArrayEquals(new byte[0], decoded.data());
	}
}
