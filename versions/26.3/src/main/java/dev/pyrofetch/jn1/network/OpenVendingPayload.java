package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ordre d'ouvrir l'écran d'un distributeur.
 *
 * @param pos sa moitié basse : c'est elle que l'achat désignera
 */
public record OpenVendingPayload(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenVendingPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("open_vending"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenVendingPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenVendingPayload::pos,
			OpenVendingPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
