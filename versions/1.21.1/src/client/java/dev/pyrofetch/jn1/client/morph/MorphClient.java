package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.client.mixin.HandledScreenAccessor;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.network.MorphSyncPayload;
import dev.pyrofetch.jn1.network.OpenMorphPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Le branchement de la transformation côté client : l'état reçu, l'écran
 * ouvert par la potion, et le bouton ajouté à l'inventaire.
 */
public final class MorphClient {
	private static final int TAILLE_BOUTON = 20;

	private MorphClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(MorphSyncPayload.ID,
				(payload, context) -> context.client().execute(() -> ClientMorphs.receive(payload)));
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.MorphGesturePayload.ID,
				(payload, context) -> context.client().execute(() -> ClientMorphs.receive(payload)));
		ClientPlayNetworking.registerGlobalReceiver(OpenMorphPayload.ID,
				(payload, context) -> context.client().execute(() -> context.client().setScreen(new MorphScreen())));

		// L'inventaire de qui est transformé — ou a bu sans encore choisir —
		// porte un bouton vers l'écran de transformation, à droite du panneau.
		// Le panneau se déplace quand le livre de recettes s'ouvre : le bouton
		// le suit à chaque image.
		ScreenEvents.AFTER_INIT.register((client, screen, largeur, hauteur) -> {
			if (!(screen instanceof InventoryScreen inventaire)) {
				return;
			}
			MorphState état = ClientMorphs.self();
			if (!état.morphed() && !état.pending()) {
				return;
			}
			ButtonWidget bouton = ButtonWidget.builder(Text.literal("♥"), b -> client.setScreen(new MorphScreen()))
					.dimensions(0, 0, TAILLE_BOUTON, TAILLE_BOUTON)
					.tooltip(Tooltip.of(Text.translatable("screen.jn1.morph.title")))
					.build();
			Screens.getButtons(inventaire).add(bouton);
			ScreenEvents.beforeRender(inventaire).register((écran, context, mouseX, mouseY, delta) -> {
				HandledScreenAccessor panneau = (HandledScreenAccessor) inventaire;
				bouton.setPosition(panneau.jn1$x() + panneau.jn1$largeur() + 2, panneau.jn1$y());
			});
		});
	}
}
