package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Demande d'achat. Le serveur revérifie le prix, le stock et la portée. */
public record ShopPurchasePayload(BlockPos pos, String entryId) implements CustomPayload {
	public static final CustomPayload.Id<ShopPurchasePayload> ID =
			new CustomPayload.Id<>(JN1.id("shop_purchase"));

	public static final PacketCodec<RegistryByteBuf, ShopPurchasePayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, ShopPurchasePayload::pos,
			PacketCodecs.STRING, ShopPurchasePayload::entryId,
			ShopPurchasePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
