package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** OnlyFaps.exe demande au serveur : la liste des photos, ou en publier une. */
public record OnlyFapsPayload(BlockPos terminal, int action, int numéro) implements CustomPacketPayload {
	public static final int LISTE = 0;
	public static final int PUBLIER = 1;
	public static final Type<OnlyFapsPayload> ID = new Type<>(JN1.id("onlyfaps"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OnlyFapsPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OnlyFapsPayload::terminal,
			ByteBufCodecs.VAR_INT, OnlyFapsPayload::action,
			ByteBufCodecs.VAR_INT, OnlyFapsPayload::numéro,
			OnlyFapsPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
