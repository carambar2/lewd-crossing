package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le corps du nageur suit son regard.
 *
 * <p>Hors de l'eau, le jeu couche un corps en pose de nage bien à plat : c'est
 * la reptation sous un bloc. Dans l'eau, il l'incline selon le tangage. Un
 * nageur du vide qui plongeait tête la première restait donc allongé à
 * l'horizontale, vu des autres ou de la troisième personne.
 *
 * <p>26.3 : l'inclinaison ({@code setupRotations}) ne lit plus l'entité mais
 * son état de rendu ; c'est donc à la relève de cet état qu'un nageur du vide
 * est dit « dans l'eau ». Pour un joueur, rien d'autre ne lit ce drapeau.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void jn1$inclineDansLeVide(Avatar entité, AvatarRenderState état, float instant, CallbackInfo ci) {
		if (entité instanceof AbstractClientPlayer joueur && VoidSwim.nage(joueur)) {
			état.isInWater = true;
		}
	}
}
