package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

public class CompanionRenderer<T extends CompanionEntity> extends GeoEntityRenderer<T> {

	public CompanionRenderer(EntityRendererFactory.Context context) {
		this(context, new CompanionModel<>());
	}

	protected CompanionRenderer(EntityRendererFactory.Context context, CompanionModel<T> model) {
		super(context, model);
		// Le cadeau qu'elle vient de recevoir, tenu à la main.
		this.addRenderLayer(new GiftLayer<>(this));
		// Ses traces de cum, par-dessus sa planche.
		this.addRenderLayer(new TracesLayer<>(this));
		this.shadowRadius = 0.4F;
	}

	/**
	 * Le rendu élimine les faces arrière, là où GeckoLib les garde par défaut.
	 *
	 * <p>La poitrine, la croupe et la robe de Gardevoir sont des cubes dont une
	 * dimension vaut zéro. Leurs faces avant et arrière sont donc **dans le même
	 * plan**, et tant que les deux sont dessinées elles se disputent chaque
	 * pixel : la face arrière, qui échantillonne une autre zone de la planche,
	 * l'emporte par endroits et y pose une tache sombre. Le dessin change avec
	 * l'angle de vue, ce qui donne une ombre qui se déplace avec le joueur.
	 *
	 * <p>Aucune géométrie n'est perdue : un cube plat n'est pas une facette
	 * simple mais une boîte dégénérée dont les deux faces regardent en sens
	 * opposés. De n'importe quel point de vue, l'une des deux est tournée vers
	 * l'œil et reste tracée.
	 *
	 * <p>Cela suppose que toutes les boîtes soient enroulées dans le même sens :
	 * une taille négative les retourne, et c'est alors la mauvaise face qui
	 * tombe. `validate_assets.py` refuse désormais les tailles négatives.
	 */
	@Override
	public RenderLayer getRenderType(T animatable, Identifier texture,
	                                 VertexConsumerProvider bufferSource, float partialTick) {
		// Aura est une projection : translucide, faces arrière toujours éliminées.
		if (animatable instanceof dev.pyrofetch.jn1.entity.AuraEntity aura && aura.isHologram()) {
			return RenderLayer.getEntityTranslucentCull(texture);
		}
		return RenderLayer.getEntityCutout(texture);
	}

	@Override
	public software.bernie.geckolib.util.Color getRenderColor(T animatable, float partialTick, int packedLight) {
		if (animatable instanceof dev.pyrofetch.jn1.entity.AuraEntity aura && aura.isHologram()) {
			return software.bernie.geckolib.util.Color.ofARGB(Hologram.alpha(animatable, partialTick), 1.0F, 1.0F, 1.0F);
		}
		return super.getRenderColor(animatable, partialTick, packedLight);
	}

	/** Une projection ne porte pas d'ombre. */
	@Override
	protected float getShadowRadius(T entity) {
		return entity instanceof dev.pyrofetch.jn1.entity.AuraEntity aura && aura.isHologram() ? 0.0F : super.getShadowRadius(entity);
	}

	/** Lissage des normales et vraie normale des cubes plats : voir {@link CompanionShading}. */
	private final CompanionShading shading = new CompanionShading();

	/** Vrai pendant que {@link TracesLayer} redessine le corps avec un calque de traces. */
	private boolean traces;

	void dessinerTraces(boolean traces) {
		this.traces = traces;
	}

	@Override
	public void renderRecursively(MatrixStack poseStack, T animatable, GeoBone bone,
	                              RenderLayer renderType, VertexConsumerProvider bufferSource,
	                              VertexConsumer buffer, boolean isReRender, float partialTick,
	                              int packedLight, int packedOverlay, int colour) {
		// Le calque de traces suit les UV de la fille : le mannequin et la pièce
		// de scène, qui lisent d'autres planches, n'en reçoivent pas.
		if (this.traces && (bone.getName().equals("steve") || bone.getName().equals(animatable.kind().sceneProp()))) {
			return;
		}
		boolean root = this.shading.enterBone(poseStack, bone);

		// Le mannequin d'un partenaire porte son skin : ses UV sont celles d'un
		// skin de joueur, 64 × 64. Translucide comme le joueur du jeu, pour la
		// seconde couche (chapeau, veste).
		if (animatable.showsScenePartner() && bone.getName().equals("steve")) {
			RenderLayer couche = RenderLayer.getEntityTranslucent(SceneView.skin(animatable).texture());
			renderType = couche;
			buffer = bufferSource.getBuffer(couche);
		}

		// La pièce de scène a sa propre planche : ses UV visent un coin que celle
		// de la compagne laisse vide. On change de couche pour son sous-arbre ;
		// GeckoLib reprend lui-même un tampon neuf pour les os suivants.
		if (animatable.showsSceneProp() && bone.getName().equals(animatable.kind().sceneProp())) {
			RenderLayer couche = RenderLayer.getEntityCutoutNoCull(
					dev.pyrofetch.jn1.JN1.id(animatable.kind().scenePropTexturePath()));
			renderType = couche;
			buffer = bufferSource.getBuffer(couche);
		}

		super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
				isReRender, partialTick, packedLight, packedOverlay, colour);

		// Le point des yeux du joueur qui tient la place de steve : le centre
		// de la tête du mannequin, là où le jeu met l'œil d'un joueur. La matrice
		// que GeckoLib vient de tenir part du pivot de l'os.
		if (bone.getName().equals("Head2") && SceneView.participating() == animatable) {
			Vector3f centre = new Vector3f();
			int n = 0;
			for (GeoCube cube : bone.getCubes()) {
				for (GeoQuad quad : cube.quads()) {
					if (quad == null) {
						continue;
					}
					for (software.bernie.geckolib.cache.object.GeoVertex sommet : quad.vertices()) {
						centre.add(sommet.position());
						n++;
					}
				}
			}
			if (n > 0) {
				centre.div(n).sub(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
				Vector3f monde = bone.getWorldSpaceMatrix().transformPosition(centre);
				SceneView.remember(animatable, new net.minecraft.util.math.Vec3d(monde.x(), monde.y(), monde.z()));
			}
		}

		this.shading.leaveBone(root);
	}

	@Override
	public void renderCube(MatrixStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
	                       int packedOverlay, int colour) {
		if (!CompanionShading.renderFlatCube(this, poseStack, cube, buffer, packedLight, packedOverlay, colour)) {
			super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
		}
	}

	@Override
	public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal,
	                                 VertexConsumer buffer, int packedLight, int packedOverlay,
	                                 int colour) {
		super.createVerticesOfQuad(quad, poseState, this.shading.normal(quad, poseState, normal), buffer,
				packedLight, packedOverlay, colour);
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, MatrixStack matrices,
	                   VertexConsumerProvider vertexConsumers, int light) {
		if (entity instanceof dev.pyrofetch.jn1.entity.AuraEntity aura && aura.isHologram()) {
			// Lumineuse dans le noir, et décalée d'un cheveu pendant un parasite.
			matrices.push();
			matrices.translate(Hologram.décalage(entity), 0.0F, 0.0F);
			super.render(entity, entityYaw, partialTick, matrices, vertexConsumers,
					LightmapTextureManager.MAX_LIGHT_COORDINATE);
			matrices.pop();
		} else {
			super.render(entity, entityYaw, partialTick, matrices, vertexConsumers, light);
		}

		// Sa bulle est dessinée dans l'interface, au-dessus d'elle (BubbleOverlay).
		dev.pyrofetch.jn1.client.hud.BubbleOverlay.ancrer(entity, partialTick);
	}

}
