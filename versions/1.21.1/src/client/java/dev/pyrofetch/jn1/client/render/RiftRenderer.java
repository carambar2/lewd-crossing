package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.rift.RiftEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

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
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {
	private static final Identifier TEXTURE = JN1.id("textures/entity/rift.png");
	private static final int BANDES = 26;
	private static final float LARGEUR = 1.15F;

	public RiftRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(RiftEntity faille, float lacet, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière) {
		float ouverture = faille.ouverture(tickDelta);
		if (ouverture <= 0.0F) {
			return;
		}
		matrices.push();
		// Debout, tournée vers la caméra autour de la verticale seulement.
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - this.dispatcher.camera.getYaw()));

		VertexConsumer tampon = tampons.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
		MatrixStack.Entry entrée = matrices.peek();
		float temps = faille.age + tickDelta;
		float hauteur = faille.getHeight();
		// Plus de bandes pour une grande faille : leur pas reste le même.
		int bandes = Math.max(BANDES, Math.round(BANDES * hauteur / RiftEntity.TAILLE_DÉFAUT));
		// Elle s'ouvre par le milieu : la hauteur vient d'abord, la largeur suit.
		float hauteurVue = hauteur * MathHelper.clamp(ouverture * 1.6F, 0.0F, 1.0F);
		float bas = (hauteur - hauteurVue) / 2.0F;
		int pas = (int) (temps / 3.0F);

		for (int bande = 0; bande < bandes; bande++) {
			float v0 = bande / (float) bandes;
			float v1 = (bande + 1) / (float) bandes;
			float yHaut = bas + hauteurVue * (1.0F - v0);
			float yBas = bas + hauteurVue * (1.0F - v1);

			// La respiration : chaque bande enfle un peu, en décalé.
			float largeur = LARGEUR * faille.getWidth() * ouverture
					* (1.0F + 0.05F * MathHelper.sin(temps * 0.12F + bande * 0.45F));
			float décalage = 0.0F;
			long empreinte = MathHelper.hashCode(bande, pas, faille.getId());
			if ((empreinte & 0xFF) < 22) {
				décalage = (((empreinte >> 8) & 0xFF) / 255.0F - 0.5F) * 0.45F * ouverture * faille.getWidth();
			}
			float gauche = -largeur / 2.0F + décalage;
			float droite = largeur / 2.0F + décalage;

			sommet(tampon, entrée, gauche, yBas, 0.0F, v1);
			sommet(tampon, entrée, droite, yBas, 1.0F, v1);
			sommet(tampon, entrée, droite, yHaut, 1.0F, v0);
			sommet(tampon, entrée, gauche, yHaut, 0.0F, v0);
		}
		matrices.pop();
		super.render(faille, lacet, tickDelta, matrices, tampons, lumière);
	}

	private static void sommet(VertexConsumer tampon, MatrixStack.Entry entrée, float x, float y, float u, float v) {
		tampon.vertex(entrée, x, y, 0.0F)
				.color(255, 255, 255, 255)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(entrée, 0.0F, 0.0F, 1.0F);
	}

	@Override
	public Identifier getTexture(RiftEntity faille) {
		return TEXTURE;
	}
}
