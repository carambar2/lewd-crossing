package dev.pyrofetch.jn1.network;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Un geste demandé depuis le menu de Jenny.
 *
 * <p>Le client n'envoie que l'intention : le serveur vérifie l'affection, la
 * distance et l'entité avant d'obéir.
 *
 * @param entityId à qui l'on s'adresse
 * @param action   {@link #ACTION_HUG} ou {@link #ACTION_OUTFIT}
 * @param argument la tenue, pour {@link #ACTION_OUTFIT} ; la demande, pour {@link #ACTION_DEMANDE} ;
 *                 le sens, pour {@link #ACTION_RYTHME} ; vide sinon
 */
public record JennyActionPayload(int entityId, int action, String argument) implements CustomPacketPayload {
	public static final int ACTION_HUG = 0;
	/** Lui fait porter une autre de ses tenues. */
	public static final int ACTION_OUTFIT = 1;
	/** Ouvrait le choix des scènes ; plus reçu depuis 0.52.0 (la télécommande seule l'ouvre). Numéro gardé. */
	public static final int ACTION_SCENES = 2;
	/** Une {@link dev.pyrofetch.jn1.entity.Demande} : l'argument est son nom, suivi de « ! » si elle est brutale. */
	public static final int ACTION_DEMANDE = 3;
	/** Le rythme de la demande en cours : l'argument est « + » ou « - ». */
	public static final int ACTION_RYTHME = 4;
	/** Ouvre son sac (l'écran à cases). */
	public static final int ACTION_INVENTAIRE = 5;
	/** Elle suit, ou elle attend là. */
	public static final int ACTION_SUIVRE = 6;
	/** Lui parler (« Talk ») : l'argument est l'interaction ({@code interaction/Conversation.Geste}). */
	public static final int ACTION_PARLER = 7;

	public static final CustomPacketPayload.Type<JennyActionPayload> ID =
			new CustomPacketPayload.Type<>(JN1.id("jenny_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, JennyActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JennyActionPayload::entityId,
			ByteBufCodecs.VAR_INT, JennyActionPayload::action,
			ByteBufCodecs.STRING_UTF8, JennyActionPayload::argument,
			JennyActionPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
