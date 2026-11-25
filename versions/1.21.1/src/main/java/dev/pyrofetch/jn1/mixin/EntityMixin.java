package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La pose de nage, dans le vide.
 *
 * <p>Le jeu ne couche un joueur à l'horizontale que dans l'eau. Ici, le sprint
 * suffit : c'est ce qui rend la pose aux autres joueurs, puisque le serveur
 * calcule lui aussi cet état et le leur transmet.
 *
 * <p>C'est aussi l'endroit où effacer la chute : le serveur la compte d'après
 * les positions reçues, et une longue dérive vers le bas finirait sinon en
 * chute mortelle au premier contact avec le sol.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void jn1$nagerDansLeVide(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (VoidSwim.nage(self)) {
			self.setSwimming(self.isSprinting());
			self.fallDistance = 0.0F;
			ci.cancel();
		}
	}

	/**
	 * Le partenaire d'une scène n'étouffe pas : le serveur le tient à la place
	 * de la fille, debout, alors qu'on le voit couché sur le lit ou à genoux —
	 * sous un plafond à deux blocs du sol, ses yeux étaient dans le plafond.
	 */
	@Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
	private void jn1$partenairePasÉtouffé(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self.getVehicle() instanceof dev.pyrofetch.jn1.entity.CompanionEntity elle && elle.isScenePartner(self)) {
			cir.setReturnValue(false);
		}
	}
}
