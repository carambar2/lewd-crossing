package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Le joueur jouit : le client fait blanchir l'écran ({@code OrgasmeEcran}). */
public record OrgasmePayload() implements CustomPacketPayload {
	public static final OrgasmePayload INSTANCE = new OrgasmePayload();
	public static final CustomPacketPayload.Type<OrgasmePayload> ID = new CustomPacketPayload.Type<>(JN1.id("orgasme"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OrgasmePayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
