package dev.pyrofetch.jn1.notebook;

import dev.pyrofetch.jn1.network.OpenNotebookPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Le carnet de terrain : clic droit, il s'ouvre (voir {@link FieldNotebook}). */
public class FieldNotebookItem extends Item {
	public FieldNotebookItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (user instanceof ServerPlayerEntity joueur) {
			ServerPlayNetworking.send(joueur, OpenNotebookPayload.INSTANCE);
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.notebook.tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
