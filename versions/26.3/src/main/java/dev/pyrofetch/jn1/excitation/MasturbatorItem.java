package dev.pyrofetch.jn1.excitation;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** Le masturbateur : l'objet de plaisir d'un homme ({@link Plaisir}). */
public class MasturbatorItem extends Item {
	public MasturbatorItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		return Plaisir.commencer(world, user, hand, false);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return Plaisir.geste();
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return Plaisir.USAGE_MAX;
	}

	@Override
	public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		Plaisir.pendant(world, user, stack, remainingUseTicks);
	}
}
