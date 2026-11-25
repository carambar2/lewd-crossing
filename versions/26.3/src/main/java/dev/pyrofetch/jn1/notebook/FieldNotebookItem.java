package dev.pyrofetch.jn1.notebook;

import dev.pyrofetch.jn1.network.OpenNotebookPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

/** Le carnet de terrain : clic droit, il s'ouvre (voir {@link FieldNotebook}). */
public class FieldNotebookItem extends Item {
	public FieldNotebookItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		if (user instanceof ServerPlayer joueur) {
			ServerPlayNetworking.send(joueur, OpenNotebookPayload.INSTANCE);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
	                            java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.notebook.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
