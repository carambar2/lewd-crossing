package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.morph.MorphGesture;
import dev.pyrofetch.jn1.morph.MorphState;
import dev.pyrofetch.jn1.network.MorphRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Le choix de la transformation : ouvert par la potion, ou depuis l'inventaire.
 *
 * <p>À gauche, les compagnes et le retour à soi ; à côté, les tenues de celle
 * qu'on est devenu ; à droite de l'aperçu, ses gestes — un bouton sans objet
 * est absent, pas grisé. À droite, le joueur tel
 * qu'il est maintenant : l'écran se reconstruit à chaque changement que le
 * serveur confirme.
 */
public class MorphScreen extends Screen {
	private static final int LARGEUR_BOUTON = 120;
	private static final int HAUTEUR_BOUTON = 20;
	private static final int PAS = 24;
	private static final int TITRE = 0xFFFFFFFF;
	private static final int CONSIGNE = 0xFFD0A0C8;

	private MorphState vu;

	public MorphScreen() {
		super(Text.translatable("screen.jn1.morph.title"));
	}

	@Override
	protected void init() {
		this.vu = ClientMorphs.self();
		int x = this.width / 2 - 2 * LARGEUR_BOUTON - 16;
		int y = this.height / 2 - 70;
		for (CompanionKind kind : CompanionKind.all()) {
			boolean elle = kind.id().equals(this.vu.kind());
			Text nom = Text.translatable("girl.jn1." + kind.id());
			ButtonWidget bouton = ButtonWidget.builder(elle ? Text.literal("▶ ").append(nom) : nom,
							b -> envoyer(MorphRequestPayload.BECOME, kind.id()))
					.dimensions(x, y, LARGEUR_BOUTON, HAUTEUR_BOUTON).build();
			bouton.active = !elle;
			this.addDrawableChild(bouton);
			y += PAS;
		}
		// Les tenues de celle qu'on est devenu, en seconde colonne.
		CompanionKind actuelle = this.vu.companion();
		if (actuelle != null && actuelle.hasOutfits()) {
			int yTenue = this.height / 2 - 70;
			for (String tenue : actuelle.outfits()) {
				boolean portée = tenue.equals(this.vu.currentOutfit());
				Text nom = dev.pyrofetch.jn1.client.screen.OutfitScreen.nom(tenue);
				ButtonWidget bouton = ButtonWidget.builder(portée ? Text.literal("▶ ").append(nom) : nom,
								b -> envoyer(MorphRequestPayload.OUTFIT, tenue))
						.dimensions(x + LARGEUR_BOUTON + 6, yTenue, LARGEUR_BOUTON, HAUTEUR_BOUTON).build();
				bouton.active = !portée;
				this.addDrawableChild(bouton);
				yTenue += PAS;
			}
		}
		y += 8;
		if (this.vu.morphed()) {
			this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.morph.revert"),
							b -> envoyer(MorphRequestPayload.REVERT, ""))
					.dimensions(x, y, LARGEUR_BOUTON, HAUTEUR_BOUTON).build());
			// Les gestes qu'elle sait faire, à droite de l'aperçu ; l'écran se
			// ferme pour qu'on les voie.
			int yGeste = this.height / 2 - 70;
			for (MorphGesture geste : MorphGesture.values()) {
				if (actuelle == null || actuelle.animation(geste.role()) == null) {
					continue;
				}
				this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.morph.gesture." + geste.id()),
								b -> {
									envoyer(MorphRequestPayload.GESTURE, geste.id());
									this.close();
								})
						.dimensions(this.width / 2 + 130, yGeste, LARGEUR_BOUTON, HAUTEUR_BOUTON).build());
				yGeste += PAS;
			}
		}
	}

	private static void envoyer(int action, String kind) {
		ClientPlayNetworking.send(new MorphRequestPayload(action, kind));
	}

	@Override
	public void tick() {
		// Le serveur a confirmé un changement : les boutons suivent.
		if (!ClientMorphs.self().equals(this.vu)) {
			this.clearAndInit();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 100, TITRE);
		if (!this.vu.morphed()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.jn1.morph.choose"),
					this.width / 2, this.height / 2 - 86, CONSIGNE);
		}
		if (this.client != null && this.client.player != null) {
			int gauche = this.width / 2 + 10;
			int haut = this.height / 2 - 80;
			dev.pyrofetch.jn1.client.screen.Portrait.dessiner(context, gauche, haut, gauche + 110, haut + 160, 60, 0.0625F,
					mouseX, mouseY, this.client.player);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
