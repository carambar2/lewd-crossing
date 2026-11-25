package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Le joueur jouit : le client fait blanchir l'écran ({@code OrgasmeEcran}). */
public record OrgasmePayload() implements CustomPayload {
	public static final OrgasmePayload INSTANCE = new OrgasmePayload();
	public static final CustomPayload.Id<OrgasmePayload> ID = new CustomPayload.Id<>(JN1.id("orgasme"));
	public static final PacketCodec<RegistryByteBuf, OrgasmePayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
