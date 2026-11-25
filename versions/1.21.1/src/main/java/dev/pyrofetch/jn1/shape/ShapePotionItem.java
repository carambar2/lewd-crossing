package dev.pyrofetch.jn1.shape;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Une potion de forme : poitrine, croupe ou taille, d'un cran en plus ou en
 * moins ({@link Shapes}).
 *
 * <p>Bue, elle agit sur le joueur — poitrine et croupe ne se voient que s'il
 * est transformé. Tendue à une compagne (par {@code CompanionEntity.interactMob},
 * dont le menu passerait avant), c'est elle qui la boit. Comme la Horny
 * Potion, ce n'est pas une potion du jeu : aucun effet, hors de l'alambic.
 */
public class ShapePotionItem extends Item {
	private static final int DRINK_TICKS = 32;

	private final Shapes.Part part;
	private final int delta;

	public ShapePotionItem(Shapes.Part part, int delta, Settings settings) {
		super(settings);
		this.part = part;
		this.delta = delta;
	}

	public Shapes.Part part() {
		return this.part;
	}

	/** Déjà à la borne dans ce sens : la dose ne servirait à rien. */
	private boolean auBout(LivingEntity cible) {
		int n = Shapes.niveau(cible, this.part);
		return this.delta > 0 ? n >= this.part.max : n <= this.part.min;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (this.auBout(user)) {
			if (!world.isClient()) {
				user.sendMessage(Text.translatable("message.jn1.shape.limit"), true);
			}
			return TypedActionResult.fail(user.getStackInHand(hand));
		}
		return ItemUsage.consumeHeldItem(world, user, hand);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof ServerPlayerEntity joueur) {
			int n = Shapes.changer(joueur, this.part, this.delta);
			joueur.sendMessage(message(this.part, n), true);
		}
		return vider(stack, user);
	}

	/** Tendue à une compagne : elle la boit. Appelé côté serveur. */
	public void faireBoire(CompanionEntity fille, PlayerEntity player, Hand hand) {
		if (this.auBout(fille)) {
			player.sendMessage(Text.translatable("message.jn1.shape.limit"), true);
			return;
		}
		int n = Shapes.changer(fille, this.part, this.delta);
		fille.getWorld().playSound(null, fille.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK,
				SoundCategory.NEUTRAL, 0.8F, 1.1F);
		player.sendMessage(message(this.part, n), true);
		ItemStack reste = vider(player.getStackInHand(hand), player);
		player.setStackInHand(hand, reste);
	}

	private static Text message(Shapes.Part part, int n) {
		return Text.translatable("message.jn1.shape." + part.id(), (n > 0 ? "+" : "") + n, part.max);
	}

	private static ItemStack vider(ItemStack stack, LivingEntity user) {
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
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.jn1.shape_potion.tooltip").formatted(Formatting.GRAY));
	}
}
