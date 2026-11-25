package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce que le joueur fait dans la boutique de Gaster : acheter un article, ou
 * sortir — et Gaster s'en va, en lâchant son œuf.
 */
public record GasterShopActionPayload(int entityId, int action, int index) implements CustomPacketPayload {
	public static final int ACHETER = 0;
	public static final int SORTIR = 1;

	public static final CustomPacketPayload.Type<GasterShopActionPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("gaster_shop_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GasterShopActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GasterShopActionPayload::entityId,
			ByteBufCodecs.VAR_INT, GasterShopActionPayload::action,
			ByteBufCodecs.VAR_INT, GasterShopActionPayload::index,
			GasterShopActionPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
