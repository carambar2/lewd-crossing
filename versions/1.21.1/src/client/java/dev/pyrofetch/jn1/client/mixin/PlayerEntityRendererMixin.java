package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Le corps du nageur suit son regard.
 *
 * <p>Hors de l'eau, le jeu couche un corps en pose de nage bien à plat : c'est
 * la reptation sous un bloc. Dans l'eau, il l'incline selon le tangage. Un
 * nageur du vide qui plongeait tête la première restait donc allongé à
 * l'horizontale, vu des autres ou de la troisième personne.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Redirect(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isTouchingWater()Z"))
	private boolean jn1$inclineDansLeVide(AbstractClientPlayerEntity joueur) {
		return VoidSwim.nage(joueur) || joueur.isTouchingWater();
	}
}
