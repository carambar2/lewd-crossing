package dev.pyrofetch.jn1.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.camera.CameraSession;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Les touches du viseur : changer de cadrage, régler le retardateur, fermer.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class CameraKeys {
	/**
	 * La rubrique des touches du mod. Depuis 1.21.9, une rubrique est un
	 * identifiant enregistré, et son nom se lit à la clé
	 * {@code key.category.jn1.main} (l'ancienne était {@code key.categories.jn1}).
	 */
	public static final KeyMapping.Category CATÉGORIE = KeyMapping.Category.register(JN1.id("main"));
	private static KeyMapping format;
	private static KeyMapping retardateur;
	private static KeyMapping ranger;

	private CameraKeys() {
	}

	public static void register() {
		// V, B et G, comme en 1.21.1 (les codes de touches sont ceux de SDL en 26.3).
		format = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jn1.photo_format",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATÉGORIE));
		retardateur = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jn1.photo_timer",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATÉGORIE));
		ranger = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jn1.photo_close",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATÉGORIE));
	}

	public static void tick() {
		if (format != null) {
			boolean changeFormat = consommer(format);
			boolean changeRetard = consommer(retardateur);
			boolean sortir = consommer(ranger);
			if (CameraSession.isActive()) {
				if (changeFormat) {
					CameraSession.formatSuivant();
				}

				if (changeRetard) {
					CameraSession.retardSuivant();
				}

				if (sortir) {
					CameraSession.leave();
				}
			}
		}
	}

	private static boolean consommer(KeyMapping touche) {
		boolean pressée = false;

		while (touche.consumeClick()) {
			pressée = true;
		}

		return pressée;
	}
}
