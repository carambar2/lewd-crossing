package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ordre d'ouvrir l'éditeur d'un écran mural.
 *
 * <p>Pas de glissement de caméra, contrairement à la note : un tableau de
 * plusieurs blocs de côté se lit de là où l'on est.
 *
 * @param pos l'écran qui tient le texte de l'ensemble
 */
public record OpenPanelPayload(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenPanelPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("open_panel"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenPanelPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenPanelPayload::pos,
			OpenPanelPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
