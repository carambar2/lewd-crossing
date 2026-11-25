package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.lab.VoidSwim;
import dev.pyrofetch.jn1.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.Music;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La musique du vide.
 *
 * <p>Le jeu choisit la musique d'après le biome, et le vide est fait du biome
 * {@code the_void} du jeu — le changer n'aurait valu que pour les tronçons
 * encore jamais générés, pas pour le laboratoire des parties en cours. On
 * choisit donc d'après la dimension, avant que le jeu ne regarde le biome.
 *
 * <p>Elle remplace ce qui jouait en entrant, puis revient après un silence
 * d'une demi-minute à deux minutes : dans le vide, le silence compte autant que
 * la musique.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Unique
	private static final Music jn1$MUSIQUE_DU_VIDE = new Music(ModSounds.MUSIC_VOID, 600, 2400, true);

	@Shadow
	@Nullable
	public LocalPlayer player;

	@Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
	private void jn1$musiqueDuVide(CallbackInfoReturnable<Music> cir) {
		if (this.player != null && VoidSwim.dansLeVide(this.player)) {
			cir.setReturnValue(jn1$MUSIQUE_DU_VIDE);
		}
	}
}
