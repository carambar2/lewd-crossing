package dev.pyrofetch.jn1.excitation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/** Le masturbateur : l'objet de plaisir d'un homme ({@link Plaisir}). */
public class MasturbatorItem extends Item {
	public MasturbatorItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return Plaisir.commencer(world, user, hand, false);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return Plaisir.geste();
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return Plaisir.USAGE_MAX;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		Plaisir.pendant(world, user, stack, remainingUseTicks);
	}
}
