package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.client.mixin.HandledScreenAccessor;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.network.MorphSyncPayload;
import dev.pyrofetch.jn1.network.OpenMorphPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

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
				(payload, context) -> context.client().execute(() -> context.client().gui.setScreen(new MorphScreen())));

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
			Button bouton = Button.builder(Component.literal("♥"), b -> client.gui.setScreen(new MorphScreen()))
					.bounds(0, 0, TAILLE_BOUTON, TAILLE_BOUTON)
					.tooltip(Tooltip.create(Component.translatable("screen.jn1.morph.title")))
					.build();
			Screens.getWidgets(inventaire).add(bouton);
			ScreenEvents.beforeExtract(inventaire).register((écran, context, mouseX, mouseY, delta) -> {
				HandledScreenAccessor panneau = (HandledScreenAccessor) inventaire;
				bouton.setPosition(panneau.jn1$x() + panneau.jn1$largeur() + 2, panneau.jn1$y());
			});
		});
	}
}
