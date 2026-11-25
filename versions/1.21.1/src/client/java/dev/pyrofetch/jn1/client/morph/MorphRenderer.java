package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.client.render.CompanionShading;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Dessine un joueur transformé sous les traits de sa compagne.
 *
 * <p>Branché par {@code EntityRenderDispatcherMixin} au seul moment du rendu
 * dans le monde (et dans l'inventaire) : ailleurs — le bras à la première
 * personne notamment — le jeu attend le rendu du joueur et le convertit.
 *
 * <p>Même éclairage que les compagnes ({@link CompanionShading}) et mêmes faces
 * arrière éliminées, pour qu'elle paraisse la même, qu'on la croise ou qu'on
 * la soit.
 */
public class MorphRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayerEntity, MorphAnimatable> {
	/** Ce qu'un joueur accroupi descend, comme au rendu du jeu. */
	private static final double SNEAK_DROP = -0.125;

	private final CompanionShading shading = new CompanionShading();

	public MorphRenderer(EntityRendererFactory.Context context) {
		super(context, new MorphModel(), MorphAnimatable.INSTANCE);
		this.shadowRadius = 0.4F;
		// Ce qu'il tient en main, dans les siennes. Pas d'armure.
		this.addRenderLayer(new MorphHeldItemLayer(this));
	}

	@Override
	public void render(AbstractClientPlayerEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
	                   VertexConsumerProvider bufferSource, int packedLight) {
		this.animatable.montrer(ClientMorphs.of(entity));
		// Transformé en Aura, on est une projection : lumineuse dans le noir.
		boolean projection = this.animatable.kind() == dev.pyrofetch.jn1.entity.CompanionKind.AURA;
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
				projection ? net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE : packedLight);
	}

	@Override
	public Vec3d getPositionOffset(AbstractClientPlayerEntity entity, float tickDelta) {
		return entity.isInSneakingPose() ? new Vec3d(0, SNEAK_DROP, 0) : Vec3d.ZERO;
	}

	@Nullable
	@Override
	public RenderLayer getRenderType(MorphAnimatable animatable, Identifier texture,
	                                 @Nullable VertexConsumerProvider bufferSource, float partialTick) {
		RenderLayer défaut = super.getRenderType(animatable, texture, bufferSource, partialTick);
		// Invisible ou translucide : on laisse faire GeckoLib ; sinon, faces
		// arrière éliminées, comme chez les compagnes — translucides pour Aura.
		if (défaut == null || this.currentEntity.isInvisible()) {
			return défaut;
		}
		return animatable.kind() == dev.pyrofetch.jn1.entity.CompanionKind.AURA
				? RenderLayer.getEntityTranslucentCull(texture) : RenderLayer.getEntityCutout(texture);
	}

	@Override
	public software.bernie.geckolib.util.Color getRenderColor(MorphAnimatable animatable, float partialTick, int packedLight) {
		if (animatable.kind() == dev.pyrofetch.jn1.entity.CompanionKind.AURA && this.currentEntity != null) {
			return software.bernie.geckolib.util.Color.ofARGB(
					dev.pyrofetch.jn1.client.render.Hologram.alpha(this.currentEntity, partialTick), 1.0F, 1.0F, 1.0F);
		}
		return super.getRenderColor(animatable, partialTick, packedLight);
	}

	@Override
	public void renderRecursively(MatrixStack poseStack, MorphAnimatable animatable, GeoBone bone,
	                              RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer,
	                              boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
		boolean root = this.shading.enterBone(poseStack, bone);
		super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
				partialTick, packedLight, packedOverlay, colour);
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
	public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
	                                 int packedLight, int packedOverlay, int colour) {
		super.createVerticesOfQuad(quad, poseState, this.shading.normal(quad, poseState, normal), buffer,
				packedLight, packedOverlay, colour);
	}
}
