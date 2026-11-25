package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Ouvre l'écran de transformation : la potion vient d'être bue. */
public record OpenMorphPayload() implements CustomPayload {
	public static final OpenMorphPayload INSTANCE = new OpenMorphPayload();
	public static final CustomPayload.Id<OpenMorphPayload> ID = new CustomPayload.Id<>(JN1.id("open_morph"));
	public static final PacketCodec<RegistryByteBuf, OpenMorphPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
