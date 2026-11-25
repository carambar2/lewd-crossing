package dev.pyrofetch.jn1.client.notebook;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.ClientProgress;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

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
		super(Text.translatable("item.jn1.notebook"));
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
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x0 = this.gauche();
		int y0 = this.haut();
		context.drawTexture(FOND, x0, y0, 0, 0, LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);

		this.zonesSommaire.clear();
		this.page(context, 2 * this.planche, GAUCHE, 0, mouseX, mouseY);
		this.page(context, 2 * this.planche + 1, DROITE, 1, mouseX, mouseY);

		// Les flèches, aux coins bas, là où l'on tourne une page de papier.
		if (this.planche > 0) {
			context.drawGuiTexture(Identifier.ofVanilla(this.surFlèche(mouseX, mouseY, false)
					? "widget/page_backward_highlighted" : "widget/page_backward"), x0 + 16, y0 + HAUTEUR - 24, 23, 13);
		}
		if (this.planche < this.dernièrePlanche()) {
			context.drawGuiTexture(Identifier.ofVanilla(this.surFlèche(mouseX, mouseY, true)
					? "widget/page_forward_highlighted" : "widget/page_forward"), x0 + LARGEUR - 39, y0 + HAUTEUR - 24, 23, 13);
		}
	}

	/** Une page : couverture, sommaire, page de contenu ou page encore blanche. */
	private void page(DrawContext context, int rang, int[] zone, int côté, int mouseX, int mouseY) {
		if (rang >= this.nombreDePages) {
			return;
		}
		int x = this.gauche() + zone[0] + MARGE;
		int y = this.haut() + zone[1];
		int largeur = zone[2] - zone[0] - 2 * MARGE;
		if (rang == 0) {
			this.texte(context, Text.translatable("notebook.jn1.cover"), x, y, largeur, true, false);
			return;
		}
		if (rang == 1) {
			this.sommaire(context, x, y, largeur, mouseX, mouseY);
			return;
		}
		// Le numéro, en bas, comme sur un carnet paginé à la main.
		String numéro = "– " + (rang - 1) + " –";
		context.drawText(this.textRenderer, numéro, x + (largeur - this.textRenderer.getWidth(numéro)) / 2,
				this.haut() + zone[3] - 14, ENCRE_PÂLE, false);

		NotebookContent.Page page = this.contenu.pages.get(rang - 2);
		if (page.débloque() != null && !ClientProgress.hasFlag(page.débloque())) {
			Text blanche = Text.translatable("notebook.jn1.blank").setStyle(Style.EMPTY.withItalic(true));
			context.drawText(this.textRenderer, blanche, x + (largeur - this.textRenderer.getWidth(blanche)) / 2,
					y + (zone[3] - zone[1]) / 2 - 4, ENCRE_PÂLE, false);
			return;
		}
		int bas = this.texte(context, Text.translatable("notebook.jn1." + page.texte()), x, y, largeur, false, page.alt());
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
	private int texte(DrawContext context, Text texte, int x, int y, int largeur, boolean centré, boolean alt) {
		String brut = texte.getString();
		int coupure = brut.indexOf("\n\n");
		Style style = alt ? Style.EMPTY.withFont(Identifier.ofVanilla("alt")) : Style.EMPTY;
		List<OrderedText> titre = coupure < 0 ? List.of()
				: this.textRenderer.wrapLines(Text.literal(brut.substring(0, coupure)).setStyle(style), largeur);
		List<OrderedText> corps = this.textRenderer.wrapLines(
				Text.literal(coupure < 0 ? brut : brut.substring(coupure + 2)).setStyle(style), largeur);
		int ligne = 0;
		for (OrderedText t : titre) {
			this.ligne(context, t, x, y, largeur, ligne++, centré, ENCRE_TITRE);
		}
		if (!titre.isEmpty()) {
			ligne++;
		}
		for (OrderedText t : corps) {
			this.ligne(context, t, x, y, largeur, ligne++, centré, ENCRE);
		}
		return y + PREMIÈRE_LIGNE - 8 + ligne * INTERLIGNE;
	}

	private void ligne(DrawContext context, OrderedText t, int x, int y, int largeur, int rang, boolean centré, int couleur) {
		int dx = centré ? (largeur - this.textRenderer.getWidth(t)) / 2 : 0;
		context.drawText(this.textRenderer, t, x + dx, y + PREMIÈRE_LIGNE - 8 + rang * INTERLIGNE, couleur, false);
	}

	/** Le sommaire : un chapitre par ligne, son numéro de page, cliquable. */
	private void sommaire(DrawContext context, int x, int y, int largeur, int mouseX, int mouseY) {
		Text titre = Text.translatable("notebook.jn1.contents");
		this.ligne(context, titre.asOrderedText(), x, y, largeur, 0, true, ENCRE_TITRE);
		int rang = 2;
		for (NotebookContent.Chapitre chapitre : this.contenu.chapitres) {
			int ly = y + PREMIÈRE_LIGNE - 8 + rang * INTERLIGNE;
			boolean dessus = mouseX >= x && mouseX < x + largeur && mouseY >= ly - 1 && mouseY < ly + INTERLIGNE - 1;
			String numéro = String.valueOf(chapitre.premièrePage() - 1);
			Text nom = Text.translatable("notebook.jn1.ch." + chapitre.titre());
			int couleur = dessus ? SURVOL : ENCRE;
			context.drawText(this.textRenderer, nom, x, ly, couleur, false);
			context.drawText(this.textRenderer, numéro, x + largeur - this.textRenderer.getWidth(numéro), ly, couleur, false);
			this.zonesSommaire.add(new int[] {x, ly - 1, x + largeur, ly + INTERLIGNE - 1, chapitre.premièrePage()});
			rang++;
		}
		// Coupée à la largeur de la page : d'une seule ligne, elle en sortait (0.56.7).
		Text consigne = Text.translatable("notebook.jn1.hint").setStyle(Style.EMPTY.withItalic(true));
		int r = rang + 1;
		for (OrderedText morceau : this.textRenderer.wrapLines(consigne, largeur)) {
			this.ligne(context, morceau, x, y, largeur, r++, false, ENCRE_PÂLE);
		}
	}

	/**
	 * Une figure en trois dimensions, centrée dans la place qui reste sous le
	 * texte. L'échelle se réduit si la scène n'y tient pas.
	 */
	private void figure(DrawContext context, NotebookContent.Figure figure, float cx, float cy, int place, float angle) {
		MinecraftClient client = MinecraftClient.getInstance();
		Vector3f centre = figure.centre();
		float étendue = 1.0F;
		for (NotebookContent.Element e : figure.éléments()) {
			étendue = Math.max(étendue, 2.0F * e.position().distance(centre) + 1.2F);
		}
		float taille = Math.min(figure.taille(), place / étendue * 1.1F);

		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(cx, cy, 150.0F);
		// Comme le jeu pour un objet en interface : y retourné, l'écran allant vers le bas.
		matrices.scale(taille, -taille, taille);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(figure.inclinaison()));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
		matrices.translate(-centre.x(), -centre.y(), -centre.z());
		DiffuseLighting.enableGuiDepthLighting();
		for (NotebookContent.Element e : figure.éléments()) {
			matrices.push();
			matrices.translate(e.position().x(), e.position().y(), e.position().z());
			if (e.bloc() != null) {
				client.getBlockRenderManager().renderBlockAsEntity(e.bloc(), matrices, context.getVertexConsumers(),
						LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
			} else if (e.objet() != null) {
				matrices.translate(0.5F, 0.5F, 0.5F);
				// Un objet en mode FIXED tient dans un demi-bloc : à côté d'un
				// bloc, et seul sur sa page, il paraissait minuscule (l'appareil
				// photo d'OnlyFaps, vu en jeu). Mis à la taille d'un bloc.
				matrices.scale(OBJET, OBJET, OBJET);
				client.getItemRenderer().renderItem(e.objet(), ModelTransformationMode.FIXED,
						LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, matrices,
						context.getVertexConsumers(), client.world, 0);
			}
			matrices.pop();
		}
		context.draw();
		matrices.pop();
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
			MinecraftClient.getInstance().getSoundManager().play(
					PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
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
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (button == 0 && this.glisse >= 0) {
			this.angleManuel[this.glisse] += (float) deltaX * 2.0F;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.glisse = -1;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		this.aller(this.planche + (vertical < 0 ? 1 : -1));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
				this.aller(this.planche + 1);
				return true;
			}
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
				this.aller(this.planche - 1);
				return true;
			}
			case GLFW.GLFW_KEY_HOME -> {
				this.aller(0);
				return true;
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
