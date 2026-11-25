package dev.pyrofetch.jn1.morph;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * La Horny Potion : on la boit comme une potion, et l'on choisit ensuite en
 * quelle compagne se transformer (voir {@link MorphHandler}).
 *
 * <p>Ce n'est pas une potion du jeu : elle ne porte aucun effet, et c'est ce
 * qui la garde hors de l'alambic et de la flèche trempée.
 */
public class HornyPotionItem extends Item {
	private static final int DRINK_TICKS = 32;

	public HornyPotionItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return ItemUsage.consumeHeldItem(world, user, hand);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof ServerPlayerEntity joueur) {
			MorphHandler.potionDrunk(joueur);
		}
		if (user instanceof PlayerEntity joueur && !joueur.getAbilities().creativeMode) {
			stack.decrement(1);
			ItemStack flacon = new ItemStack(Items.GLASS_BOTTLE);
			if (stack.isEmpty()) {
				return flacon;
			}
			if (!joueur.getInventory().insertStack(flacon)) {
				joueur.dropItem(flacon, false);
			}
		}
		return stack;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return DRINK_TICKS;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.horny_potion.tooltip").formatted(Formatting.LIGHT_PURPLE));
	}
}
