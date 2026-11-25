package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Ordre d'ouvrir l'écran d'un distributeur.
 *
 * @param pos sa moitié basse : c'est elle que l'achat désignera
 */
public record OpenVendingPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<OpenVendingPayload> ID =
			new CustomPayload.Id<>(JN1.id("open_vending"));

	public static final PacketCodec<RegistryByteBuf, OpenVendingPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenVendingPayload::pos,
			OpenVendingPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
