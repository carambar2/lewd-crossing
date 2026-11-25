package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le serveur prévient le client qu'un câlin commence, et pour combien de temps.
 *
 * <p>Le client ne décide pas de la durée : elle est celle du geste, tenue par le
 * serveur, et l'immobilité doit s'arrêter exactement avec lui.
 *
 * @param ticks durée du geste ; zéro rend la main tout de suite
 */
public record HugPayload(int ticks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HugPayload> ID = new CustomPacketPayload.Type<>(JN1.id("hug"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HugPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HugPayload::ticks,
			HugPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
