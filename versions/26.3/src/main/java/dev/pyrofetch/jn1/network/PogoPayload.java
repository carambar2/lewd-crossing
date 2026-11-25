package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Un rebond du pogo, dit par le client qui l'a fait, avec sa qualité (0 raté, 1 parfait). */
public record PogoPayload(float qualité) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PogoPayload> ID = new CustomPacketPayload.Type<>(JN1.id("pogo"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PogoPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, PogoPayload::qualité,
			PogoPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
