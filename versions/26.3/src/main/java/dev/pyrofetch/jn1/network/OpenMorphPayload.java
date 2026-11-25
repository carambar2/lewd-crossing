package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ouvre l'écran de transformation : la potion vient d'être bue. */
public record OpenMorphPayload() implements CustomPacketPayload {
	public static final OpenMorphPayload INSTANCE = new OpenMorphPayload();
	public static final CustomPacketPayload.Type<OpenMorphPayload> ID = new CustomPacketPayload.Type<>(JN1.id("open_morph"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenMorphPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
