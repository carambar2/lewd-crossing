package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * La capsule.
 *
 * <p>Vide ou pleine, elle se lance : vide, elle capture ; pleine, elle relâche
 * sa captive là où elle touche (0.56.9). Manquée,
 * elle retombe telle quelle : rien ne se perd.
 */
public class CapsuleItem extends Item {
	public CapsuleItem(Settings settings) {
		super(settings);
	}

	public static boolean isFull(ItemStack stack) {
		return stack.get(ModComponents.CAPSULE_CONTENTS) != null;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.6F);
		if (!world.isClient()) {
			CapsuleEntity thrown = new CapsuleEntity(world, player);
			thrown.setItem(stack);
			thrown.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.4F, 1.0F);
			world.spawnEntity(thrown);
		}
		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	/**
	 * Écarts de hauteur essayés autour du point visé, dans l'ordre.
	 *
	 * <p>On monte avant de descendre : le défaut courant est de viser trop bas,
	 * et la surface est alors au-dessus.
	 */
	private static final int[] LIFTS = {0, 1, 2, 3, -1, -2};

	/**
	 * Le premier emplacement où elle tient, au plus près du point visé.
	 *
	 * <p>{@code spawnEntity} ne repousse pas une entité hors des blocs : posée
	 * dans le sol, elle y reste plantée. La place est donc cherchée ici, et à
	 * défaut on la rend au joueur — là où il se tient est libre par construction.
	 */
	private static Vec3d freeSpot(ServerWorld world, Entity entity, Vec3d wanted, Vec3d fallback) {
		for (int lift : LIFTS) {
			Vec3d attempt = wanted.add(0.0, lift, 0.0);
			if (world.isSpaceEmpty(entity,
					entity.getType().getDimensions().getBoxAt(attempt))) {
				return attempt;
			}
		}
		return fallback;
	}

	/** Reconstruit la captive telle qu'elle a été capturée. */
	public static boolean release(ItemStack stack, ServerWorld world, Vec3d pos, Vec3d fallback) {
		CapsuleContents contents = stack.get(ModComponents.CAPSULE_CONTENTS);
		if (contents == null) {
			return false;
		}
		// Le type est relu dans l'état sauvegardé : une capsule ne rend pas
		// forcément une Jenny, et recréer un type fixe échangeait la captive.
		Entity restored = EntityType.getEntityFromNbt(contents.entityData(), world).orElse(null);
		if (restored == null) {
			return false;
		}
		restored.readNbt(contents.entityData());
		// Sans nouvel identifiant, relâcher deux captives issues de la même
		// compagne n'en produirait qu'une : le monde refuse les doublons.
		restored.setUuid(java.util.UUID.randomUUID());
		Vec3d spot = freeSpot(world, restored, pos, fallback);
		restored.refreshPositionAndAngles(spot.x, spot.y, spot.z, restored.getYaw(), 0.0F);
		if (restored instanceof CompanionEntity jenny) {
			jenny.showHearts(5);
		}
		return world.spawnEntity(restored);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context,
	                          List<Text> tooltip, TooltipType type) {
		CapsuleContents contents = stack.get(ModComponents.CAPSULE_CONTENTS);
		if (contents == null) {
			tooltip.add(Text.translatable("item.jn1.capsule.empty").formatted(Formatting.DARK_GRAY));
			return;
		}
		tooltip.add(Text.literal(contents.name()).formatted(Formatting.LIGHT_PURPLE));
		// Une créature qui n'est pas une compagne n'a pas d'affection (-1).
		if (contents.affection() < 0) {
			return;
		}
		tooltip.add(Text.translatable("item.jn1.capsule.affection",
				contents.affection(), dev.pyrofetch.jn1.progress.PlayerProgress.MAX_TRUST)
				.formatted(Formatting.GRAY));
	}
}
