package dev.pyrofetch.jn1.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.pyrofetch.jn1.client.morph.MorphHands;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Le bras à la première personne d'un joueur transformé : celui de la
 * compagne ({@link MorphHands}) au lieu de celui du skin, et sans la manche du
 * skin par-dessus.
 *
 * <p>On n'intercepte que les deux dessins de {@code renderArm} : la pose que le
 * jeu prépare juste avant (angles, balancement) reste la sienne.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerArmMixin {
	@WrapOperation(method = "renderArm", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
	private void jn1$brasDeLaCompagne(ModelPart bras, MatrixStack matrices, VertexConsumer tampon, int light, int overlay,
	                                   Operation<Void> original,
	                                   @Local(argsOnly = true) AbstractClientPlayerEntity joueur,
	                                   @Local(argsOnly = true) VertexConsumerProvider tampons) {
		Identifier planche = MorphHands.texture(joueur);
		if (planche == null) {
			original.call(bras, matrices, tampon, light, overlay);
			return;
		}
		boolean droite = bras == ((PlayerEntityRenderer) (Object) this).getModel().rightArm;
		ModelPart sien = MorphHands.bras(droite);
		sien.copyTransform(bras);
		sien.render(matrices, tampons.getBuffer(RenderLayer.getEntitySolid(planche)), light, overlay);
	}

	@WrapOperation(method = "renderArm", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"))
	private void jn1$sansManche(ModelPart manche, MatrixStack matrices, VertexConsumer tampon, int light, int overlay,
	                            Operation<Void> original,
	                            @Local(argsOnly = true) AbstractClientPlayerEntity joueur) {
		if (MorphHands.texture(joueur) == null) {
			original.call(manche, matrices, tampon, light, overlay);
		}
	}
}
