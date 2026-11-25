package dev.pyrofetch.jn1.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.Nullable;

/**
 * Des vues en trois dimensions dans un écran : les portraits des compagnes, les
 * figures du carnet.
 *
 * <p>Depuis 1.21.6, l'interface ne dessine plus de modèle 3D directement : chaque
 * vue est rendue à part dans une texture (« image dans l'image »), puis collée
 * dans l'écran. Le jeu n'a qu'une texture par sorte de vue : deux portraits de
 * la même sorte dans une image sortiraient tous deux avec le second (le
 * terminal en montre jusqu'à quatre, le carnet une figure par page). D'où
 * {@value #PLACES} places, chacune sa sorte et donc sa texture, attribuées dans
 * l'ordre de chaque image.
 *
 * <p>Le repère reçu par {@link Dessin} part du milieu de la vue, comme celui de
 * {@code InventoryScreen.renderEntityInInventory} en 1.21.1 : x à droite, y vers
 * le bas, une unité valant {@code échelle} pixels d'interface.
 */
public final class Vignettes {
	/** Ce qu'une image peut montrer de vues à la fois. */
	public static final int PLACES = 8;

	/** Le dessin d'une vue, au moment où le jeu la rend dans sa texture. */
	@FunctionalInterface
	public interface Dessin {
		void dessiner(PoseStack pile, SubmitNodeCollector envoi);
	}

	/** L'état commun des places. */
	public interface Vue extends PictureInPictureRenderState {
		Dessin dessin();
	}

	public record Vue0(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue1(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue2(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue3(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue4(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue5(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue6(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	public record Vue7(Dessin dessin, int x0, int y0, int x1, int y1, float scale,
	                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements Vue {
	}

	/** Le constructeur de chaque place : dessin, cadre, échelle, découpe, emprise. */
	@FunctionalInterface
	private interface Fabrique {
		Vue créer(Dessin dessin, int x0, int y0, int x1, int y1, float échelle,
		          @Nullable ScreenRectangle découpe, @Nullable ScreenRectangle emprise);
	}

	private static final Fabrique[] FABRIQUES = {Vue0::new, Vue1::new, Vue2::new, Vue3::new,
			Vue4::new, Vue5::new, Vue6::new, Vue7::new};
	private static final Class<?>[] SORTES = {Vue0.class, Vue1.class, Vue2.class, Vue3.class,
			Vue4.class, Vue5.class, Vue6.class, Vue7.class};

	/** L'image en cours (un extracteur d'interface neuf par image), et la prochaine place libre. */
	@Nullable
	private static GuiGraphicsExtractor image;
	private static int prochaine;

	private Vignettes() {
	}

	/** Une place par sorte de vue, chacune son rendu et sa texture. */
	public static void register() {
		for (Class<?> sorte : SORTES) {
			PictureInPictureRendererRegistry.register(contexte -> rendu(sorte));
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static PictureInPictureRenderer<?> rendu(Class<?> sorte) {
		return new Rendu(sorte);
	}

	/**
	 * Ajoute une vue au cadre {@code x0..x1, y0..y1}, à cette échelle. Rend faux
	 * s'il ne reste plus de place dans cette image.
	 */
	public static boolean ajouter(GuiGraphicsExtractor contexte, int x0, int y0, int x1, int y1, float échelle,
	                              Dessin dessin) {
		if (contexte != image) {
			image = contexte;
			prochaine = 0;
		}
		if (prochaine >= PLACES) {
			return false;
		}
		ScreenRectangle découpe = contexte.scissorStack.peek();
		ScreenRectangle emprise = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, découpe);
		contexte.guiRenderState.addPicturesInPictureState(
				FABRIQUES[prochaine++].créer(dessin, x0, y0, x1, y1, échelle, découpe, emprise));
		return true;
	}

	/** Le rendu d'une place : il appelle le dessin, le repère au milieu de la vue. */
	private static final class Rendu<T extends Vue> extends PictureInPictureRenderer<T> {
		private final Class<T> sorte;

		Rendu(Class<T> sorte) {
			this.sorte = sorte;
		}

		@Override
		public Class<T> getRenderStateClass() {
			return this.sorte;
		}

		@Override
		protected void renderToTexture(T état, PoseStack pile, SubmitNodeCollector envoi) {
			état.dessin().dessiner(pile, envoi);
		}

		@Override
		protected float getTranslateY(int hauteur, int échelleInterface) {
			return hauteur / 2.0F;
		}

		@Override
		protected String getTextureLabel() {
			return "jn1 view";
		}
	}
}
