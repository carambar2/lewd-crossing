package dev.pyrofetch.jn1.client.hud;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
 *
 * <p>26.3 : le monde est d'abord « relevé » (les entités, la caméra), puis
 * l'interface, puis tout est dessiné. Les ancres posées pendant la relève des
 * entités passent, à la fin de la relève du monde ({@link #retenir}), dans la
 * liste que dessine l'interface de la même image ; rien ne s'accumule si
 * l'interface est masquée (F1).
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
	private static Vec3 œil = Vec3.ZERO;
	private static boolean matrices;

	private record Ancre(Vec3 point, SpeechBubbles.Bubble bulle) {
	}

	private static final List<Ancre> ANCRES = new ArrayList<>();
	/** Les ancres de la dernière relève du monde : celles que dessine l'interface. */
	private static final List<Ancre> AFFICHÉES = new ArrayList<>();

	private BubbleOverlay() {
	}

	/** Au début d'une image : les ancres de l'image d'avant ne valent plus (interface masquée, F1). */
	public static void nouvelleImage() {
		ANCRES.clear();
	}

	/** Quand elle est dessinée : sa bulle, s'il y en a une, sera mise au-dessus d'elle. */
	public static void ancrer(Entity entité, float tickDelta) {
		SpeechBubbles.Bubble bulle = SpeechBubbles.get(entité.getId());
		if (bulle == null || bulle.opacity() <= 0.0F || bulle.lines().isEmpty()) {
			return;
		}
		Vec3 pieds = entité.getPosition(tickDelta);
		ANCRES.add(new Ancre(pieds.add(0.0, entité.getBbHeight() + HAUTEUR, 0.0), bulle));
	}

	/**
	 * À la fin de la relève du monde, d'après la caméra relevée : sa rotation, et
	 * sa projection secouée comme le jeu la secoue au dessin (balancement de la
	 * marche, coup reçu) — celle qu'avait le rendu du monde en 1.21.1.
	 */
	public static void retenir(net.minecraft.client.renderer.state.level.CameraRenderState caméra) {
		Minecraft client = Minecraft.getInstance();
		com.mojang.blaze3d.vertex.PoseStack secousse = new com.mojang.blaze3d.vertex.PoseStack();
		var entité = caméra.entityRenderState;
		if (entité.isLiving) {
			float coup = entité.hurtTime;
			if (entité.isDeadOrDying) {
				float durée = Math.min(entité.deathTime, 20.0F);
				secousse.rotateDegrees(com.mojang.math.Axis.ZP, 40.0F - 8000.0F / (durée + 200.0F));
			}
			if (coup >= 0.0F) {
				coup /= entité.hurtDuration;
				coup = net.minecraft.util.Mth.sin(coup * coup * coup * coup * (float) Math.PI);
				float sens = entité.hurtDir;
				secousse.rotateDegrees(com.mojang.math.Axis.YP, -sens);
				secousse.rotateDegrees(com.mojang.math.Axis.ZP,
						(float) (-coup * 14.0 * client.options.damageTiltStrength().get()));
				secousse.rotateDegrees(com.mojang.math.Axis.YP, sens);
			}
		}
		if (client.options.bobView().get() && entité.isPlayer) {
			float marche = entité.backwardsInterpolatedWalkDistance;
			float bob = entité.bob;
			secousse.translate(net.minecraft.util.Mth.sin(marche * (float) Math.PI) * bob * 0.5F,
					-Math.abs(net.minecraft.util.Mth.cos(marche * (float) Math.PI) * bob), 0.0F);
			secousse.rotateDegrees(com.mojang.math.Axis.ZP,
					net.minecraft.util.Mth.sin(marche * (float) Math.PI) * bob * 3.0F);
			secousse.rotateDegrees(com.mojang.math.Axis.XP,
					Math.abs(net.minecraft.util.Mth.cos(marche * (float) Math.PI - 0.2F) * bob) * 5.0F);
		}
		Matrix4f projection = new Matrix4f(caméra.projectionMatrix).mul(secousse.last().pose());
		retenir(caméra.viewRotationMatrix, projection, caméra.pos);
	}

	/** À la fin de la relève du monde : les matrices de cette image, et ses ancres. */
	public static void retenir(Matrix4f vue, Matrix4f projection, Vec3 caméra) {
		VUE.set(vue);
		PROJECTION.set(projection);
		œil = caméra;
		matrices = true;
		AFFICHÉES.clear();
		AFFICHÉES.addAll(ANCRES);
		ANCRES.clear();
	}

	public static void render(GuiGraphicsExtractor contexte) {
		Minecraft client = Minecraft.getInstance();
		if (AFFICHÉES.isEmpty()) {
			return;
		}
		if (!matrices || client.level == null) {
			AFFICHÉES.clear();
			return;
		}
		Font police = client.font;
		float largeur = contexte.guiWidth();
		float hauteur = contexte.guiHeight();
		// Les plus lointaines d'abord : une bulle proche passe devant.
		AFFICHÉES.sort(Comparator.comparingDouble((Ancre a) -> -a.point().distanceToSqr(œil)));
		for (Ancre ancre : AFFICHÉES) {
			Vec3 relatif = ancre.point().subtract(œil);
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

			HitResult mur = client.level.clip(new ClipContext(œil, ancre.point(),
					ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.player));
			float opacité = ancre.bulle().opacity() * (mur.getType() == HitResult.Type.BLOCK ? 0.35F : 1.0F);
			int alpha = Math.round(opacité * 255.0F);
			// Le jeu tient un alpha sous 4 pour opaque : la fin du fondu clignotait en blanc.
			if (alpha < 4) {
				continue;
			}
			int fond = Math.round(client.options.getBackgroundOpacity(0.4F) * opacité * 255.0F) << 24;
			int couleur = alpha << 24 | 0xFFFFFF;

			List<FormattedCharSequence> lignes = ancre.bulle().lines();
			int total = lignes.size() * INTERLIGNE;
			contexte.pose().pushMatrix();
			contexte.pose().translate(x, y);
			contexte.pose().scale(échelle, échelle);
			for (int i = 0; i < lignes.size(); i++) {
				FormattedCharSequence ligne = lignes.get(i);
				int l = police.width(ligne);
				int gauche = -l / 2;
				int haut = -total + i * INTERLIGNE;
				if (fond >>> 24 > 0) {
					contexte.fill(gauche - 1, haut - 1, gauche + l + 1, haut + INTERLIGNE - 1, fond);
				}
				contexte.text(police, ligne, gauche, haut, couleur, false);
			}
			contexte.pose().popMatrix();
		}
	}
}
