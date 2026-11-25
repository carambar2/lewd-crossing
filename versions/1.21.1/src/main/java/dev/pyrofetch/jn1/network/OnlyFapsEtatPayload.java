package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.onlyfaps.Offre;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/**
 * Ce qu'OnlyFaps.exe affiche : les abonnés, les photos de l'inventaire, et
 * après une publication, le commentaire d'un abonné (une clé de langue) et le gain.
 */
public record OnlyFapsEtatPayload(int abonnés, List<Offre> offres, String commentaire, int gain) implements CustomPayload {
	public static final Id<OnlyFapsEtatPayload> ID = new Id<>(JN1.id("onlyfaps_state"));
	public static final PacketCodec<RegistryByteBuf, OnlyFapsEtatPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, OnlyFapsEtatPayload::abonnés,
			Offre.PACKET_CODEC.collect(PacketCodecs.toList()), OnlyFapsEtatPayload::offres,
			PacketCodecs.STRING, OnlyFapsEtatPayload::commentaire,
			PacketCodecs.VAR_INT, OnlyFapsEtatPayload::gain,
			OnlyFapsEtatPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
