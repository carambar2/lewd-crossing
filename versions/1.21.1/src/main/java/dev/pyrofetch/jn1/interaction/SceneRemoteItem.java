package dev.pyrofetch.jn1.interaction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** La télécommande de scènes. Tout se passe chez la compagne visée : voir {@link SceneHandler}. */
public class SceneRemoteItem extends Item {
	public SceneRemoteItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.scene_remote.tooltip").formatted(Formatting.GRAY));
	}
}
