package dev.pyrofetch.jn1.morph;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * La Horny Potion : on la boit comme une potion, et l'on choisit ensuite en
 * quelle compagne se transformer (voir {@link MorphHandler}).
 *
 * <p>Ce n'est pas une potion du jeu : elle ne porte aucun effet, et c'est ce
 * qui la garde hors de l'alambic et de la flèche trempée.
 */
public class HornyPotionItem extends Item {
	private static final int DRINK_TICKS = 32;

	public HornyPotionItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(world, user, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (user instanceof ServerPlayer joueur) {
			MorphHandler.potionDrunk(joueur);
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
		return DRINK_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.DRINK;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.horny_potion.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
