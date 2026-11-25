package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * La boutique de Gaster, du serveur au client : ouvre l'écran, ou le met à
 * jour après un achat.
 *
 * @param entityId Gaster
 * @param objets   les articles du jour, dans l'ordre de l'étal
 * @param rangs    leur rang ({@code GasterShop.Rang}, ordinal), qui fait le prix
 * @param vendus   un bit par article déjà vendu
 * @param retour   la suite d'un achat : {@link #RIEN}, {@link #MERCI}, {@link #PAUVRE}, {@link #DÉJÀ}
 */
public record GasterShopPayload(int entityId, List<ItemStack> objets, List<Integer> rangs, int vendus, int retour)
		implements CustomPayload {
	public static final int RIEN = 0;
	public static final int MERCI = 1;
	public static final int PAUVRE = 2;
	public static final int DÉJÀ = 3;

	public static final CustomPayload.Id<GasterShopPayload> ID = new CustomPayload.Id<>(JN1.id("gaster_shop"));

	public static final PacketCodec<RegistryByteBuf, GasterShopPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, GasterShopPayload::entityId,
			ItemStack.OPTIONAL_LIST_PACKET_CODEC, GasterShopPayload::objets,
			PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), GasterShopPayload::rangs,
			PacketCodecs.VAR_INT, GasterShopPayload::vendus,
			PacketCodecs.VAR_INT, GasterShopPayload::retour,
			GasterShopPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
