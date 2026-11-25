package dev.pyrofetch.jn1.client.screen;

import dev.pyrofetch.jn1.network.OpenScenesPayload;
import dev.pyrofetch.jn1.network.SceneJoinPayload;
import dev.pyrofetch.jn1.network.ScenePayload;
import dev.pyrofetch.jn1.network.ScenePropPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Le choix des scènes de la télécommande : une colonne par préfixe
 * ({@code b_}, {@code d_}, {@code p_}), dans l'ordre que le serveur a donné,
 * un bouton pour arrêter, le réglage de la pièce de scène chez une compagne
 * qui en a une — absent, pas grisé, chez les autres —, et celui de la
 * participation : prendre la place de {@code steve}, ou regarder de
 * l'extérieur en gardant la main sur son personnage.
 *
 * <p>L'écran est fait pour qu'on regarde la compagne à travers : le panneau
 * est rangé à gauche, rien n'est flouté, et le jeu ne se met pas en pause — en
 * solo, la pause aurait figé l'animation qu'on vient de lancer. Il reste ouvert
 * après un choix, pour passer d'une scène à l'autre.
 */
public class SceneScreen extends Screen {
	private static final int LARGEUR_BOUTON = 58;
	private static final int HAUTEUR_BOUTON = 18;
	private static final int PAS = 20;
	private static final int ÉCART = 6;
	private static final int MARGE = 8;
	private static final int PANNEAU = 0xB0101014;
	private static final int FILET = 0xFF3A3A44;
	private static final int TITRE = 0xFFE8E8F0;
	private static final int PRÉFIXE = 0xFFA0A0B0;

	private final int entityId;
	private final Map<String, List<String>> groupes = new LinkedHashMap<>();
	private final List<ButtonWidget> boutons = new ArrayList<>();
	private final List<String> scènesDesBoutons = new ArrayList<>();
	private String actuelle;
	private final String pièce;
	private boolean pièceVue;
	private ButtonWidget boutonPièce;
	/**
	 * Prendre la place de {@code steve} : caméra à ses yeux, personnage monté
	 * sur elle. Sinon on regarde de l'extérieur, en gardant la main.
	 */
	private boolean participer;
	private ButtonWidget boutonParticiper;

	private int gauche;
	private int haut;
	private int largeur;
	private int hauteur;

	public SceneScreen(OpenScenesPayload payload) {
		super(Text.translatable("screen.jn1.scenes.title"));
		this.entityId = payload.entityId();
		this.actuelle = payload.actuelle();
		this.pièce = payload.pièce();
		this.pièceVue = payload.pièceVue();
		this.participer = payload.participe();
		for (String scène : payload.scènes()) {
			int trait = scène.indexOf('_');
			String préfixe = trait < 0 ? scène : scène.substring(0, trait + 1);
			this.groupes.computeIfAbsent(préfixe, clé -> new ArrayList<>()).add(scène);
		}
	}

	@Override
	protected void init() {
		this.boutons.clear();
		this.scènesDesBoutons.clear();
		int colonnes = Math.max(1, this.groupes.size());
		int lignes = this.groupes.values().stream().mapToInt(List::size).max().orElse(0);
		this.largeur = MARGE * 2 + colonnes * LARGEUR_BOUTON + (colonnes - 1) * ÉCART;
		int bas = this.pièce.isEmpty() ? 2 : 3;
		this.hauteur = MARGE + 12 + 12 + lignes * PAS + ÉCART + bas * PAS - (PAS - HAUTEUR_BOUTON) + MARGE;
		this.gauche = MARGE;
		this.haut = Math.max(MARGE, (this.height - this.hauteur) / 2);

		int colonne = 0;
		for (List<String> scènes : this.groupes.values()) {
			int x = this.gauche + MARGE + colonne * (LARGEUR_BOUTON + ÉCART);
			for (int i = 0; i < scènes.size(); i++) {
				String scène = scènes.get(i);
				ButtonWidget bouton = ButtonWidget.builder(Text.translatableWithFallback("scene.jn1." + scène, scène),
								b -> this.choisir(scène))
						.dimensions(x, this.haut + MARGE + 24 + i * PAS, LARGEUR_BOUTON, HAUTEUR_BOUTON)
						.build();
				this.boutons.add(this.addDrawableChild(bouton));
				this.scènesDesBoutons.add(scène);
			}
			colonne++;
		}
		int yBas = this.haut + MARGE + 24 + lignes * PAS + ÉCART;
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.jn1.scenes.stop"),
						b -> this.choisir(""))
				.dimensions(this.gauche + MARGE, yBas, this.largeur - MARGE * 2, HAUTEUR_BOUTON)
				.build());
		this.boutonPièce = null;
		int yRéglage = yBas + PAS;
		if (!this.pièce.isEmpty()) {
			this.boutonPièce = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> this.basculerPièce())
					.dimensions(this.gauche + MARGE, yRéglage, this.largeur - MARGE * 2, HAUTEUR_BOUTON)
					.build());
			yRéglage += PAS;
		}
		this.boutonParticiper = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> this.basculerParticipation())
				.dimensions(this.gauche + MARGE, yRéglage, this.largeur - MARGE * 2, HAUTEUR_BOUTON)
				.build());
		this.marquer();
	}

	private void choisir(String scène) {
		ClientPlayNetworking.send(new ScenePayload(this.entityId, scène));
		// Le serveur traite les paquets dans l'ordre : la scène joue déjà
		// quand arrive la demande d'y prendre part.
		if (!scène.isEmpty() && this.participer) {
			ClientPlayNetworking.send(new SceneJoinPayload(this.entityId, true));
		}
		this.actuelle = scène;
		this.marquer();
	}

	private void basculerParticipation() {
		this.participer = !this.participer;
		if (!this.actuelle.isEmpty()) {
			ClientPlayNetworking.send(new SceneJoinPayload(this.entityId, this.participer));
		}
		this.marquer();
	}

	private void basculerPièce() {
		this.pièceVue = !this.pièceVue;
		ClientPlayNetworking.send(new ScenePropPayload(this.entityId, this.pièceVue));
		this.marquer();
	}

	/** La scène qui joue se lit à son bouton, marqué et enfoncé. */
	private void marquer() {
		for (int i = 0; i < this.boutons.size(); i++) {
			String scène = this.scènesDesBoutons.get(i);
			boolean joue = scène.equals(this.actuelle);
			Text nom = Text.translatableWithFallback("scene.jn1." + scène, scène);
			this.boutons.get(i).setMessage(joue ? Text.literal("▶ ").append(nom) : nom);
		}
		if (this.boutonParticiper != null) {
			this.boutonParticiper.setMessage(Text.translatable(this.participer
					? "screen.jn1.scenes.join.on" : "screen.jn1.scenes.join.off"));
		}
		if (this.boutonPièce != null) {
			this.boutonPièce.setMessage(Text.translatable(this.pièceVue
					? "screen.jn1.scenes.prop.shown" : "screen.jn1.scenes.prop.hidden", this.pièce));
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// Pas de flou ni de voile sur tout l'écran : seulement le panneau.
		context.fill(this.gauche - 1, this.haut - 1, this.gauche + this.largeur + 1,
				this.haut + this.hauteur + 1, FILET);
		context.fill(this.gauche, this.haut, this.gauche + this.largeur, this.haut + this.hauteur, PANNEAU);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawText(this.textRenderer, this.title, this.gauche + MARGE, this.haut + MARGE, TITRE, false);
		int colonne = 0;
		for (String préfixe : this.groupes.keySet()) {
			int x = this.gauche + MARGE + colonne * (LARGEUR_BOUTON + ÉCART);
			context.drawText(this.textRenderer, Text.translatableWithFallback("scene.jn1.group." + préfixe, préfixe),
					x + 2, this.haut + MARGE + 13, PRÉFIXE, false);
			colonne++;
		}
	}

	/**
	 * La scène peut changer sans l'écran : la barre la mène à sa fin, puis
	 * l'arrête ; un autre joueur en choisit une. Le bouton marqué suit.
	 */
	@Override
	public void tick() {
		super.tick();
		if (this.client != null && this.client.world != null
				&& this.client.world.getEntityById(this.entityId) instanceof dev.pyrofetch.jn1.entity.CompanionEntity elle
				&& !elle.getScene().equals(this.actuelle)) {
			this.actuelle = elle.getScene();
			this.marquer();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
