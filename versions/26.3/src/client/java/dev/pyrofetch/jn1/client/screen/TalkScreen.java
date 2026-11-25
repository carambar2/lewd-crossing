package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import dev.pyrofetch.jn1.network.TalkPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Lui parler (0.56.0, bouton « Talk » de son écran) : six façons de s'adresser
 * à elle — {@code chat joke compliment flirt tease kiss} — et sa réponse
 * dessous. Le serveur décide si elle le prend bien ({@code interaction/Conversation}) ;
 * l'écran attend sa réponse avant de rouvrir les boutons. Échap ramène à son
 * écran.
 */
public class TalkScreen extends Screen {
	private static final String[] GESTES = {"chat", "joke", "compliment", "flirt", "tease", "kiss"};
	private static final int RATÉ = 0xFFE0A0A0;
	private static final int RÉUSSI = 0xFFA8E0A0;

	private final CompanionEntity compagne;
	private final Portrait portrait;
	private int affection;
	private final List<Button> boutons = new ArrayList<>();
	private Component réponse = Component.empty();
	private Component issue = Component.empty();
	private int couleurIssue;
	/** Ticks avant de rendre la main si le serveur ne répond pas. */
	private int attente;

	public TalkScreen(CompanionEntity compagne, int affection) {
		super(Component.translatable("screen.jn1.talk.title", compagne.getDisplayName()));
		this.compagne = compagne;
		this.portrait = new Portrait(compagne);
		this.affection = affection;
	}

	public CompanionEntity compagne() {
		return this.compagne;
	}

	@Override
	protected void init() {
		this.boutons.clear();
		int x0 = Panneau.gauche(this.width) + Panneau.DROITE;
		int y0 = Panneau.haut(this.height) + 40;
		for (int i = 0; i < GESTES.length; i++) {
			String geste = GESTES[i];
			Button bouton = Button.builder(Component.translatable("screen.jn1.talk." + geste), b -> {
						ClientPlayNetworking.send(new JennyActionPayload(this.compagne.getId(),
								JennyActionPayload.ACTION_PARLER, geste));
						this.attente = 40;
						this.activer(false);
					})
					.bounds(x0 + (i % 2) * (CompanionScreen.DEMI + 8), y0 + (i / 2) * Panneau.PAS,
							CompanionScreen.DEMI, Panneau.BOUTON_H).build();
			this.boutons.add(this.addRenderableWidget(bouton));
		}
		this.activer(this.attente == 0);
	}

	private void activer(boolean oui) {
		for (Button b : this.boutons) {
			b.active = oui && !this.compagne.hasScene();
		}
	}

	/** Sa réponse est arrivée. */
	public void recevoir(TalkPayload réponse) {
		this.réponse = réponse.réponse();
		this.affection = réponse.affection();
		this.issue = Component.translatable(réponse.réussi() ? "screen.jn1.talk.success" : "screen.jn1.talk.fail",
				Component.translatable("screen.jn1.talk." + réponse.geste()));
		this.couleurIssue = réponse.réussi() ? RÉUSSI : RATÉ;
		this.attente = 0;
		this.activer(true);
	}

	@Override
	public void tick() {
		if (this.compagne.isRemoved() || this.minecraft.player == null
				|| this.minecraft.player.distanceToSqr(this.compagne) > 8 * 8) {
			this.minecraft.gui.setScreen(null);
			return;
		}
		if (this.attente > 0 && --this.attente == 0) {
			this.activer(true);
		}
	}

	/** Échap : retour à son écran, pas au jeu. */
	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(new CompanionScreen(this.compagne, this.affection));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		super.extractBackground(contexte, mouseX, mouseY, delta);
		int x = Panneau.gauche(this.width);
		int y = Panneau.haut(this.height);
		Panneau.dessiner(contexte, x, y, this.portrait, mouseX, mouseY);
		int dx = x + Panneau.DROITE;
		int largeur = Panneau.LARGEUR - Panneau.DROITE - 8;
		contexte.text(this.font, this.title, dx, y + 10, Panneau.TITRE);
		Humeur.dessiner(contexte, this.font, this.compagne, x + Panneau.LARGEUR - 8, y + 10);
		contexte.text(this.font,
				Component.literal("♥ " + this.affection + "/" + PlayerProgress.MAX_TRUST), dx, y + 24, Panneau.NOTE);
		int bas = y + 40 + 3 * Panneau.PAS + 4;
		if (!this.issue.getString().isEmpty()) {
			contexte.text(this.font, this.issue, dx, bas, this.couleurIssue);
		}
		Humeur.réplique(contexte, this.font, this.réponse, dx, bas + 13, largeur, 5);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
