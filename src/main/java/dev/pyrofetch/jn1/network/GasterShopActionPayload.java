package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Ce que le joueur fait dans la boutique de Gaster : acheter un article, ou
 * sortir — et Gaster s'en va, en lâchant son œuf.
 */
public record GasterShopActionPayload(int entityId, int action, int index) implements CustomPayload {
	public static final int ACHETER = 0;
	public static final int SORTIR = 1;

	public static final CustomPayload.Id<GasterShopActionPayload> ID =
			new CustomPayload.Id<>(JN1.id("gaster_shop_action"));

	public static final PacketCodec<RegistryByteBuf, GasterShopActionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, GasterShopActionPayload::entityId,
			PacketCodecs.VAR_INT, GasterShopActionPayload::action,
			PacketCodecs.VAR_INT, GasterShopActionPayload::index,
			GasterShopActionPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
