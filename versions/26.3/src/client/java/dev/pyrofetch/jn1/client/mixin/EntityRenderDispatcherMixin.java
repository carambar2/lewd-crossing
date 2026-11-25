package dev.pyrofetch.jn1.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.pyrofetch.jn1.client.morph.ClientMorphs;
import dev.pyrofetch.jn1.client.morph.MorphRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un joueur transformé est dessiné par {@link MorphRenderer}.
 *
 * <p>En 1.21.1, la substitution se faisait dans {@code render} seulement, pas
 * dans {@code getRenderer} : le bras à la première personne demandait le rendu
 * du joueur et le convertissait en {@code PlayerEntityRenderer}. En 26.3, ce
 * bras le demande à partir de l'état de rendu d'avatar
 * ({@code getRenderer(AvatarRenderState)}), que l'on ne touche pas : on peut
 * donc substituer dans {@code getRenderer(Entity)}, que suivent la relève de
 * l'état ({@code extractEntity}), l'inventaire et les portraits — l'inventaire
 * et l'écran de transformation montrent donc la compagne, comme avant. Au
 * dessin ({@code submit}), le rendu est retrouvé d'après l'état : le sien est
 * un {@code MorphRenderer.État}.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Unique
	private MorphRenderer jn1$morph;

	@Inject(method = "onResourceManagerReload", at = @At("TAIL"))
	private void jn1$créerMorph(ResourceManager manager, CallbackInfo ci,
	                            @Local EntityRendererProvider.Context context) {
		this.jn1$morph = new MorphRenderer(context);
	}

	/** Le joueur qui tient la place de steve n'est dessiné que par le mannequin. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void jn1$cacherPartenaire(Entity entity, Frustum frustum, double x, double y, double z, float instant,
	                                  CallbackInfoReturnable<Boolean> cir) {
		if (dev.pyrofetch.jn1.client.render.SceneView.hidesPlayer(entity)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void jn1$rendreMorph(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (this.jn1$morph != null && entity instanceof AbstractClientPlayer joueur
				&& ClientMorphs.of(joueur).morphed()) {
			cir.setReturnValue(this.jn1$morph);
		}
	}

	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void jn1$rendreÉtatMorph(EntityRenderState état, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (this.jn1$morph != null && état instanceof MorphRenderer.État) {
			cir.setReturnValue(this.jn1$morph);
		}
	}
}
