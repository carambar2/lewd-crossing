package dev.pyrofetch.jn1.client.excitation;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
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
			boolean avant = client.options.forwardKey.isPressed();
			boolean arrière = client.options.backKey.isPressed();
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
	private static CompanionEntity partenaire(MinecraftClient client) {
		if (client.player == null || client.currentScreen != null
				|| !(client.player.getVehicle() instanceof CompanionEntity elle)
				|| !elle.isScenePartner(client.player) || elle.demandeRythme() == -1) {
			return null;
		}
		return elle;
	}

	public static void render(DrawContext contexte) {
		MinecraftClient client = MinecraftClient.getInstance();
		CompanionEntity elle = partenaire(client);
		if (elle == null || client.options.hudHidden) {
			return;
		}
		boolean intro = elle.demandeRythme() == CompanionEntity.INTRO;
		int niveau = Math.clamp(elle.demandeRythme(), 0, NOMS.length - 1);
		// À gauche de la barre d'excitation (ExcitationBar : x = milieu − 129).
		int droite = contexte.getScaledWindowWidth() / 2 - 134;
		int bas = contexte.getScaledWindowHeight() - 3;
		// Pendant l'introduction, « avancer » la passe.
		Text rythme = Text.translatable(intro ? "hud.jn1.rythme.intro" : "hud.jn1.rythme." + NOMS[niveau]);
		Text touches = intro
				? Text.translatable("hud.jn1.rythme.commencer", Text.keybind("key.forward"))
				: Text.translatable("hud.jn1.rythme.touches", Text.keybind("key.forward"), Text.keybind("key.back"));
		contexte.drawTextWithShadow(client.textRenderer, rythme,
				droite - client.textRenderer.getWidth(rythme), bas - 20, 0xFFFF7AB0);
		contexte.drawTextWithShadow(client.textRenderer, touches,
				droite - client.textRenderer.getWidth(touches), bas - 9, 0xFFD8D8D8);
	}
}
