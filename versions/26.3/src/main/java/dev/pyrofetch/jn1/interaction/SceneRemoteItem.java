package dev.pyrofetch.jn1.interaction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** La télécommande de scènes. Tout se passe chez la compagne visée : voir {@link SceneHandler}. */
public class SceneRemoteItem extends Item {
	public SceneRemoteItem(Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.scene_remote.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
