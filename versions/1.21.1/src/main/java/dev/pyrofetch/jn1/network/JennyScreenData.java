package dev.pyrofetch.jn1.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Ce que le client doit savoir à l'ouverture du menu, en plus des objets :
 * quelle entité, et où en est l'affection.
 *
 * @param entityId l'entité concernée, pour retrouver son nom et l'afficher
 * @param trust    0 à 10 ; le serveur reste seul juge de cette valeur
 */
public record JennyScreenData(int entityId, int trust) {
	public static final PacketCodec<RegistryByteBuf, JennyScreenData> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, JennyScreenData::entityId,
			PacketCodecs.VAR_INT, JennyScreenData::trust,
			JennyScreenData::new
	);
}
