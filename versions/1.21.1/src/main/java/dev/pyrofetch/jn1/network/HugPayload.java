package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Le serveur prévient le client qu'un câlin commence, et pour combien de temps.
 *
 * <p>Le client ne décide pas de la durée : elle est celle du geste, tenue par le
 * serveur, et l'immobilité doit s'arrêter exactement avec lui.
 *
 * @param ticks durée du geste ; zéro rend la main tout de suite
 */
public record HugPayload(int ticks) implements CustomPayload {
	public static final CustomPayload.Id<HugPayload> ID = new CustomPayload.Id<>(JN1.id("hug"));

	public static final PacketCodec<RegistryByteBuf, HugPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, HugPayload::ticks,
			HugPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
