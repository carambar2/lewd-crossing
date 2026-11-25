package dev.pyrofetch.jn1.shape;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

	public ShapePotionItem(Shapes.Part part, int delta, Properties settings) {
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
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		if (this.auBout(user)) {
			if (!world.isClientSide()) {
				user.sendOverlayMessage(Component.translatable("message.jn1.shape.limit"));
			}
			return InteractionResult.FAIL;
		}
		return ItemUtils.startUsingInstantly(world, user, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (user instanceof ServerPlayer joueur) {
			int n = Shapes.changer(joueur, this.part, this.delta);
			joueur.sendOverlayMessage(message(this.part, n));
		}
		return vider(stack, user);
	}

	/** Tendue à une compagne : elle la boit. Appelé côté serveur. */
	public void faireBoire(CompanionEntity fille, Player player, InteractionHand hand) {
		if (this.auBout(fille)) {
			player.sendOverlayMessage(Component.translatable("message.jn1.shape.limit"));
			return;
		}
		int n = Shapes.changer(fille, this.part, this.delta);
		fille.level().playSound(null, fille.blockPosition(), SoundEvents.GENERIC_DRINK.value(),
				SoundSource.NEUTRAL, 0.8F, 1.1F);
		player.sendOverlayMessage(message(this.part, n));
		ItemStack reste = vider(player.getItemInHand(hand), player);
		player.setItemInHand(hand, reste);
	}

	private static Component message(Shapes.Part part, int n) {
		return Component.translatable("message.jn1.shape." + part.id(), (n > 0 ? "+" : "") + n, part.max);
	}

	private static ItemStack vider(ItemStack stack, LivingEntity user) {
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
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		tooltip.accept(Component.translatable("item.jn1.shape_potion.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
