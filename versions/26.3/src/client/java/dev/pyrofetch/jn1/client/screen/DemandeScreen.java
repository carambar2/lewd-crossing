package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.Demande;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ce qu'on peut lui demander, ouvert depuis son écran (bouton « Demande »).
 *
 * <p>Dans le panneau de son écran ({@link Panneau}, 0.53.3) : elle à gauche, à
 * droite les {@link Demande}s dans l'ordre où elles se débloquent. Celles dont
 * l'affection manque restent visibles, grisées, avec le seuil — savoir qu'elles
 * existent fait partie du jeu. Celles que la fille ne saura jamais jouer sont
 * absentes. En dessous, « doucement » ou « brutalement », qui ne s'ouvre qu'à
 * l'affection pleine. Échap ramène à son écran.
 *
 * <p>Le serveur revérifie tout ({@code JennyActionHandler}) : l'affection
 * affichée ici n'est que celle que le menu a reçue.
 */
public class DemandeScreen extends Screen {
	private final CompanionEntity compagne;
	private final Portrait portrait;
	private final int affection;
	private boolean brutale;

	public DemandeScreen(CompanionEntity compagne, int affection) {
		super(Component.translatable("screen.jn1.demande.title", compagne.getDisplayName()));
		this.compagne = compagne;
		this.portrait = new Portrait(compagne);
		this.affection = affection;
	}

	@Override
	protected void init() {
		int x = Panneau.gauche(this.width) + Panneau.DROITE;
		int y = Panneau.haut(this.height) + 30;
		for (Demande demande : Demande.TOUTES) {
			if (!demande.possible(this.compagne.kind())) {
				continue;
			}
			boolean ouverte = this.affection >= demande.seuil();
			Component nom = Component.translatable("demande.jn1." + demande.id());
			if (!ouverte) {
				nom = nom.copy().append(Component.literal("  ♥ " + demande.seuil()).withStyle(ChatFormatting.DARK_RED));
			}
			Button bouton = Button.builder(nom, b -> {
						ClientPlayNetworking.send(new JennyActionPayload(this.compagne.getId(),
								JennyActionPayload.ACTION_DEMANDE, demande.id() + (this.brutale ? "!" : "")));
						this.minecraft.gui.setScreen(null);
					})
					.bounds(x, y, Panneau.BOUTON, Panneau.BOUTON_H).build();
			bouton.active = ouverte;
			if (!ouverte) {
				bouton.setTooltip(Tooltip.create(Component.translatable("screen.jn1.demande.seuil", demande.seuil(),
						PlayerProgress.MAX_TRUST)));
			}
			this.addRenderableWidget(bouton);
			y += Panneau.PAS;
		}
		// Doucement ou brutalement : à l'affection pleine seulement.
		boolean permise = this.affection >= PlayerProgress.BRUTAL_TRUST;
		Button manière = Button.builder(this.libelléManière(permise), b -> {
					this.brutale = !this.brutale;
					b.setMessage(this.libelléManière(true));
				})
				.bounds(x, y + 6, Panneau.BOUTON, Panneau.BOUTON_H).build();
		manière.active = permise;
		if (!permise) {
			manière.setTooltip(Tooltip.create(Component.translatable("screen.jn1.demande.seuil", PlayerProgress.BRUTAL_TRUST,
					PlayerProgress.MAX_TRUST)));
		}
		this.addRenderableWidget(manière);
	}

	/**
	 * Ouvert : « Pace: Gently » ou « Pace: Roughly! », qu'un clic échange.
	 * Fermé : ce qui est fermé, et son seuil — « Roughly! ♥ 20 ». Jusqu'en
	 * 0.56.6, fermé, il disait « Gently ♥ 20 » : on lisait que la douceur
	 * demandait 20 d'affection (vu en jeu).
	 */
	private Component libelléManière(boolean permise) {
		if (!permise) {
			return Component.translatable("screen.jn1.demande.brutale").copy()
					.append(Component.literal("  ♥ " + PlayerProgress.BRUTAL_TRUST).withStyle(ChatFormatting.DARK_RED));
		}
		return Component.translatable("screen.jn1.demande.manière",
				Component.translatable(this.brutale ? "screen.jn1.demande.brutale" : "screen.jn1.demande.douce"));
	}

	@Override
	public void tick() {
		if (this.compagne.isRemoved() || this.minecraft.player == null
				|| this.minecraft.player.distanceToSqr(this.compagne) > 8 * 8) {
			this.minecraft.gui.setScreen(null);
		}
	}

	/** Échap : retour à son écran, pas au jeu. */
	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(new CompanionScreen(this.compagne, this.affection));
	}

	// Le flou du jeu d'abord, puis le panneau : flouter après l'aurait flouté lui aussi.
	@Override
	public void extractBackground(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		super.extractBackground(contexte, mouseX, mouseY, delta);
		int x = Panneau.gauche(this.width);
		int y = Panneau.haut(this.height);
		Panneau.dessiner(contexte, x, y, this.portrait, mouseX, mouseY);
		contexte.text(this.font, this.title, x + Panneau.DROITE, y + 10, Panneau.TITRE);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
