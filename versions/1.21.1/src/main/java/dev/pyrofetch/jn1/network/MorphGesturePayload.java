package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Un geste fait par un joueur transformé, relayé à ceux qui le voient. */
public record MorphGesturePayload(UUID player, String gesture) implements CustomPayload {
	public static final CustomPayload.Id<MorphGesturePayload> ID = new CustomPayload.Id<>(JN1.id("morph_gesture"));

	public static final PacketCodec<RegistryByteBuf, MorphGesturePayload> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, MorphGesturePayload::player,
			PacketCodecs.STRING, MorphGesturePayload::gesture,
			MorphGesturePayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
