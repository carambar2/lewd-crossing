package dev.pyrofetch.jn1.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Le point d'apparition d'un monde neuf, posé près d'une maison ({@link dev.pyrofetch.jn1.worldgen.HouseSpawn}). */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "setupSpawn", at = @At("TAIL"))
	private static void jn1$prèsDUneMaison(ServerWorld monde, ServerWorldProperties propriétés, boolean coffre,
	                                       boolean débogage, CallbackInfo ci) {
		if (!débogage) {
			dev.pyrofetch.jn1.worldgen.HouseSpawn.placer(monde);
		}
	}
}
