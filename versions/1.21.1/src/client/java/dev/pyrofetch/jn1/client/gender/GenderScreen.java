package dev.pyrofetch.jn1.client.gender;

import dev.pyrofetch.jn1.network.GenderChoicePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * La question posée à la première venue dans un monde : homme ou femme. Échap
 * ne la ferme pas. Elle attend que l'écran soit libre (la question d'âge, le
 * chargement du monde) avant de s'ouvrir.
 */
public class GenderScreen extends Screen {
	private static boolean enAttente;

	public GenderScreen() {
		super(Text.translatable("screen.jn1.gender.title"));
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(dev.pyrofetch.jn1.network.OpenGenderPayload.ID,
				(payload, context) -> enAttente = true);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> enAttente = false);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// La question revient tant qu'il n'a pas répondu : un écran ouvert
			// par-dessus (mort, menu) la faisait sinon disparaître pour la séance.
			if (enAttente && client.player != null && client.currentScreen == null) {
				client.setScreen(new GenderScreen());
			}
		});
	}

	@Override
	protected void init() {
		int x = this.width / 2;
		int y = this.height / 2;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.gender.homme"), b -> this.choisir(false))
				.dimensions(x - 104, y, 100, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.gender.femme"), b -> this.choisir(true))
				.dimensions(x + 4, y, 100, 20).build());
	}

	private void choisir(boolean femme) {
		enAttente = false;
		ClientPlayNetworking.send(new GenderChoicePayload(femme));
		this.close();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(DrawContext contexte, int mouseX, int mouseY, float delta) {
		super.render(contexte, mouseX, mouseY, delta);
		contexte.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 34, 0xFFFFFF);
		contexte.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.jn1.gender.hint"),
				this.width / 2, this.height / 2 - 20, 0xA0A0A0);
	}
}
