package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** OnlyFaps.exe demande au serveur : la liste des photos, ou en publier une. */
public record OnlyFapsPayload(BlockPos terminal, int action, int numéro) implements CustomPayload {
	public static final int LISTE = 0;
	public static final int PUBLIER = 1;
	public static final Id<OnlyFapsPayload> ID = new Id<>(JN1.id("onlyfaps"));
	public static final PacketCodec<RegistryByteBuf, OnlyFapsPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OnlyFapsPayload::terminal,
			PacketCodecs.VAR_INT, OnlyFapsPayload::action,
			PacketCodecs.VAR_INT, OnlyFapsPayload::numéro,
			OnlyFapsPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
