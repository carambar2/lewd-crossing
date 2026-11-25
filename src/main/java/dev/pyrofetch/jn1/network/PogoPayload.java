package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Un rebond du pogo, dit par le client qui l'a fait, avec sa qualité (0 raté, 1 parfait). */
public record PogoPayload(float qualité) implements CustomPayload {
	public static final CustomPayload.Id<PogoPayload> ID = new CustomPayload.Id<>(JN1.id("pogo"));
	public static final PacketCodec<RegistryByteBuf, PogoPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, PogoPayload::qualité,
			PogoPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
