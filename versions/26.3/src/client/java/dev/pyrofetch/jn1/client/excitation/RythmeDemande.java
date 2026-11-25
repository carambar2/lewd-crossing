package dev.pyrofetch.jn1.client.excitation;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Le rythme d'une demande ({@link dev.pyrofetch.jn1.entity.Demande}), réglé par
 * le partenaire pendant la scène : la touche « avancer » accélère, « reculer »
 * ralentit : le rythme est au joueur. Monté sur la fille,
 * le joueur ne marche pas : ces touches sont libres.
 *
 * <p>À gauche de la barre d'excitation, le rythme en cours et les deux touches,
 * telles que le joueur les a réglées.
 */
public final class RythmeDemande {
	private static final String[] NOMS = {"lent", "normal", "rapide"};
	private static boolean avantTenu;
	private static boolean arrièreTenu;

	private RythmeDemande() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			CompanionEntity elle = partenaire(client);
			boolean avant = client.options.keyUp.isDown();
			boolean arrière = client.options.keyDown.isDown();
			// Un appui, pas une touche tenue : un cran par appui.
			if (elle != null && avant && !avantTenu) {
				envoyer(elle, "+");
			}
			if (elle != null && arrière && !arrièreTenu) {
				envoyer(elle, "-");
			}
			avantTenu = avant;
			arrièreTenu = arrière;
		});
	}

	private static void envoyer(CompanionEntity elle, String sens) {
		ClientPlayNetworking.send(new JennyActionPayload(elle.getId(), JennyActionPayload.ACTION_RYTHME, sens));
	}

	/** La fille dont le joueur est le partenaire, dans une demande ; sinon rien. */
	@Nullable
	private static CompanionEntity partenaire(Minecraft client) {
		if (client.player == null || client.gui.screen() != null
				|| !(client.player.getVehicle() instanceof CompanionEntity elle)
				|| !elle.isScenePartner(client.player) || elle.demandeRythme() == -1) {
			return null;
		}
		return elle;
	}

	public static void render(GuiGraphicsExtractor contexte) {
		Minecraft client = Minecraft.getInstance();
		CompanionEntity elle = partenaire(client);
		if (elle == null || client.gui.hud.isHidden()) {
			return;
		}
		boolean intro = elle.demandeRythme() == CompanionEntity.INTRO;
		int niveau = Math.clamp(elle.demandeRythme(), 0, NOMS.length - 1);
		// À gauche de la barre d'excitation (ExcitationBar : x = milieu − 129).
		int droite = contexte.guiWidth() / 2 - 134;
		int bas = contexte.guiHeight() - 3;
		// Pendant l'introduction, « avancer » la passe.
		Component rythme = Component.translatable(intro ? "hud.jn1.rythme.intro" : "hud.jn1.rythme." + NOMS[niveau]);
		Component touches = intro
				? Component.translatable("hud.jn1.rythme.commencer", Component.keybind("key.forward"))
				: Component.translatable("hud.jn1.rythme.touches", Component.keybind("key.forward"), Component.keybind("key.back"));
		contexte.text(client.font, rythme,
				droite - client.font.width(rythme), bas - 20, 0xFFFF7AB0);
		contexte.text(client.font, touches,
				droite - client.font.width(touches), bas - 9, 0xFFD8D8D8);
	}
}
