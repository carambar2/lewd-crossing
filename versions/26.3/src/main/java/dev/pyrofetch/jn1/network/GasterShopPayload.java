package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

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
		implements CustomPacketPayload {
	public static final int RIEN = 0;
	public static final int MERCI = 1;
	public static final int PAUVRE = 2;
	public static final int DÉJÀ = 3;

	public static final CustomPacketPayload.Type<GasterShopPayload> ID = new CustomPacketPayload.Type<>(JN1.id("gaster_shop"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GasterShopPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GasterShopPayload::entityId,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, GasterShopPayload::objets,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), GasterShopPayload::rangs,
			ByteBufCodecs.VAR_INT, GasterShopPayload::vendus,
			ByteBufCodecs.VAR_INT, GasterShopPayload::retour,
			GasterShopPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
