package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ordre d'ouvrir l'éditeur d'un écran mural.
 *
 * <p>Pas de glissement de caméra, contrairement à la note : un tableau de
 * plusieurs blocs de côté se lit de là où l'on est.
 *
 * @param pos l'écran qui tient le texte de l'ensemble
 */
public record OpenPanelPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<OpenPanelPayload> ID =
			new CustomPayload.Id<>(JN1.id("open_panel"));

	public static final PacketCodec<RegistryByteBuf, OpenPanelPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenPanelPayload::pos,
			OpenPanelPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
