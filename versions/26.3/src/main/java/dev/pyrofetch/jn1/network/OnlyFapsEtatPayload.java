package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.onlyfaps.Offre;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ce qu'OnlyFaps.exe affiche : les abonnés, les photos de l'inventaire, et
 * après une publication, le commentaire d'un abonné (une clé de langue) et le gain.
 */
public record OnlyFapsEtatPayload(int abonnés, List<Offre> offres, String commentaire, int gain) implements CustomPacketPayload {
	public static final Type<OnlyFapsEtatPayload> ID = new Type<>(JN1.id("onlyfaps_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OnlyFapsEtatPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OnlyFapsEtatPayload::abonnés,
			Offre.PACKET_CODEC.apply(ByteBufCodecs.list()), OnlyFapsEtatPayload::offres,
			ByteBufCodecs.STRING_UTF8, OnlyFapsEtatPayload::commentaire,
			ByteBufCodecs.VAR_INT, OnlyFapsEtatPayload::gain,
			OnlyFapsEtatPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
