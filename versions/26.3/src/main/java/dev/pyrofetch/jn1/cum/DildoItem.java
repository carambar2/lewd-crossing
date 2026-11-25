package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.excitation.Plaisir;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Le dildo en main. Accroupi et visé sur un bloc, il se pose ({@link DildoBlock}) ;
 * sinon, c'est l'objet de plaisir d'une femme ({@link Plaisir}). Le même objet
 * sert au dildo de cheval et à Excalibur, plus gros, plus rapides.
 */
public class DildoItem extends BlockItem {
	/** En combien de ticks il remplit la barre : plus il est gros, plus c'est vite. */
	private final int durée;

	public DildoItem(Block block, Properties settings) {
		this(block, settings, Plaisir.DURÉE);
	}

	public DildoItem(Block block, Properties settings, int durée) {
		super(block, settings);
		this.durée = durée;
	}

	/** Il ne se pose qu'accroupi : sinon, visé vers le sol pour s'en servir, il s'y posait. */
	@Override
	public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
		if (context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()) {
			return net.minecraft.world.InteractionResult.PASS;
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		return Plaisir.commencer(world, user, hand, true);
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
		Plaisir.pendant(world, user, stack, remainingUseTicks, this.durée);
	}
}
