package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.rift.RiftEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * La faille : une déchirure noire, cernée de blanc, qui fait face à celui qui
 * la regarde.
 *
 * <p>Elle est découpée en bandes horizontales, et de temps en temps une bande
 * glisse de côté puis revient : c'est l'image qui bégaie, pas la faille qui
 * bouge. Le décalage dépend du temps et du numéro de la bande, pas d'un tirage
 * au hasard par image, sans quoi elle grésillerait au lieu de bégayer.
 *
 * <p>Toujours à pleine lumière : elle ne reçoit pas celle du monde, elle est
 * même ce qu'il y a de plus visible dans une nuit noire.
 *
 * <p>En 26.3, ce qu'il faut de la faille est relevé à l'image ({@link État}),
 * puis le dessin est soumis au jeu.
 */
public class RiftRenderer extends EntityRenderer<RiftEntity, RiftRenderer.État> {
	/** Ce que le dessin lisait sur la faille. */
	public static class État extends EntityRenderState {
		float ouverture;
		float temps;
		float hauteur;
		float largeur;
		int id;
	}

	private static final Identifier TEXTURE = JN1.id("textures/entity/rift.png");
	private static final int BANDES = 26;
	private static final float LARGEUR = 1.15F;

	public RiftRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(RiftEntity faille, État état, float tickDelta) {
		super.extractRenderState(faille, état, tickDelta);
		état.ouverture = faille.ouverture(tickDelta);
		état.temps = faille.tickCount + tickDelta;
		état.hauteur = faille.getBbHeight();
		état.largeur = faille.getBbWidth();
		état.id = faille.getId();
	}

	@Override
	public void submit(État état, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		float ouverture = état.ouverture;
		if (ouverture <= 0.0F) {
			return;
		}
		matrices.pushPose();
		// Debout, tournée vers la caméra autour de la verticale seulement.
		matrices.rotate(Axis.YP.rotationDegrees(180.0F - caméra.yRot));
		tâches.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(TEXTURE),
				(entrée, tampon) -> dessiner(état, entrée, tampon));
		matrices.popPose();
		super.submit(état, matrices, tâches, caméra);
	}

	private static void dessiner(État état, PoseStack.Pose entrée, VertexConsumer tampon) {
		float ouverture = état.ouverture;
		float temps = état.temps;
		float hauteur = état.hauteur;
		// Plus de bandes pour une grande faille : leur pas reste le même.
		int bandes = Math.max(BANDES, Math.round(BANDES * hauteur / RiftEntity.TAILLE_DÉFAUT));
		// Elle s'ouvre par le milieu : la hauteur vient d'abord, la largeur suit.
		float hauteurVue = hauteur * Mth.clamp(ouverture * 1.6F, 0.0F, 1.0F);
		float bas = (hauteur - hauteurVue) / 2.0F;
		int pas = (int) (temps / 3.0F);

		for (int bande = 0; bande < bandes; bande++) {
			float v0 = bande / (float) bandes;
			float v1 = (bande + 1) / (float) bandes;
			float yHaut = bas + hauteurVue * (1.0F - v0);
			float yBas = bas + hauteurVue * (1.0F - v1);

			// La respiration : chaque bande enfle un peu, en décalé.
			float largeur = LARGEUR * état.largeur * ouverture
					* (1.0F + 0.05F * Mth.sin(temps * 0.12F + bande * 0.45F));
			float décalage = 0.0F;
			long empreinte = Mth.getSeed(bande, pas, état.id);
			if ((empreinte & 0xFF) < 22) {
				décalage = (((empreinte >> 8) & 0xFF) / 255.0F - 0.5F) * 0.45F * ouverture * état.largeur;
			}
			float gauche = -largeur / 2.0F + décalage;
			float droite = largeur / 2.0F + décalage;

			sommet(tampon, entrée, gauche, yBas, 0.0F, v1);
			sommet(tampon, entrée, droite, yBas, 1.0F, v1);
			sommet(tampon, entrée, droite, yHaut, 1.0F, v0);
			sommet(tampon, entrée, gauche, yHaut, 0.0F, v0);
		}
	}

	private static void sommet(VertexConsumer tampon, PoseStack.Pose entrée, float x, float y, float u, float v) {
		tampon.addVertex(entrée, x, y, 0.0F)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(entrée, 0.0F, 0.0F, 1.0F);
	}
}
