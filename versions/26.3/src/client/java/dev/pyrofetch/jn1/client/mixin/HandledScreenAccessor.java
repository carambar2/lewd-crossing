package dev.pyrofetch.jn1.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Le coin du panneau d'inventaire, qui se déplace quand le livre de recettes s'ouvre. */
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
	@Accessor("leftPos")
	int jn1$x();

	@Accessor("topPos")
	int jn1$y();

	@Accessor("imageWidth")
	int jn1$largeur();
}
