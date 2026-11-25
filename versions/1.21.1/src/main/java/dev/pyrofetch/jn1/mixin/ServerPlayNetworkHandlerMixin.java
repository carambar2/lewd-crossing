package dev.pyrofetch.jn1.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Un nageur du vide n'est pas un tricheur qui vole, ni un passager d'ascenseur.
 *
 * <p>Le serveur expulse au bout de quatre secondes un joueur qui se maintient
 * en l'air sans rien sous les pieds — il ne regarde que les blocs. C'est
 * exactement ce que fait quelqu'un qui nage dans le vide, ou qui monte dans une
 * cabine (un plancher qui est une entité, au-dessus d'une gaine vide) : on lui
 * dit donc qu'il n'est pas « sur de l'air ».
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Inject(method = "isEntityOnAir", at = @At("HEAD"), cancellable = true)
	private void jn1$nageurDuVide(Entity entité, CallbackInfoReturnable<Boolean> cir) {
		if (VoidSwim.nage(entité) || dev.pyrofetch.jn1.lift.PlatformEntity.sous(entité) != null) {
			cir.setReturnValue(false);
		}
	}
}
