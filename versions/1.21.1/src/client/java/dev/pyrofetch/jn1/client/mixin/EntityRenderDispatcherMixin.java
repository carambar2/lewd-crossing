package dev.pyrofetch.jn1.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.pyrofetch.jn1.client.morph.ClientMorphs;
import dev.pyrofetch.jn1.client.morph.MorphRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Un joueur transformé est dessiné par {@link MorphRenderer}.
 *
 * <p>La substitution se fait dans {@code render} seulement, pas dans
 * {@code getRenderer} : le bras à la première personne demande le rendu du
 * joueur et le convertit en {@code PlayerEntityRenderer} — lui rendre autre
 * chose ferait planter le jeu. L'inventaire, lui, passe par {@code render}, et
 * montre donc la compagne.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Unique
	private MorphRenderer jn1$morph;

	@Inject(method = "reload", at = @At("TAIL"))
	private void jn1$créerMorph(ResourceManager manager, CallbackInfo ci,
	                            @Local EntityRendererFactory.Context context) {
		this.jn1$morph = new MorphRenderer(context);
	}

	/** Le joueur qui tient la place de steve n'est dessiné que par le mannequin. */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void jn1$cacherPartenaire(Entity entity, net.minecraft.client.render.Frustum frustum,
	                                  double x, double y, double z,
	                                  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if (dev.pyrofetch.jn1.client.render.SceneView.hidesPlayer(entity)) {
			cir.setReturnValue(false);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;"))
	private EntityRenderer<?> jn1$rendreMorph(EntityRenderDispatcher dispatcher, Entity entity,
	                                           Operation<EntityRenderer<?>> original) {
		if (this.jn1$morph != null && entity instanceof AbstractClientPlayerEntity joueur
				&& ClientMorphs.of(joueur).morphed()) {
			return this.jn1$morph;
		}
		return original.call(dispatcher, entity);
	}
}
