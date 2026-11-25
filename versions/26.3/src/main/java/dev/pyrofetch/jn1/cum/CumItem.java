package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Le cum, en fiole. Il se verse : sur une face de bloc, il y laisse une trace
 * ({@link CumTraceBlock}) ; sur une fille, il s'ajoute à ses traces
 * ({@link CompanionEntity#addTraces}). Le flacon vide revient, comme celui
 * d'une potion.
 *
 * <p>Visé dans le vide, il se boit : trois points de nourriture et quelques
 * secondes de régénération. Versé dans un chaudron, il y caille en fromage
 * ({@link CumCauldronBlock}).
 *
 * <p>Verser sur une fille passe par {@code CompanionEntity.interactMob} : son
 * clic droit ouvre d'ordinaire son menu, et l'objet n'y serait jamais
 * consulté. Il faut s'accroupir ; debout, la fiole est un cadeau.
 */
public class CumItem extends Item {
	public CumItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level monde = context.getLevel();
		net.minecraft.core.BlockPos visé = context.getClickedPos();
		net.minecraft.world.level.block.state.BlockState état = monde.getBlockState(visé);
		boolean versé;
		if (état.is(dev.pyrofetch.jn1.registry.ModBlocks.CUM_TRACE)) {
			// Visée sur une trace : sèche, elle redevient fraîche ; fraîche, elle
			// prend une face de plus si l'on vise un autre côté. Sinon la fiole
			// se buvait, faute de bloc où poser.
			if (état.getValue(CumTraceBlock.SEC)) {
				if (!monde.isClientSide()) {
					monde.setBlock(visé, état.setValue(CumTraceBlock.SEC, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
					Cum.bruit(monde, visé);
				}
				versé = true;
			} else {
				versé = Cum.poserTrace(monde, visé, context.getClickedFace().getOpposite());
			}
			if (!versé) {
				return InteractionResult.FAIL;
			}
		} else if (état.canBeReplaced() && !état.isAir()) {
			// De l'herbe haute, de la neige : la trace prend sa place, sur le bloc dessous.
			versé = Cum.poserTrace(monde, visé, net.minecraft.core.Direction.DOWN);
		} else {
			versé = Cum.poserTrace(monde, visé.relative(context.getClickedFace()), context.getClickedFace().getOpposite());
		}
		if (!versé) {
			return InteractionResult.PASS;
		}
		if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
			vider(context.getPlayer(), context.getHand());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		user.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 32;
	}

	/** Bue : trois points de nourriture, cinq secondes de régénération, le flacon revient. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (user instanceof ServerPlayer joueur) {
			CriteriaTriggers.CONSUME_ITEM.trigger(joueur, stack);
			joueur.awardStat(Stats.ITEM_USED.get(this));
		}
		if (!world.isClientSide()) {
			if (user instanceof Player joueur) {
				joueur.getFoodData().eat(3, 0.4F);
			}
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
		}
		user.gameEvent(GameEvent.DRINK);
		if (user instanceof Player joueur && joueur.getAbilities().instabuild) {
			return stack;
		}
		stack.shrink(1);
		if (stack.isEmpty()) {
			return new ItemStack(Items.GLASS_BOTTLE);
		}
		if (user instanceof Player joueur && !joueur.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
			joueur.drop(new ItemStack(Items.GLASS_BOTTLE), false, net.minecraft.util.Prediction.PREDICTED);
		}
		return stack;
	}

	/** Versé sur une fille : appelé par son interaction, côté serveur. */
	public static void verserSur(CompanionEntity fille, Player player, InteractionHand hand) {
		fille.addTraces(1);
		Cum.bruit(fille.level(), fille.blockPosition());
		vider(player, hand);
		dev.pyrofetch.jn1.interaction.Conversation.événement(
				player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null, fille, "cum_poured");
	}

	/** Une fiole de moins, un flacon vide de plus — sauf en créatif. */
	private static void vider(Player player, InteractionHand hand) {
		if (player.getAbilities().instabuild) {
			return;
		}
		ItemStack pile = player.getItemInHand(hand);
		pile.shrink(1);
		ItemStack flacon = new ItemStack(Items.GLASS_BOTTLE);
		if (pile.isEmpty()) {
			player.setItemInHand(hand, flacon);
		} else if (!player.getInventory().add(flacon)) {
			player.drop(flacon, false, net.minecraft.util.Prediction.PREDICTED);
		}
	}
}
