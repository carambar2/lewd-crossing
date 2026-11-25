package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

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
	public CumItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World monde = context.getWorld();
		net.minecraft.util.math.BlockPos visé = context.getBlockPos();
		net.minecraft.block.BlockState état = monde.getBlockState(visé);
		boolean versé;
		if (état.isOf(dev.pyrofetch.jn1.registry.ModBlocks.CUM_TRACE)) {
			// Visée sur une trace : sèche, elle redevient fraîche ; fraîche, elle
			// prend une face de plus si l'on vise un autre côté. Sinon la fiole
			// se buvait, faute de bloc où poser.
			if (état.get(CumTraceBlock.SEC)) {
				if (!monde.isClient()) {
					monde.setBlockState(visé, état.with(CumTraceBlock.SEC, false), net.minecraft.block.Block.NOTIFY_ALL);
					Cum.bruit(monde, visé);
				}
				versé = true;
			} else {
				versé = Cum.poserTrace(monde, visé, context.getSide().getOpposite());
			}
			if (!versé) {
				return ActionResult.FAIL;
			}
		} else if (état.isReplaceable() && !état.isAir()) {
			// De l'herbe haute, de la neige : la trace prend sa place, sur le bloc dessous.
			versé = Cum.poserTrace(monde, visé, net.minecraft.util.math.Direction.DOWN);
		} else {
			versé = Cum.poserTrace(monde, visé.offset(context.getSide()), context.getSide().getOpposite());
		}
		if (!versé) {
			return ActionResult.PASS;
		}
		if (!context.getWorld().isClient() && context.getPlayer() != null) {
			vider(context.getPlayer(), context.getHand());
		}
		return ActionResult.success(context.getWorld().isClient());
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 32;
	}

	/** Bue : trois points de nourriture, cinq secondes de régénération, le flacon revient. */
	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof ServerPlayerEntity joueur) {
			Criteria.CONSUME_ITEM.trigger(joueur, stack);
			joueur.incrementStat(Stats.USED.getOrCreateStat(this));
		}
		if (!world.isClient()) {
			if (user instanceof PlayerEntity joueur) {
				joueur.getHungerManager().add(3, 0.4F);
			}
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
		}
		user.emitGameEvent(GameEvent.DRINK);
		if (user instanceof PlayerEntity joueur && joueur.getAbilities().creativeMode) {
			return stack;
		}
		stack.decrement(1);
		if (stack.isEmpty()) {
			return new ItemStack(Items.GLASS_BOTTLE);
		}
		if (user instanceof PlayerEntity joueur && !joueur.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE))) {
			joueur.dropItem(new ItemStack(Items.GLASS_BOTTLE), false);
		}
		return stack;
	}

	/** Versé sur une fille : appelé par son interaction, côté serveur. */
	public static void verserSur(CompanionEntity fille, PlayerEntity player, Hand hand) {
		fille.addTraces(1);
		Cum.bruit(fille.getWorld(), fille.getBlockPos());
		vider(player, hand);
		dev.pyrofetch.jn1.interaction.Conversation.événement(
				player instanceof net.minecraft.server.network.ServerPlayerEntity sp ? sp : null, fille, "cum_poured");
	}

	/** Une fiole de moins, un flacon vide de plus — sauf en créatif. */
	private static void vider(PlayerEntity player, Hand hand) {
		if (player.getAbilities().creativeMode) {
			return;
		}
		ItemStack pile = player.getStackInHand(hand);
		pile.decrement(1);
		ItemStack flacon = new ItemStack(Items.GLASS_BOTTLE);
		if (pile.isEmpty()) {
			player.setStackInHand(hand, flacon);
		} else if (!player.getInventory().insertStack(flacon)) {
			player.dropItem(flacon, false);
		}
	}
}
