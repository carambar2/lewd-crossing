package dev.pyrofetch.jn1.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Le coin du panneau d'inventaire, qui se déplace quand le livre de recettes s'ouvre. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x")
	int jn1$x();

	@Accessor("y")
	int jn1$y();

	@Accessor("backgroundWidth")
	int jn1$largeur();
}
