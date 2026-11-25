package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ouvre le carnet de terrain : son contenu est côté client. */
public record OpenNotebookPayload() implements CustomPacketPayload {
	public static final OpenNotebookPayload INSTANCE = new OpenNotebookPayload();
	public static final CustomPacketPayload.Type<OpenNotebookPayload> ID = new CustomPacketPayload.Type<>(JN1.id("open_notebook"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenNotebookPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
