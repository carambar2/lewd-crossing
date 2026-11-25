package dev.pyrofetch.jn1.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pyrofetch.jn1.client.morph.MorphHands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Le bras à la première personne d'un joueur transformé : celui de la
 * compagne ({@link MorphHands}) au lieu de celui du skin, et sans la manche du
 * skin par-dessus.
 *
 * <p>On n'intercepte que le dessin de {@code renderHand} : la pose que le jeu
 * prépare juste avant (angles, balancement) reste la sienne.
 *
 * <p>26.3 : le bras et sa manche (son enfant) partent d'un seul envoi au
 * rendu ; le remplacer suffit à taire la manche. Le bras à la première personne
 * est toujours celui du joueur local : la méthode ne reçoit plus le joueur.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerArmMixin {
	@WrapOperation(method = "renderHand", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;)V"))
	private void jn1$brasDeLaCompagne(SubmitNodeCollector envoi, ModelPart bras, PoseStack matrices, RenderType couche,
	                                   int light, int overlay, UvMapping uv, Operation<Void> original) {
		AbstractClientPlayer joueur = Minecraft.getInstance().player;
		Identifier planche = joueur == null ? null : MorphHands.texture(joueur);
		if (planche == null) {
			original.call(envoi, bras, matrices, couche, light, overlay, uv);
			return;
		}
		boolean droite = bras == ((AvatarRenderer<?>) (Object) this).getModel().rightArm;
		ModelPart sien = MorphHands.bras(droite);
		sien.loadPose(bras.storePose());
		envoi.submitModelPart(sien, matrices, RenderTypes.entitySolid(planche), light, overlay, null);
	}
}
