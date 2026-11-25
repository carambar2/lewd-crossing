package dev.pyrofetch.jn1.capsule;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModComponents;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

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
	public static boolean capturable(net.minecraft.entity.LivingEntity cible, ServerPlayerEntity captor) {
		if (!cible.isAlive() || cible instanceof net.minecraft.entity.player.PlayerEntity
				|| cible instanceof dev.pyrofetch.jn1.gaster.GasterEntity
				|| cible instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) {
			return false;
		}
		if (cible instanceof CompanionEntity jenny) {
			if (jenny.hasScene() || jenny.isCuffed() || jenny.estEnVeille() || !jenny.peutQuitterSaPlace()) {
				return false;
			}
			java.util.UUID maître = jenny.getCompanionUuid();
			return maître == null || captor != null && maître.equals(captor.getUuid());
		}
		if (cible instanceof net.minecraft.entity.passive.TameableEntity apprivoisé && apprivoisé.isTamed()) {
			return captor != null && captor.getUuid().equals(apprivoisé.getOwnerUuid());
		}
		return cible instanceof net.minecraft.entity.mob.MobEntity;
	}

	/**
	 * Range la créature dans une capsule et la fait tomber sur place.
	 *
	 * @param captor celui à qui l'on doit l'affection enregistrée, ou {@code null}
	 * @return la capsule pleine
	 */
	public static ItemStack capture(net.minecraft.entity.LivingEntity cible, ServerPlayerEntity captor) {
		// L'affection appartient au joueur, pas à l'entité : c'est celle du
		// ravisseur que la capsule emporte. Une créature qui n'est pas une
		// compagne n'en a pas (-1, que l'infobulle tait).
		int affection = cible instanceof CompanionEntity jenny
				? captor != null ? ModAttachments.get(captor).trust(jenny.kind().id()) : 0
				: -1;
		if (cible instanceof net.minecraft.entity.mob.MobEntity mob && mob.isLeashed()) {
			mob.detachLeash(true, true);
		}
		cible.stopRiding();
		cible.removeAllPassengers();

		NbtCompound data = new NbtCompound();
		cible.saveSelfNbt(data);

		ItemStack filled = new ItemStack(ModItems.CAPSULE);
		filled.set(ModComponents.CAPSULE_CONTENTS, new CapsuleContents(
				cible.getName().getString(), affection, data));

		ServerWorld world = (ServerWorld) cible.getWorld();
		world.spawnParticles(ParticleTypes.END_ROD,
				cible.getX(), cible.getY() + cible.getHeight() * 0.5, cible.getZ(),
				40, 0.3, 0.6, 0.3, 0.06);
		world.playSound(null, cible.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
				SoundCategory.NEUTRAL, 0.7F, 1.6F);

		world.spawnEntity(new ItemEntity(world,
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
			ServerPlayerEntity lanceur, ItemStack capsule) {
		ServerWorld world = (ServerWorld) gaster.getWorld();
		world.spawnParticles(ParticleTypes.SCULK_SOUL, gaster.getX(), gaster.getY() + 1.6, gaster.getZ(),
				24, 0.4, 0.6, 0.4, 0.02);
		world.spawnParticles(ParticleTypes.SMOKE, gaster.getX(), gaster.getY() + 1.0, gaster.getZ(),
				30, 0.5, 1.0, 0.5, 0.01);
		world.playSound(null, gaster.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE,
				2.0F, 0.6F);
		world.playSound(null, gaster.getBlockPos(), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT,
				1.0F, 0.5F);
		net.minecraft.util.math.Vec3d où = lanceur != null ? lanceur.getPos() : gaster.getPos();
		world.spawnEntity(new ItemEntity(world, où.x, où.y + 0.5, où.z, capsule.copyWithCount(1)));
		if (lanceur != null) {
			lanceur.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
					net.minecraft.entity.effect.StatusEffects.DARKNESS, 120, 0, false, false));
			gaster.getLookControl().lookAt(lanceur);
			int n = world.getRandom().nextInt(REFUS_GASTER);
			lanceur.sendMessage(net.minecraft.text.Text.literal("Gaster: ").formatted(net.minecraft.util.Formatting.GRAY)
					.append(net.minecraft.text.Text.translatable("message.jn1.gaster.capsule." + n)
							.formatted(net.minecraft.util.Formatting.DARK_GRAY, net.minecraft.util.Formatting.ITALIC)),
					false);
		}
	}
}
