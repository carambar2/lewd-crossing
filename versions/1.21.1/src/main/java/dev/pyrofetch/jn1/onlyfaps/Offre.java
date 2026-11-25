package dev.pyrofetch.jn1.onlyfaps;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;

/**
 * Une photo de l'inventaire, telle qu'OnlyFaps la voit : son prix si on la
 * publiait maintenant, et ce qu'il y a dessus.
 *
 * @param ancienne prise avant qu'on relève les sujets : invendable
 */
public record Offre(int numéro, PhotoFormat format, int prix, boolean publiée, boolean ancienne, List<Sujet> sujets) {
	public static final PacketCodec<RegistryByteBuf, Offre> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, Offre::numéro,
			PhotoFormat.PACKET_CODEC, Offre::format,
			PacketCodecs.VAR_INT, Offre::prix,
			PacketCodecs.BOOL, Offre::publiée,
			PacketCodecs.BOOL, Offre::ancienne,
			Sujet.PACKET_CODEC.collect(PacketCodecs.toList()), Offre::sujets,
			Offre::new
	);
}
