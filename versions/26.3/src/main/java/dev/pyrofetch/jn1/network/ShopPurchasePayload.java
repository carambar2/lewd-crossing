package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Demande d'achat. Le serveur revérifie le prix, le stock et la portée. */
public record ShopPurchasePayload(BlockPos pos, String entryId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ShopPurchasePayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("shop_purchase"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShopPurchasePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ShopPurchasePayload::pos,
			ByteBufCodecs.STRING_UTF8, ShopPurchasePayload::entryId,
			ShopPurchasePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
