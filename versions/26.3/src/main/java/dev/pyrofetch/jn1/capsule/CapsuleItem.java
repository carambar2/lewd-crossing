package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * La capsule.
 *
 * <p>Vide ou pleine, elle se lance : vide, elle capture ; pleine, elle relâche
 * sa captive là où elle touche (0.56.9). Manquée,
 * elle retombe telle quelle : rien ne se perd.
 */
public class CapsuleItem extends Item {
	public CapsuleItem(Properties settings) {
		super(settings);
	}

	public static boolean isFull(ItemStack stack) {
		return stack.get(ModComponents.CAPSULE_CONTENTS) != null;
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.6F);
		if (!world.isClientSide()) {
			CapsuleEntity thrown = new CapsuleEntity(world, player);
			thrown.setItem(stack);
			thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.4F, 1.0F);
			world.addFreshEntity(thrown);
		}
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
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
	private static Vec3 freeSpot(ServerLevel world, Entity entity, Vec3 wanted, Vec3 fallback) {
		for (int lift : LIFTS) {
			Vec3 attempt = wanted.add(0.0, lift, 0.0);
			if (world.noCollision(entity,
					entity.getType().getDimensions().makeBoundingBox(attempt))) {
				return attempt;
			}
		}
		return fallback;
	}

	/** Reconstruit la captive telle qu'elle a été capturée. */
	public static boolean release(ItemStack stack, ServerLevel world, Vec3 pos, Vec3 fallback) {
		CapsuleContents contents = stack.get(ModComponents.CAPSULE_CONTENTS);
		if (contents == null) {
			return false;
		}
		// Le type est relu dans l'état sauvegardé : une capsule ne rend pas
		// forcément une Jenny, et recréer un type fixe échangeait la captive.
		// 26.3 : lue par une entrée de valeurs ; la création la charge aussi. Sans
		// vérification d'apparition (paisible) : elle existait déjà, comme avant.
		Entity restored;
		try (net.minecraft.util.ProblemReporter.ScopedCollector rapport =
				     new net.minecraft.util.ProblemReporter.ScopedCollector(dev.pyrofetch.jn1.JN1.LOGGER)) {
			restored = EntityType.create(
					net.minecraft.world.level.storage.TagValueInput.create(rapport, world.registryAccess(), contents.entityData()),
					world, new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD, true))
					.orElse(null);
		}
		if (restored == null) {
			return false;
		}
		// Sans nouvel identifiant, relâcher deux captives issues de la même
		// compagne n'en produirait qu'une : le monde refuse les doublons.
		restored.setUUID(java.util.UUID.randomUUID());
		Vec3 spot = freeSpot(world, restored, pos, fallback);
		restored.snapTo(spot.x, spot.y, spot.z, restored.getYRot(), 0.0F);
		if (restored instanceof CompanionEntity jenny) {
			jenny.showHearts(5);
		}
		return world.addFreshEntity(restored);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay affichage,
			java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
		CapsuleContents contents = stack.get(ModComponents.CAPSULE_CONTENTS);
		if (contents == null) {
			tooltip.accept(Component.translatable("item.jn1.capsule.empty").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.accept(Component.literal(contents.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
		// Une créature qui n'est pas une compagne n'a pas d'affection (-1).
		if (contents.affection() < 0) {
			return;
		}
		tooltip.accept(Component.translatable("item.jn1.capsule.affection",
				contents.affection(), dev.pyrofetch.jn1.progress.PlayerProgress.MAX_TRUST)
				.withStyle(ChatFormatting.GRAY));
	}
}
