package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.Demande;
import dev.pyrofetch.jn1.entity.JennyMode;
import dev.pyrofetch.jn1.network.JennyActionPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * L'écran d'une compagne, au clic main vide (0.53.0).
 *
 * <p>À gauche, elle, qui suit la souris du regard. À droite, son nom, la jauge
 * d'affection et le prochain palier, puis les boutons : Inventaire (son sac,
 * l'écran à cases d'avant), Tenue, Demande…, Câlin, Me suivre / Attendre ici.
 * Un bouton dont l'affection manque reste visible, grisé, son seuil en
 * info-bulle ; une action qu'elle ne pourra jamais tenir est absente.
 *
 * <p>0.56.0 : son humeur en haut à droite, ce qu'elle dit en le voyant venir
 * sous le palier, et un bouton « Talk » ({@link TalkScreen}) ; les boutons
 * passent sur deux colonnes pour tenir dans le même panneau.
 *
 * <p>L'affection vient du serveur à l'ouverture ({@code OpenCompanionPayload}) ;
 * le serveur revérifie chaque demande ({@code JennyActionHandler}).
 */
public class CompanionScreen extends Screen {
	private static final int BOUTON = Panneau.BOUTON;
	private static final int BOUTON_H = Panneau.BOUTON_H;
	private static final int PAS = Panneau.PAS;

	private static final int VIDE = 0xFF3A3A42;
	private static final int BAS = 0xFF8E5A6E;
	private static final int HAUT = 0xFFE0709A;

	private final CompanionEntity compagne;
	private final Portrait portrait;
	/** Deux colonnes de boutons dans la colonne de droite. */
	static final int DEMI = (Panneau.LARGEUR - Panneau.DROITE - 8 - 8) / 2;

	private final int affection;
	private final Component salut;
	private JennyMode vu;

	public CompanionScreen(CompanionEntity compagne, int affection) {
		this(compagne, affection, Component.empty());
	}

	public CompanionScreen(CompanionEntity compagne, int affection, Component salut) {
		super(compagne.getDisplayName());
		this.compagne = compagne;
		this.portrait = new Portrait(compagne);
		this.affection = affection;
		this.salut = salut;
	}

	private int gauche() {
		return Panneau.gauche(this.width);
	}

	private int haut() {
		return Panneau.haut(this.height);
	}

	@Override
	protected void init() {
		this.vu = this.compagne.getMode();
		int x0 = this.gauche() + Panneau.DROITE;
		int y0 = this.haut() + 92;
		int[] place = {0};
		java.util.function.BiFunction<Integer, Integer, int[]> où = (i, j) -> new int[] {
				x0 + (i % 2) * (DEMI + 8), y0 + (i / 2) * PAS};

		int[] p = où.apply(place[0]++, 0);
		Button parler = this.bouton(p[0], p[1], Component.translatable("screen.jn1.fille.parler"),
				!this.compagne.hasScene(), null,
				() -> Minecraft.getInstance().gui.setScreen(new TalkScreen(this.compagne, this.affection)));
		if (this.compagne.hasScene()) {
			parler.setTooltip(Tooltip.create(Component.translatable("screen.jn1.fille.occupee", this.compagne.getName())));
		}
		if (Demande.TOUTES.stream().anyMatch(d -> d.possible(this.compagne.kind()))) {
			// Toujours ouvert : l'écran des demandes montre ce qui reste à gagner.
			p = où.apply(place[0]++, 0);
			this.bouton(p[0], p[1], Component.translatable("screen.jn1.jenny.demande"), true, null,
					() -> Minecraft.getInstance().gui.setScreen(new DemandeScreen(this.compagne, this.affection)));
		}
		int câlin = PlayerProgress.hugTrust(this.compagne.kind());
		// Une fille qui n'a pas de câlin (autre squelette) : bouton absent.
		if (this.compagne.kind().saitCâliner()) {
			p = où.apply(place[0]++, 0);
			this.bouton(p[0], p[1], Component.translatable("screen.jn1.jenny.hug"), this.affection >= câlin, câlin,
					() -> this.envoyer(JennyActionPayload.ACTION_HUG, true));
		}
		if (this.compagne.kind().hasOutfits()) {
			p = où.apply(place[0]++, 0);
			this.bouton(p[0], p[1], Component.translatable("screen.jn1.jenny.change"),
					this.affection >= PlayerProgress.CHANGE_TRUST, PlayerProgress.CHANGE_TRUST,
					() -> Minecraft.getInstance().gui.setScreen(new OutfitScreen(this.compagne, this.affection)));
		}
		p = où.apply(place[0]++, 0);
		this.bouton(p[0], p[1], Component.translatable("screen.jn1.fille.inventaire"), true, null,
				() -> this.envoyer(JennyActionPayload.ACTION_INVENTAIRE, true));
		// FapGPT ne quitte pas son projecteur : le bouton n'a pas lieu d'être.
		if (!this.compagne.peutQuitterSaPlace()) {
			return;
		}
		boolean suit = this.vu == JennyMode.FOLLOW;
		p = où.apply(place[0]++, 0);
		Button suivre = this.bouton(p[0], p[1],
				Component.translatable(suit ? "screen.jn1.fille.attendre" : "screen.jn1.fille.suivre"),
				!this.compagne.isCuffed(), null, () -> this.envoyer(JennyActionPayload.ACTION_SUIVRE, false));
		if (this.compagne.isCuffed()) {
			suivre.setTooltip(Tooltip.create(Component.translatable("message.jn1.handcuffs.cannot_follow",
					this.compagne.getName())));
		}
	}

	private Button bouton(int x, int y, Component texte, boolean actif, Integer seuil, Runnable action) {
		Button bouton = Button.builder(texte, b -> action.run())
				.bounds(x, y, DEMI, BOUTON_H).build();
		bouton.active = actif;
		if (!actif && seuil != null) {
			bouton.setTooltip(Tooltip.create(Component.translatable("screen.jn1.demande.seuil", seuil,
					PlayerProgress.MAX_TRUST)));
		}
		return this.addRenderableWidget(bouton);
	}

	private void envoyer(int action, boolean fermer) {
		ClientPlayNetworking.send(new JennyActionPayload(this.compagne.getId(), action, ""));
		if (fermer) {
			this.onClose();
		}
	}

	/** Le prochain palier : le plus petit seuil au-dessus de l'affection, parmi ce qu'elle sait faire. */
	private Component prochainPalier() {
		List<Palier> paliers = new ArrayList<>();
		int câlin = PlayerProgress.hugTrust(this.compagne.kind());
		if (câlin > 0 && this.compagne.kind().saitCâliner()) {
			paliers.add(new Palier(câlin, Component.translatable("screen.jn1.jenny.hug")));
		}
		if (this.compagne.kind().hasOutfits()) {
			paliers.add(new Palier(PlayerProgress.CHANGE_TRUST, Component.translatable("screen.jn1.jenny.change")));
		}
		boolean demandes = false;
		for (Demande demande : Demande.TOUTES) {
			if (demande.possible(this.compagne.kind())) {
				paliers.add(new Palier(demande.seuil(), Component.translatable("demande.jn1." + demande.id())));
				demandes = true;
			}
		}
		if (demandes) {
			paliers.add(new Palier(PlayerProgress.BRUTAL_TRUST, Component.translatable("screen.jn1.demande.brutale")));
		}
		return paliers.stream()
				.filter(p -> p.seuil > this.affection)
				.min(java.util.Comparator.comparingInt(Palier::seuil))
				.map(p -> (Component) Component.translatable("screen.jn1.fille.prochain", p.nom, p.seuil))
				.orElse(Component.translatable(this.affection >= PlayerProgress.MAX_TRUST
						? "screen.jn1.fille.max" : "screen.jn1.fille.rien"));
	}

	private record Palier(int seuil, Component nom) {
	}

	@Override
	public void tick() {
		if (this.compagne.isRemoved() || this.minecraft.player == null
				|| this.minecraft.player.distanceToSqr(this.compagne) > 8 * 8) {
			this.onClose();
		} else if (this.compagne.getMode() != this.vu) {
			// Suivre ↔ attendre : le libellé suit ce que le serveur a accepté.
			// Le salut reste : il est gardé par l'écran, pas par init.
			this.rebuildWidgets();
		}
	}

	// Le flou du jeu d'abord, puis le panneau : flouter après l'aurait flouté lui aussi.
	@Override
	public void extractBackground(GuiGraphicsExtractor contexte, int mouseX, int mouseY, float delta) {
		super.extractBackground(contexte, mouseX, mouseY, delta);
		int x = this.gauche();
		int y = this.haut();
		Panneau.dessiner(contexte, x, y, this.portrait, mouseX, mouseY);

		// Le nom, la jauge, le prochain palier.
		int dx = x + Panneau.DROITE;
		contexte.text(this.font, this.title, dx, y + 10, 0xFFFFFFFF);
		Humeur.dessiner(contexte, this.font, this.compagne, x + Panneau.LARGEUR - 8, y + 10);
		int segments = PlayerProgress.MAX_TRUST;
		Component chiffre = Component.literal(this.affection + "/" + PlayerProgress.MAX_TRUST);
		// La jauge s'arrête avant le chiffre : elle passait dessous.
		int place = BOUTON - 10 - this.font.width(chiffre) - 6;
		int largeurSegment = Math.max(2, place / segments);
		int barreY = y + 26;
		contexte.text(this.font, "♥", dx, barreY - 1, HAUT);
		for (int i = 0; i < segments; i++) {
			int sx = dx + 10 + i * largeurSegment;
			int couleur = i < this.affection ? mélange(BAS, HAUT, i / (float) (segments - 1)) : VIDE;
			contexte.fill(sx, barreY, sx + largeurSegment - 1, barreY + 7, couleur);
		}
		contexte.text(this.font, chiffre,
				dx + BOUTON - this.font.width(chiffre), barreY - 1, 0xFFE0E0E0);
		contexte.text(this.font, this.prochainPalier(), dx, y + 40, Panneau.NOTE);
		Humeur.réplique(contexte, this.font, this.salut, dx, y + 56, Panneau.LARGEUR - Panneau.DROITE - 8, 3);
	}

	private static int mélange(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
