package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remplace le déplacement d'un joueur qui nage dans le vide.
 *
 * <p>Au niveau du joueur et non de {@code LivingEntity} : {@code PlayerEntity.travel}
 * retouche la vitesse verticale d'un nageur avant de passer la main, et ne le
 * fait que tête baissée — on plongeait alors trois fois plus vite qu'on ne
 * remontait. Intercepter plus bas laissait passer cette retouche.
 *
 * <p>Seulement du côté qui calcule le mouvement — le client, pour un joueur :
 * le serveur ne fait que recevoir ses positions.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
		super(type, world);
	}

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void jn1$nagerDansLeVide(Vec3d saisie, CallbackInfo ci) {
		if (this.isLogicalSideForUpdatingMovement() && VoidSwim.nage(this)) {
			VoidSwim.avancer(this, saisie, this.jumping, this.isSneaking());
			ci.cancel();
		}
	}
}
