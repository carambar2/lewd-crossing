package dev.pyrofetch.jn1.client;

import dev.pyrofetch.jn1.client.camera.CameraSession;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;

/**
 * Les touches du viseur : changer de cadrage, régler le retardateur, fermer.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class CameraKeys {
	private static final String CATÉGORIE = "key.categories.jn1";
	private static KeyBinding format;
	private static KeyBinding retardateur;
	private static KeyBinding ranger;

	private CameraKeys() {
	}

	public static void register() {
		format = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jn1.photo_format", Type.KEYSYM, 86, "key.categories.jn1"));
		retardateur = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jn1.photo_timer", Type.KEYSYM, 66, "key.categories.jn1"));
		ranger = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jn1.photo_close", Type.KEYSYM, 71, "key.categories.jn1"));
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

	private static boolean consommer(KeyBinding touche) {
		boolean pressée = false;

		while (touche.wasPressed()) {
			pressée = true;
		}

		return pressée;
	}
}
