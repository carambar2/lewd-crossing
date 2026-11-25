package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

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
                                boolean pièceVue, boolean participe) implements CustomPayload {
	public static final CustomPayload.Id<OpenScenesPayload> ID =
			new CustomPayload.Id<>(JN1.id("open_scenes"));

	public static final PacketCodec<RegistryByteBuf, OpenScenesPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, OpenScenesPayload::entityId,
			PacketCodecs.STRING.collect(PacketCodecs.toList()), OpenScenesPayload::scènes,
			PacketCodecs.STRING, OpenScenesPayload::actuelle,
			PacketCodecs.STRING, OpenScenesPayload::pièce,
			PacketCodecs.BOOL, OpenScenesPayload::pièceVue,
			PacketCodecs.BOOL, OpenScenesPayload::participe,
			OpenScenesPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
