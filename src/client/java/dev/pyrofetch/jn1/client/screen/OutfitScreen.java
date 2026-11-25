package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Le choix de la tenue d'une compagne, ouvert depuis son écran.
 *
 * <p>Dans le panneau de son écran ({@link Panneau}, 0.53.3) : elle à gauche,
 * qui change de tenue sous les yeux ; à droite ses tenues, dans l'ordre de sa
 * fiche ({@code CompanionKind.outfits}), celle qu'elle porte marquée. L'écran
 * suit ce que le serveur a accepté : il se reconstruit quand la tenue suivie
 * change. Échap ramène à son écran.
 */
public class OutfitScreen extends Screen {
	private final CompanionEntity compagne;
	private final Portrait portrait;
	private final int affection;
	private String vue;

	public OutfitScreen(CompanionEntity compagne, int affection) {
		super(Text.translatable("screen.jn1.outfit.title", compagne.getDisplayName()));
		this.compagne = compagne;
		this.portrait = new Portrait(compagne);
		this.affection = affection;
	}

	/** Le nom d'une tenue, par celui de son modèle. */
	public static Text nom(String modèle) {
		return Text.translatable("outfit.jn1." + modèle);
	}

	@Override
	protected void init() {
		this.vue = this.compagne.getOutfit();
		int x = Panneau.gauche(this.width) + Panneau.DROITE;
		int y = Panneau.haut(this.height) + 30;
		for (String tenue : this.compagne.kind().outfits()) {
			boolean portée = tenue.equals(this.vue);
			ButtonWidget bouton = ButtonWidget.builder(portée ? Text.literal("▶ ").append(nom(tenue)) : nom(tenue),
							b -> ClientPlayNetworking.send(new JennyActionPayload(this.compagne.getId(),
									JennyActionPayload.ACTION_OUTFIT, tenue)))
					.dimensions(x, y, Panneau.BOUTON, Panneau.BOUTON_H).build();
			bouton.active = !portée;
			this.addDrawableChild(bouton);
			y += Panneau.PAS;
		}
	}

	@Override
	public void tick() {
		if (this.compagne.isRemoved() || this.client.player == null
				|| this.client.player.squaredDistanceTo(this.compagne) > 8 * 8) {
			this.client.setScreen(null);
		} else if (!this.compagne.getOutfit().equals(this.vue)) {
			this.clearAndInit();
		}
	}

	/** Échap : retour à son écran, pas au jeu. */
	@Override
	public void close() {
		this.client.setScreen(new CompanionScreen(this.compagne, this.affection));
	}

	// Le flou du jeu d'abord, puis le panneau : flouter après l'aurait flouté lui aussi.
	@Override
	public void renderBackground(DrawContext contexte, int mouseX, int mouseY, float delta) {
		super.renderBackground(contexte, mouseX, mouseY, delta);
		int x = Panneau.gauche(this.width);
		int y = Panneau.haut(this.height);
		Panneau.dessiner(contexte, x, y, this.portrait, mouseX, mouseY);
		contexte.drawTextWithShadow(this.textRenderer, this.title, x + Panneau.DROITE, y + 10, Panneau.TITRE);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
