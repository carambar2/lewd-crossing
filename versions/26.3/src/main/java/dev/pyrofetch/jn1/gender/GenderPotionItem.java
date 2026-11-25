package dev.pyrofetch.jn1.gender;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** La potion d'inversion : bue, elle fait d'un homme une femme, et inversement. */
public class GenderPotionItem extends Item {
	public GenderPotionItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(world, user, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (user instanceof ServerPlayer joueur) {
			Gender.inverser(joueur);
			// Un objet à nous : le jeu ne déclenche « consommer » que pour ses potions.
			net.minecraft.advancements.triggers.CriteriaTriggers.CONSUME_ITEM.trigger(joueur, stack);
		}
		if (user instanceof Player joueur && !joueur.getAbilities().instabuild) {
			stack.shrink(1);
			ItemStack flacon = new ItemStack(Items.GLASS_BOTTLE);
			if (stack.isEmpty()) {
				return flacon;
			}
			if (!joueur.getInventory().add(flacon)) {
				joueur.drop(flacon, false, net.minecraft.util.Prediction.PREDICTED);
			}
		}
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 32;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.DRINK;
	}
}
