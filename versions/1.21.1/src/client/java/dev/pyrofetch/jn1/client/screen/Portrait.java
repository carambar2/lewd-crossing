package dev.pyrofetch.jn1.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Une compagne dessinée dans un écran — son portrait —, à la place de
 * {@code InventoryScreen.drawEntity}.
 *
 * <p><strong>Attention (0.53.3) :</strong> {@code drawEntity} dessine
 * l'entité comme à la fin du tick (instant 1,0), alors que le monde, dans la
 * même image, la dessine en cours de tick. Pour GeckoLib, qui tient une
 * horloge d'animation par entité, le temps avançait puis reculait à chaque
 * image ; la remise au repos de la tête, calculée sur cet écart, prenait une
 * fraction <em>négative</em> et amplifiait l'angle au lieu de le ramener. Le
 * portrait tournait la tête jusqu'à la butée, d'un côté ou de l'autre selon
 * l'orientation de la fille dans le monde (Ellie : 0° et 90°
 * à droite, 180° et 270° à gauche ; Jenny avait le même défaut, en 0.53.1,
 * à moindre angle).
 *
 * <p>D'où une <strong>copie</strong> : une entité du même type, hors du monde,
 * qui porte la même tenue, les mêmes traces et les mêmes formes, et a sa propre
 * horloge. Elle vit au même âge que l'originale et est dessinée au même instant
 * du tick que le monde : ses animations coulent comme les siennes. La vraie
 * fille, elle, n'est plus touchée.
 */
public final class Portrait {
	private final CompanionEntity source;
	@Nullable
	private CompanionEntity copie;

	public Portrait(CompanionEntity source) {
		this.source = source;
	}

	/** Mêmes arguments que {@code InventoryScreen.drawEntity}, l'entité en moins. */
	public void dessiner(DrawContext contexte, int x1, int y1, int x2, int y2, int taille, float décalage,
			float sourisX, float sourisY) {
		CompanionEntity copie = this.copie();
		dessiner(contexte, x1, y1, x2, y2, taille, décalage, sourisX, sourisY, copie != null ? copie : this.source);
	}

	@Nullable
	private CompanionEntity copie() {
		if (this.copie == null && this.source.getWorld() != null
				&& this.source.getType().create(this.source.getWorld()) instanceof CompanionEntity neuve) {
			this.copie = neuve;
		}
		if (this.copie != null) {
			this.source.copierApparence(this.copie);
			this.copie.age = this.source.age;
		}
		return this.copie;
	}

	/**
	 * Une entité de face, qui suit la souris, comme {@code drawEntity} — mais
	 * dessinée à l'instant du tick du monde, ses angles « précédents » posés
	 * comme les autres. Pour une entité du monde (le joueur, dans l'écran de
	 * transformation), les angles sont rendus ensuite.
	 */
	public static void dessiner(DrawContext contexte, int x1, int y1, int x2, int y2, int taille, float décalage,
			float sourisX, float sourisY, LivingEntity entité) {
		float cx = (x1 + x2) / 2.0F;
		float cy = (y1 + y2) / 2.0F;
		contexte.enableScissor(x1, y1, x2, y2);
		float i = (float) Math.atan((cx - sourisX) / 40.0F);
		float j = (float) Math.atan((cy - sourisY) / 40.0F);
		Quaternionf vue = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf penché = new Quaternionf().rotateX(j * 20.0F * (float) (Math.PI / 180.0));
		vue.mul(penché);

		float corps = entité.bodyYaw;
		float corpsAvant = entité.prevBodyYaw;
		float lacet = entité.getYaw();
		float lacetAvant = entité.prevYaw;
		float tangage = entité.getPitch();
		float tangageAvant = entité.prevPitch;
		float tête = entité.headYaw;
		float têteAvant = entité.prevHeadYaw;

		entité.bodyYaw = entité.prevBodyYaw = 180.0F + i * 20.0F;
		entité.setYaw(180.0F + i * 40.0F);
		entité.prevYaw = entité.getYaw();
		entité.setPitch(-j * 20.0F);
		entité.prevPitch = entité.getPitch();
		entité.headYaw = entité.prevHeadYaw = entité.getYaw();

		float échelle = entité.getScale();
		Vector3f centre = new Vector3f(0.0F, entité.getHeight() / 2.0F + décalage * échelle, 0.0F);
		try {
			rendre(contexte, cx, cy, taille / échelle, centre, vue, penché, entité);
		} finally {
			entité.bodyYaw = corps;
			entité.prevBodyYaw = corpsAvant;
			entité.setYaw(lacet);
			entité.prevYaw = lacetAvant;
			entité.setPitch(tangage);
			entité.prevPitch = tangageAvant;
			entité.headYaw = tête;
			entité.prevHeadYaw = têteAvant;
			contexte.disableScissor();
		}
	}

	/** Le dessin de {@code drawEntity}, à l'instant du tick du monde au lieu de 1,0. */
	private static void rendre(DrawContext contexte, float x, float y, float taille, Vector3f centre,
			Quaternionf vue, Quaternionf penché, LivingEntity entité) {
		MinecraftClient client = MinecraftClient.getInstance();
		float instant = client.getRenderTickCounter().getTickDelta(false);
		contexte.getMatrices().push();
		contexte.getMatrices().translate(x, y, 50.0);
		contexte.getMatrices().scale(taille, taille, -taille);
		contexte.getMatrices().translate(centre.x, centre.y, centre.z);
		contexte.getMatrices().multiply(vue);
		DiffuseLighting.method_34742();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		dispatcher.setRotation(penché.conjugate(new Quaternionf()).rotateY((float) Math.PI));
		dispatcher.setRenderShadows(false);
		RenderSystem.runAsFancy(() -> dispatcher.render(entité, 0.0, 0.0, 0.0, 0.0F, instant,
				contexte.getMatrices(), contexte.getVertexConsumers(), 0xF000F0));
		contexte.draw();
		dispatcher.setRenderShadows(true);
		contexte.getMatrices().pop();
		DiffuseLighting.enableGuiDepthLighting();
	}
}
