package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.DividedChunks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One out-of-band section, sent section by section; an empty {@code data} means remove it. */
public record CubePayload(int chunkX, int sectionY, int chunkZ, byte[] data) implements CustomPacketPayload {

	public static final Type<CubePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "cube"));

	public static final StreamCodec<FriendlyByteBuf, CubePayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.chunkX());
				buffer.writeVarInt(payload.sectionY());
				buffer.writeVarInt(payload.chunkZ());
				buffer.writeByteArray(payload.data());
			},
			buffer -> new CubePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray()));

	@Override
	public Type<CubePayload> type() {
		return TYPE;
	}
}
