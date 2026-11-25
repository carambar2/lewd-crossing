package dev.pyrofetch.jn1.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ce que le client doit savoir à l'ouverture du menu, en plus des objets :
 * quelle entité, et où en est l'affection.
 *
 * @param entityId l'entité concernée, pour retrouver son nom et l'afficher
 * @param trust    0 à 10 ; le serveur reste seul juge de cette valeur
 */
public record JennyScreenData(int entityId, int trust) {
	public static final StreamCodec<RegistryFriendlyByteBuf, JennyScreenData> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JennyScreenData::entityId,
			ByteBufCodecs.VAR_INT, JennyScreenData::trust,
			JennyScreenData::new
	);
}
