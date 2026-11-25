package dev.pyrofetch.jn1.onlyfaps;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Une photo de l'inventaire, telle qu'OnlyFaps la voit : son prix si on la
 * publiait maintenant, et ce qu'il y a dessus.
 *
 * @param ancienne prise avant qu'on relève les sujets : invendable
 */
public record Offre(int numéro, PhotoFormat format, int prix, boolean publiée, boolean ancienne, List<Sujet> sujets) {
	public static final StreamCodec<RegistryFriendlyByteBuf, Offre> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Offre::numéro,
			PhotoFormat.PACKET_CODEC, Offre::format,
			ByteBufCodecs.VAR_INT, Offre::prix,
			ByteBufCodecs.BOOL, Offre::publiée,
			ByteBufCodecs.BOOL, Offre::ancienne,
			Sujet.PACKET_CODEC.apply(ByteBufCodecs.list()), Offre::sujets,
			Offre::new
	);
}
