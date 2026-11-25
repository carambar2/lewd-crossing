package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Ouvre le carnet de terrain : son contenu est côté client. */
public record OpenNotebookPayload() implements CustomPayload {
	public static final OpenNotebookPayload INSTANCE = new OpenNotebookPayload();
	public static final CustomPayload.Id<OpenNotebookPayload> ID = new CustomPayload.Id<>(JN1.id("open_notebook"));
	public static final PacketCodec<RegistryByteBuf, OpenNotebookPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
