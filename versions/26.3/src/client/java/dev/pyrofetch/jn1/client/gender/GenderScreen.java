package dev.pyrofetch.jn1.client.gender;

import dev.pyrofetch.jn1.network.GenderChoicePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * La question posée à la première venue dans un monde : homme ou femme. Échap
 * ne la ferme pas. Elle attend que l'écran soit libre (la question d'âge, le
 * chargement du monde) avant de s'ouvrir.
 */
public class GenderScreen extends Screen {
	private static boolean enAttente;

	public GenderScreen() {
		super(Component.translatable("screen.jn1.gender.title"));
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenGenderPayload.ID,
				(payload, context) -> enAttente = true);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> enAttente = false);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// La question revient tant qu'il n'a pas répondu : un écran ouvert
			// par-dessus (mort, menu) la faisait sinon disparaître pour la séance.
			if (enAttente && client.player != null && client.gui.screen() == null) {
				client.gui.setScreen(new GenderScreen());
			}
		});
	}

	@Override
	protected void init() {
		int x = this.width / 2;
		int y = this.height / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.gender.homme"), b -> this.choisir(false))
				.bounds(x - 104, y, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.jn1.gender.femme"), b -> this.choisir(true))
				.bounds(x + 4, y, 100, 20).build());
	}

	private void choisir(boolean femme) {
		enAttente = false;
		ClientPlayNetworking.send(new GenderChoicePayload(femme));
		this.onClose();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		super.extractRenderState(contexte, mouseX, mouseY, delta);
		contexte.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 34, 0xFFFFFFFF);
		contexte.centeredText(this.font, Component.translatable("screen.jn1.gender.hint"),
				this.width / 2, this.height / 2 - 20, 0xFFA0A0A0);
	}
}
