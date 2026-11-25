package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.excitation.Plaisir;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Le dildo en main. Accroupi et visé sur un bloc, il se pose ({@link DildoBlock}) ;
 * sinon, c'est l'objet de plaisir d'une femme ({@link Plaisir}). Le même objet
 * sert au dildo de cheval et à Excalibur, plus gros, plus rapides.
 */
public class DildoItem extends BlockItem {
	/** En combien de ticks il remplit la barre : plus il est gros, plus c'est vite. */
	private final int durée;

	public DildoItem(Block block, Settings settings) {
		this(block, settings, Plaisir.DURÉE);
	}

	public DildoItem(Block block, Settings settings, int durée) {
		super(block, settings);
		this.durée = durée;
	}

	/** Il ne se pose qu'accroupi : sinon, visé vers le sol pour s'en servir, il s'y posait. */
	@Override
	public net.minecraft.util.ActionResult useOnBlock(net.minecraft.item.ItemUsageContext context) {
		if (context.getPlayer() != null && !context.getPlayer().isSneaking()) {
			return net.minecraft.util.ActionResult.PASS;
		}
		return super.useOnBlock(context);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return Plaisir.commencer(world, user, hand, true);
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
		Plaisir.pendant(world, user, stack, remainingUseTicks, this.durée);
	}
}
