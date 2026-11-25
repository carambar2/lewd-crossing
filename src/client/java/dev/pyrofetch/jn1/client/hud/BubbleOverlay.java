package dev.pyrofetch.jn1.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Les bulles des filles, dessinées sur l'écran par-dessus le monde rendu, à
 * l'aplomb de leur tête (0.56.5 : le texte au-dessus des filles sortait
 * semi-transparent, et certaines choses passaient devant).
 *
 * <p>Jusque-là, la bulle était un texte du monde, comme une étiquette de nom :
 * sur certaines installations, les nuages se dessinaient par-dessus — fond et lettres
 * —, et les lettres sortaient grises. L'ordre de rendu du monde n'est pas le
 * même d'une installation à l'autre (optimisations, nuages, shaders) ; sur
 * d'autres, elle sortait propre. Dessinée dans l'interface, elle ne
 * dépend plus de rien de cela.
 *
 * <p>Le point au-dessus de sa tête est relevé quand la fille est dessinée (elle
 * est donc dans le champ et pas cachée par le jeu), puis projeté avec les
 * matrices du rendu qui vient de finir. La taille suit la distance comme celle
 * d'un texte du monde. Un bloc entre l'œil et la bulle la pâlit, comme le jeu
 * le fait d'une étiquette derrière un mur.
 */
public final class BubbleOverlay {
	/** Hauteur au-dessus de sa tête, en blocs. */
	private static final float HAUTEUR = 0.5F;
	/** Un pixel de police, en blocs, comme l'ancien texte du monde. */
	private static final float ÉCHELLE = 0.016F;
	/** Au plus deux pixels d'interface par pixel de police : collée à la caméra (scène vue par ses yeux), elle couvrait l'écran. */
	private static final float ÉCHELLE_MAX = 2.0F;
	private static final int INTERLIGNE = 10;

	private static final Matrix4f VUE = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static Vec3d œil = Vec3d.ZERO;
	private static boolean matrices;

	private record Ancre(Vec3d point, SpeechBubbles.Bubble bulle) {
	}

	private static final List<Ancre> ANCRES = new ArrayList<>();

	private BubbleOverlay() {
	}

	/** Au début du rendu du monde : les ancres de l'image d'avant ne valent plus (interface masquée, F1). */
	public static void nouvelleImage() {
		ANCRES.clear();
	}

	/** Quand elle est dessinée : sa bulle, s'il y en a une, sera mise au-dessus d'elle. */
	public static void ancrer(Entity entité, float tickDelta) {
		SpeechBubbles.Bubble bulle = SpeechBubbles.get(entité.getId());
		if (bulle == null || bulle.opacity() <= 0.0F || bulle.lines().isEmpty()) {
			return;
		}
		Vec3d pieds = entité.getLerpedPos(tickDelta);
		ANCRES.add(new Ancre(pieds.add(0.0, entité.getHeight() + HAUTEUR, 0.0), bulle));
	}

	/** À la fin du rendu du monde : les matrices de cette image. */
	public static void retenir(Matrix4f vue, Matrix4f projection, Vec3d caméra) {
		VUE.set(vue);
		PROJECTION.set(projection);
		œil = caméra;
		matrices = true;
	}

	public static void render(DrawContext contexte) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (ANCRES.isEmpty()) {
			return;
		}
		if (!matrices || client.world == null) {
			ANCRES.clear();
			return;
		}
		TextRenderer police = client.textRenderer;
		float largeur = contexte.getScaledWindowWidth();
		float hauteur = contexte.getScaledWindowHeight();
		// Les plus lointaines d'abord : une bulle proche passe devant.
		ANCRES.sort(Comparator.comparingDouble((Ancre a) -> -a.point().squaredDistanceTo(œil)));
		for (Ancre ancre : ANCRES) {
			Vec3d relatif = ancre.point().subtract(œil);
			Vector4f clip = new Vector4f((float) relatif.x, (float) relatif.y, (float) relatif.z, 1.0F);
			VUE.transform(clip);
			PROJECTION.transform(clip);
			if (clip.w() <= 0.05F) {
				continue;
			}
			float x = (clip.x() / clip.w() * 0.5F + 0.5F) * largeur;
			float y = (0.5F - clip.y() / clip.w() * 0.5F) * hauteur;
			float échelle = Math.min(ÉCHELLE_MAX, ÉCHELLE * PROJECTION.m11() / clip.w() * hauteur * 0.5F);
			if (échelle < 0.05F) {
				continue;
			}

			HitResult mur = client.world.raycast(new RaycastContext(œil, ancre.point(),
					RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, client.player));
			float opacité = ancre.bulle().opacity() * (mur.getType() == HitResult.Type.BLOCK ? 0.35F : 1.0F);
			int alpha = Math.round(opacité * 255.0F);
			// Le jeu tient un alpha sous 4 pour opaque : la fin du fondu clignotait en blanc.
			if (alpha < 4) {
				continue;
			}
			int fond = Math.round(client.options.getTextBackgroundOpacity(0.4F) * opacité * 255.0F) << 24;
			int couleur = alpha << 24 | 0xFFFFFF;

			List<OrderedText> lignes = ancre.bulle().lines();
			int total = lignes.size() * INTERLIGNE;
			contexte.getMatrices().push();
			contexte.getMatrices().translate(x, y, 0.0F);
			contexte.getMatrices().scale(échelle, échelle, 1.0F);
			for (int i = 0; i < lignes.size(); i++) {
				OrderedText ligne = lignes.get(i);
				int l = police.getWidth(ligne);
				int gauche = -l / 2;
				int haut = -total + i * INTERLIGNE;
				if (fond >>> 24 > 0) {
					contexte.fill(gauche - 1, haut - 1, gauche + l + 1, haut + INTERLIGNE - 1, fond);
				}
				contexte.drawText(police, ligne, gauche, haut, couleur, false);
			}
			contexte.getMatrices().pop();
		}
		ANCRES.clear();
	}
}
