package dev.pyrofetch.jn1.client.notebook;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.ClientProgress;
import dev.pyrofetch.jn1.client.screen.Vignettes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/**
 * Le carnet de terrain : un vieux carnet de chercheur, ouvert en double page.
 *
 * <p>La première double page porte la couverture et le sommaire ; le signet
 * rouge y ramène de n'importe où. Les pages qu'un drapeau de progression n'a
 * pas encore débloquées restent blanches — le sommaire les annonce quand même.
 *
 * <p>Les figures sont de vrais blocs et de vrais objets du jeu, rendus en trois
 * dimensions ({@code renderBlockAsEntity}, {@code renderItem}) : elles tournent
 * lentement d'elles-mêmes, et on les fait tourner en les tirant à la souris.
 * Ce que dessine un rendu d'entité de bloc — vantaux d'ascenseur, texte d'un
 * écran — n'y paraît pas.
 *
 * <p>Le fond est une texture peinte (art/textures/gui/notebook.png) ; ses pages
 * sont aux emplacements de {@link #GAUCHE} et {@link #DROITE}.
 */
public class NotebookScreen extends Screen {
	private static final Identifier FOND = JN1.id("textures/gui/notebook.png");
	private static final int LARGEUR = 320;
	private static final int HAUTEUR = 200;
	/** x0, y0, x1, y1 de chaque page dans le fond. */
	private static final int[] GAUCHE = {12, 10, 156, 190};
	private static final int[] DROITE = {164, 10, 308, 190};
	private static final int MARGE = 8;
	/** Le pas des lignes du papier, et la première d'entre elles. */
	private static final int INTERLIGNE = 10;
	private static final int PREMIÈRE_LIGNE = 17;
	/** Le signet, en haut de la page de droite. */
	private static final int SIGNET_X = 270;
	private static final int SIGNET_LARGEUR = 7;
	private static final int SIGNET_HAUTEUR = 34;

	/** Un objet en figure, à la taille d'un bloc (le mode FIXED du jeu le dessine moitié moins grand). */
	private static final float OBJET = 1.8F;
	/** Comment le jeu montre un bloc seul (le contexte ordinaire, comme les blocs portés des golems). */
	private static final BlockDisplayContext FIGURE = BlockDisplayContext.create();

	private static final int ENCRE = 0xFF3A2A1C;
	private static final int ENCRE_TITRE = 0xFF6E2A1A;
	private static final int ENCRE_PÂLE = 0xFF9C8A6C;
	private static final int SURVOL = 0xFFA0341E;

	/** Rouvert là où on l'avait laissé, le temps d'une session de jeu. */
	private static int dernièrePlanche;

	private final NotebookContent contenu;
	private final int nombreDePages;
	private int planche;
	/** L'angle ajouté à la main à chaque figure, page gauche et page droite. */
	private final float[] angleManuel = new float[2];
	private int glisse = -1;
	private final List<int[]> zonesSommaire = new ArrayList<>();

	public NotebookScreen() {
		super(Component.translatable("item.jn1.notebook"));
		// Deux pages d'abord : la couverture et le sommaire.
		this.contenu = NotebookContent.load(2);
		this.nombreDePages = 2 + this.contenu.pages.size();
		this.planche = Math.min(dernièrePlanche, this.dernièrePlanche());
	}

	private int dernièrePlanche() {
		return (this.nombreDePages - 1) / 2;
	}

	private int gauche() {
		return (this.width - LARGEUR) / 2;
	}

	private int haut() {
		return (this.height - HAUTEUR) / 2;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		int x0 = this.gauche();
		int y0 = this.haut();
		context.blit(RenderPipelines.GUI_TEXTURED, FOND, x0, y0, 0, 0, LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);

		this.zonesSommaire.clear();
		this.page(context, 2 * this.planche, GAUCHE, 0, mouseX, mouseY);
		this.page(context, 2 * this.planche + 1, DROITE, 1, mouseX, mouseY);

		// Les flèches, aux coins bas, là où l'on tourne une page de papier.
		if (this.planche > 0) {
			context.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(this.surFlèche(mouseX, mouseY, false)
					? "widget/page_backward_highlighted" : "widget/page_backward"), x0 + 16, y0 + HAUTEUR - 24, 23, 13);
		}
		if (this.planche < this.dernièrePlanche()) {
			context.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(this.surFlèche(mouseX, mouseY, true)
					? "widget/page_forward_highlighted" : "widget/page_forward"), x0 + LARGEUR - 39, y0 + HAUTEUR - 24, 23, 13);
		}
	}

	/** Une page : couverture, sommaire, page de contenu ou page encore blanche. */
	private void page(GuiGraphicsExtractor context, int rang, int[] zone, int côté, int mouseX, int mouseY) {
		if (rang >= this.nombreDePages) {
			return;
		}
		int x = this.gauche() + zone[0] + MARGE;
		int y = this.haut() + zone[1];
		int largeur = zone[2] - zone[0] - 2 * MARGE;
		if (rang == 0) {
			this.texte(context, Component.translatable("notebook.jn1.cover"), x, y, largeur, true, false);
			return;
		}
		if (rang == 1) {
			this.sommaire(context, x, y, largeur, mouseX, mouseY);
			return;
		}
		// Le numéro, en bas, comme sur un carnet paginé à la main.
		String numéro = "– " + (rang - 1) + " –";
		context.text(this.font, numéro, x + (largeur - this.font.width(numéro)) / 2,
				this.haut() + zone[3] - 14, ENCRE_PÂLE, false);

		NotebookContent.Page page = this.contenu.pages.get(rang - 2);
		if (page.débloque() != null && !ClientProgress.hasFlag(page.débloque())) {
			Component blanche = Component.translatable("notebook.jn1.blank").setStyle(Style.EMPTY.withItalic(true));
			context.text(this.font, blanche, x + (largeur - this.font.width(blanche)) / 2,
					y + (zone[3] - zone[1]) / 2 - 4, ENCRE_PÂLE, false);
			return;
		}
		int bas = this.texte(context, Component.translatable("notebook.jn1." + page.texte()), x, y, largeur, false, page.alt());
		if (page.figure() != null) {
			int haut = bas + 6;
			int fond = this.haut() + zone[3] - 20;
			if (fond - haut > 24) {
				float angle = (System.currentTimeMillis() % 18000L) / 50.0F + this.angleManuel[côté];
				this.figure(context, page.figure(), x + largeur / 2.0F, (haut + fond) / 2.0F, fond - haut, angle);
			}
		}
	}

	/**
	 * Le texte d'une page, posé sur les lignes du papier. Le premier paragraphe
	 * — le titre — est à l'encre rouge. Rend la hauteur atteinte.
	 */
	private int texte(GuiGraphicsExtractor context, Component texte, int x, int y, int largeur, boolean centré, boolean alt) {
		String brut = texte.getString();
		int coupure = brut.indexOf("\n\n");
		Style style = alt ? Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt"))) : Style.EMPTY;
		List<FormattedCharSequence> titre = coupure < 0 ? List.of()
				: this.font.split(Component.literal(brut.substring(0, coupure)).setStyle(style), largeur);
		List<FormattedCharSequence> corps = this.font.split(
				Component.literal(coupure < 0 ? brut : brut.substring(coupure + 2)).setStyle(style), largeur);
		int ligne = 0;
		for (FormattedCharSequence t : titre) {
			this.ligne(context, t, x, y, largeur, ligne++, centré, ENCRE_TITRE);
		}
		if (!titre.isEmpty()) {
			ligne++;
		}
		for (FormattedCharSequence t : corps) {
			this.ligne(context, t, x, y, largeur, ligne++, centré, ENCRE);
		}
		return y + PREMIÈRE_LIGNE - 8 + ligne * INTERLIGNE;
	}

	private void ligne(GuiGraphicsExtractor context, FormattedCharSequence t, int x, int y, int largeur, int rang, boolean centré, int couleur) {
		int dx = centré ? (largeur - this.font.width(t)) / 2 : 0;
		context.text(this.font, t, x + dx, y + PREMIÈRE_LIGNE - 8 + rang * INTERLIGNE, couleur, false);
	}

	/** Le sommaire : un chapitre par ligne, son numéro de page, cliquable. */
	private void sommaire(GuiGraphicsExtractor context, int x, int y, int largeur, int mouseX, int mouseY) {
		Component titre = Component.translatable("notebook.jn1.contents");
		this.ligne(context, titre.getVisualOrderText(), x, y, largeur, 0, true, ENCRE_TITRE);
		int rang = 2;
		for (NotebookContent.Chapitre chapitre : this.contenu.chapitres) {
			int ly = y + PREMIÈRE_LIGNE - 8 + rang * INTERLIGNE;
			boolean dessus = mouseX >= x && mouseX < x + largeur && mouseY >= ly - 1 && mouseY < ly + INTERLIGNE - 1;
			String numéro = String.valueOf(chapitre.premièrePage() - 1);
			Component nom = Component.translatable("notebook.jn1.ch." + chapitre.titre());
			int couleur = dessus ? SURVOL : ENCRE;
			context.text(this.font, nom, x, ly, couleur, false);
			context.text(this.font, numéro, x + largeur - this.font.width(numéro), ly, couleur, false);
			this.zonesSommaire.add(new int[] {x, ly - 1, x + largeur, ly + INTERLIGNE - 1, chapitre.premièrePage()});
			rang++;
		}
		// Coupée à la largeur de la page : d'une seule ligne, elle en sortait (0.56.7).
		Component consigne = Component.translatable("notebook.jn1.hint").setStyle(Style.EMPTY.withItalic(true));
		int r = rang + 1;
		for (FormattedCharSequence morceau : this.font.split(consigne, largeur)) {
			this.ligne(context, morceau, x, y, largeur, r++, false, ENCRE_PÂLE);
		}
	}

	/**
	 * Une figure en trois dimensions, centrée dans la place qui reste sous le
	 * texte. L'échelle se réduit si la scène n'y tient pas.
	 *
	 * <p>Depuis 1.21.6, une vue 3D est rendue à part dans sa texture
	 * ({@link Vignettes}), une par page : la figure y est posée dans un cadre de
	 * la largeur de la page, un peu plus haut que la place, pour qu'en tournant
	 * elle ne soit pas coupée plus qu'avant. Ce qu'il faut dessiner (modèles des
	 * blocs, des objets) est relevé ici ; la vue le dessine ensuite.
	 */
	private void figure(GuiGraphicsExtractor context, NotebookContent.Figure figure, float cx, float cy, int place, float angle) {
		Minecraft client = Minecraft.getInstance();
		Vector3f centre = figure.centre();
		float étendue = 1.0F;
		for (NotebookContent.Element e : figure.éléments()) {
			étendue = Math.max(étendue, 2.0F * e.position().distance(centre) + 1.2F);
		}
		float taille = Math.min(figure.taille(), place / étendue * 1.1F);

		BlockModelResolver blocs = new BlockModelResolver(client.getModelManager());
		List<BlockModelRenderState> modèlesBlocs = new ArrayList<>();
		List<ItemStackRenderState> modèlesObjets = new ArrayList<>();
		for (NotebookContent.Element e : figure.éléments()) {
			if (e.bloc() != null) {
				BlockModelRenderState modèle = new BlockModelRenderState();
				blocs.update(modèle, e.bloc(), FIGURE);
				modèlesBlocs.add(modèle);
				modèlesObjets.add(null);
			} else if (e.objet() != null) {
				ItemStackRenderState modèle = new ItemStackRenderState();
				client.getItemModelResolver().updateForTopItem(modèle, e.objet(), ItemDisplayContext.FIXED,
						client.level, null, 0);
				modèlesBlocs.add(null);
				modèlesObjets.add(modèle);
			} else {
				modèlesBlocs.add(null);
				modèlesObjets.add(null);
			}
		}

		int demiLargeur = (GAUCHE[2] - GAUCHE[0]) / 2;
		int demiHauteur = Math.round(place * 0.7F);
		int x0 = Math.round(cx) - demiLargeur;
		int y0 = Math.round(cy) - demiHauteur;
		Vignettes.ajouter(context, x0, y0, x0 + 2 * demiLargeur, y0 + 2 * demiHauteur, taille, (matrices, envoi) -> {
			// Comme le jeu pour un objet en interface : y retourné, l'écran allant
			// vers le bas (la vue part de (s, s, −s), il faut (s, −s, s)).
			matrices.scale(1.0F, -1.0F, -1.0F);
			matrices.rotate(Axis.XP.rotationDegrees(figure.inclinaison()));
			matrices.rotate(Axis.YP.rotationDegrees(angle));
			matrices.translate(-centre.x(), -centre.y(), -centre.z());
			client.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
			List<NotebookContent.Element> éléments = figure.éléments();
			for (int i = 0; i < éléments.size(); i++) {
				NotebookContent.Element e = éléments.get(i);
				matrices.pushPose();
				matrices.translate(e.position().x(), e.position().y(), e.position().z());
				if (modèlesBlocs.get(i) != null) {
					modèlesBlocs.get(i).submit(matrices, envoi, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
				} else if (modèlesObjets.get(i) != null) {
					matrices.translate(0.5F, 0.5F, 0.5F);
					// Un objet en mode FIXED tient dans un demi-bloc : à côté d'un
					// bloc, et seul sur sa page, il paraissait minuscule (l'appareil
					// photo d'OnlyFaps, vu en jeu). Mis à la taille d'un bloc.
					matrices.scale(OBJET, OBJET, OBJET);
					modèlesObjets.get(i).submit(matrices, envoi, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
				}
				matrices.popPose();
			}
		});
	}

	private boolean surFlèche(double mouseX, double mouseY, boolean suivante) {
		int x = suivante ? this.gauche() + LARGEUR - 39 : this.gauche() + 16;
		int y = this.haut() + HAUTEUR - 24;
		return mouseX >= x && mouseX < x + 23 && mouseY >= y && mouseY < y + 13;
	}

	private void aller(int planche) {
		int cible = Math.max(0, Math.min(planche, this.dernièrePlanche()));
		if (cible != this.planche) {
			this.planche = cible;
			dernièrePlanche = cible;
			this.angleManuel[0] = 0;
			this.angleManuel[1] = 0;
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent événement, boolean doubleClic) {
		double mouseX = événement.x();
		double mouseY = événement.y();
		int button = événement.button();
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			if (this.surFlèche(mouseX, mouseY, false)) {
				this.aller(this.planche - 1);
				return true;
			}
			if (this.surFlèche(mouseX, mouseY, true)) {
				this.aller(this.planche + 1);
				return true;
			}
			int sx = this.gauche() + SIGNET_X;
			if (mouseX >= sx && mouseX < sx + SIGNET_LARGEUR && mouseY >= this.haut() && mouseY < this.haut() + SIGNET_HAUTEUR) {
				this.aller(0);
				return true;
			}
			for (int[] zone : this.zonesSommaire) {
				if (mouseX >= zone[0] && mouseX < zone[2] && mouseY >= zone[1] && mouseY < zone[3]) {
					this.aller(zone[4] / 2);
					return true;
				}
			}
			// Une prise sur une page : on tire la figure qui s'y trouve.
			this.glisse = mouseX < this.gauche() + LARGEUR / 2.0 ? 0 : 1;
		}
		return super.mouseClicked(événement, doubleClic);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent événement, double deltaX, double deltaY) {
		double mouseX = événement.x();
		double mouseY = événement.y();
		int button = événement.button();
		if (button == InputConstants.MOUSE_BUTTON_LEFT && this.glisse >= 0) {
			this.angleManuel[this.glisse] += (float) deltaX * 2.0F;
			return true;
		}
		return super.mouseDragged(événement, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent événement) {
		double mouseX = événement.x();
		double mouseY = événement.y();
		int button = événement.button();
		this.glisse = -1;
		return super.mouseReleased(événement);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		this.aller(this.planche + (vertical < 0 ? 1 : -1));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent événement) {
		int keyCode = événement.key();
		switch (keyCode) {
			case InputConstants.KEY_RIGHT, InputConstants.KEY_PAGEDOWN -> {
				this.aller(this.planche + 1);
				return true;
			}
			case InputConstants.KEY_LEFT, InputConstants.KEY_PAGEUP -> {
				this.aller(this.planche - 1);
				return true;
			}
			case InputConstants.KEY_HOME -> {
				this.aller(0);
				return true;
			}
			default -> {
				return super.keyPressed(événement);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
