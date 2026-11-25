package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModComponents;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * La capture, qu'elle vienne d'un jet ou d'un contact.
 *
 * <p>Les deux chemins produisent exactement la même capsule : le geste choisi
 * ne doit rien changer à ce qu'on récupère.
 */
public final class CapsuleCapture {
	private CapsuleCapture() {
	}

	/**
	 * Peut-on la capturer ? Une compagne : pas en scène ni menottée, ni FapGPT en veille ; et
	 * pas la compagne d'un autre joueur.
	 *
	 * <p>Depuis 0.56.9, toute créature se capture aussi : pas un joueur, pas le dragon (fait de morceaux), pas un animal
	 * apprivoisé par un autre, et jamais Gaster ({@link #refusGaster}).
	 */
	public static boolean capturable(net.minecraft.world.entity.LivingEntity cible, ServerPlayer captor) {
		if (!cible.isAlive() || cible instanceof net.minecraft.world.entity.player.Player
				|| cible instanceof dev.pyrofetch.jn1.gaster.GasterEntity
				|| cible instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
			return false;
		}
		if (cible instanceof CompanionEntity jenny) {
			if (jenny.hasScene() || jenny.isCuffed() || jenny.estEnVeille() || !jenny.peutQuitterSaPlace()) {
				return false;
			}
			java.util.UUID maître = jenny.getCompanionUuid();
			return maître == null || captor != null && maître.equals(captor.getUUID());
		}
		if (cible instanceof net.minecraft.world.entity.TamableAnimal apprivoisé && apprivoisé.isTame()) {
			net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.LivingEntity> maître = apprivoisé.getOwnerReference();
			return captor != null && maître != null && captor.getUUID().equals(maître.getUUID());
		}
		return cible instanceof net.minecraft.world.entity.Mob;
	}

	/**
	 * Range la créature dans une capsule et la fait tomber sur place.
	 *
	 * @param captor celui à qui l'on doit l'affection enregistrée, ou {@code null}
	 * @return la capsule pleine
	 */
	public static ItemStack capture(net.minecraft.world.entity.LivingEntity cible, ServerPlayer captor) {
		// L'affection appartient au joueur, pas à l'entité : c'est celle du
		// ravisseur que la capsule emporte. Une créature qui n'est pas une
		// compagne n'en a pas (-1, que l'infobulle tait).
		int affection = cible instanceof CompanionEntity jenny
				? captor != null ? ModAttachments.get(captor).trust(jenny.kind().id()) : 0
				: -1;
		if (cible instanceof net.minecraft.world.entity.Mob mob && mob.isLeashed()) {
			mob.dropLeash();
		}
		cible.stopRiding();
		cible.ejectPassengers();

		// 26.3 : l'entité s'écrit dans une sortie de valeurs, d'où l'on tire le
		// même NBT qu'avant.
		CompoundTag data;
		try (net.minecraft.util.ProblemReporter.ScopedCollector rapport =
				     new net.minecraft.util.ProblemReporter.ScopedCollector(cible.problemPath(), dev.pyrofetch.jn1.JN1.LOGGER)) {
			net.minecraft.world.level.storage.TagValueOutput sortie =
					net.minecraft.world.level.storage.TagValueOutput.createWithContext(rapport, cible.registryAccess());
			cible.saveAsPassenger(sortie);
			data = sortie.buildResult();
		}

		ItemStack filled = new ItemStack(ModItems.CAPSULE);
		filled.set(ModComponents.CAPSULE_CONTENTS, new CapsuleContents(
				cible.getName().getString(), affection, data));

		ServerLevel world = (ServerLevel) cible.level();
		world.sendParticles(ParticleTypes.END_ROD,
				cible.getX(), cible.getY() + cible.getBbHeight() * 0.5, cible.getZ(),
				40, 0.3, 0.6, 0.3, 0.06);
		world.playSound(null, cible.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
				SoundSource.NEUTRAL, 0.7F, 1.6F);

		world.addFreshEntity(new ItemEntity(world,
				cible.getX(), cible.getY() + 0.4, cible.getZ(), filled));
		cible.discard();
		return filled;
	}

	/** Combien de répliques de Gaster quand on veut le capturer ({@code message.jn1.gaster.capsule.<n>}). */
	private static final int REFUS_GASTER = 5;

	/**
	 * Gaster ne tient dans aucune capsule. La capsule lui revient aux pieds du lanceur, la
	 * lumière se retire (obscurité, comme le Warden), un battement sourd, des
	 * âmes qui s'échappent de lui, et il parle.
	 */
	public static void refusGaster(dev.pyrofetch.jn1.gaster.GasterEntity gaster, @org.jetbrains.annotations.Nullable
			ServerPlayer lanceur, ItemStack capsule) {
		ServerLevel world = (ServerLevel) gaster.level();
		world.sendParticles(ParticleTypes.SCULK_SOUL, gaster.getX(), gaster.getY() + 1.6, gaster.getZ(),
				24, 0.4, 0.6, 0.4, 0.02);
		world.sendParticles(ParticleTypes.SMOKE, gaster.getX(), gaster.getY() + 1.0, gaster.getZ(),
				30, 0.5, 1.0, 0.5, 0.01);
		world.playSound(null, gaster.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
				2.0F, 0.6F);
		world.playSound(null, gaster.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
				1.0F, 0.5F);
		net.minecraft.world.phys.Vec3 où = lanceur != null ? lanceur.position() : gaster.position();
		world.addFreshEntity(new ItemEntity(world, où.x, où.y + 0.5, où.z, capsule.copyWithCount(1)));
		if (lanceur != null) {
			lanceur.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.DARKNESS, 120, 0, false, false));
			gaster.getLookControl().setLookAt(lanceur);
			int n = world.getRandom().nextInt(REFUS_GASTER);
			lanceur.sendSystemMessage(net.minecraft.network.chat.Component.literal("Gaster: ").withStyle(net.minecraft.ChatFormatting.GRAY)
					.append(net.minecraft.network.chat.Component.translatable("message.jn1.gaster.capsule." + n)
							.withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC)));
		}
	}
}
