package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level world) {
		super(type, world);
	}

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void jn1$nagerDansLeVide(Vec3 saisie, CallbackInfo ci) {
		if (this.isLocalInstanceAuthoritative() && VoidSwim.nage(this)) {
			VoidSwim.avancer(this, saisie, this.jumping, this.isShiftKeyDown());
			ci.cancel();
		}
	}
}
