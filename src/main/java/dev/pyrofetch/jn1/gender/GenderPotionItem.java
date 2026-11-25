package dev.pyrofetch.jn1.gender;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/** La potion d'inversion : bue, elle fait d'un homme une femme, et inversement. */
public class GenderPotionItem extends Item {
	public GenderPotionItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return ItemUsage.consumeHeldItem(world, user, hand);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof ServerPlayerEntity joueur) {
			Gender.inverser(joueur);
			// Un objet à nous : le jeu ne déclenche « consommer » que pour ses potions.
			net.minecraft.advancement.criterion.Criteria.CONSUME_ITEM.trigger(joueur, stack);
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
		return 32;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}
}
