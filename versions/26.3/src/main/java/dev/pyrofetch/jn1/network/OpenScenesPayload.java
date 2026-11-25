package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ouvre le choix des scènes de la télécommande.
 *
 * <p>La liste vient du serveur, qui l'a lue dans le fichier d'animations (voir
 * {@link dev.pyrofetch.jn1.entity.CompanionScenes}) : l'écran ne propose que ce
 * que le serveur acceptera.
 *
 * @param entityId la compagne visée
 * @param scènes   les noms courts, dans l'ordre où les proposer
 * @param actuelle celle qui joue, ou vide
 * @param pièce    le nom de l'os que le réglage fait paraître, ou vide si
 *                 elle n'en a pas — le bouton est alors absent
 * @param pièceVue l'état du réglage
 * @param participe celui qui ouvre l'écran tient déjà la place de {@code steve}
 */
public record OpenScenesPayload(int entityId, List<String> scènes, String actuelle, String pièce,
                                boolean pièceVue, boolean participe) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenScenesPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("open_scenes"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenScenesPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OpenScenesPayload::entityId,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenScenesPayload::scènes,
			ByteBufCodecs.STRING_UTF8, OpenScenesPayload::actuelle,
			ByteBufCodecs.STRING_UTF8, OpenScenesPayload::pièce,
			ByteBufCodecs.BOOL, OpenScenesPayload::pièceVue,
			ByteBufCodecs.BOOL, OpenScenesPayload::participe,
			OpenScenesPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
