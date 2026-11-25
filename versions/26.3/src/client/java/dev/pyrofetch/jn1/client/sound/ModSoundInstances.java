package dev.pyrofetch.jn1.client.sound;

import dev.pyrofetch.jn1.lift.PlatformEntity;
import dev.pyrofetch.jn1.registry.ModSounds;
import dev.pyrofetch.jn1.rift.RiftEntity;
import dev.pyrofetch.jn1.terminal.Radio;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Les sons qui durent : radio, bourdon de la faille, moteur de la cabine.
 *
 * <p>Chacun se surveille lui-même et s'arrête quand sa source disparaît — le
 * terminal cassé, la faille refermée, la cabine retirée. Rien n'a besoin de
 * penser à les couper, et un monde quitté les emporte avec lui.
 */
public final class ModSoundInstances {
	/** Les radios en cours, par terminal : une seule à la fois par machine. */
	private static final Map<BlockPos, RadioSound> RADIOS = new HashMap<>();

	private ModSoundInstances() {
	}

	/** Le battement d'un terminal : allume, change ou laisse sa radio. */
	public static void radio(Level monde, BlockPos pos, BlockState état) {
		Radio.Station station = Radio.station(état);
		RadioSound enCours = RADIOS.get(pos);
		// Un changement de dimension ou F3+T coupe tous les sons sans les dire
		// finis : sans le second test, la radio restait muette pour de bon.
		if (enCours != null && (enCours.isStopped() || enCours.station != station
				|| !Minecraft.getInstance().getSoundManager().isActive(enCours))) {
			enCours.arrêter();
			RADIOS.remove(pos);
			enCours = null;
		}
		if (station != null && enCours == null) {
			RadioSound son = new RadioSound(station, pos.immutable());
			RADIOS.put(pos.immutable(), son);
			Minecraft.getInstance().getSoundManager().play(son);
		}
	}

	public static void clear() {
		RADIOS.clear();
	}

	/** À l'arrivée d'une entité chez le client : la faille bourdonne, la cabine ronronne. */
	public static void entitéChargée(Entity entité) {
		if (entité instanceof RiftEntity faille) {
			Minecraft.getInstance().getSoundManager().play(new BourdonDeFaille(faille));
		} else if (entité instanceof PlatformEntity cabine) {
			Minecraft.getInstance().getSoundManager().play(new MoteurDeCabine(cabine));
		}
	}

	/** Une station jouée par un terminal, en boucle, tant qu'il la joue. */
	private static final class RadioSound extends AbstractTickableSoundInstance {
		private final Radio.Station station;
		private final BlockPos pos;

		RadioSound(Radio.Station station, BlockPos pos) {
			super(station.son(), SoundSource.RECORDS, RandomSource.create());
			this.station = station;
			this.pos = pos;
			this.looping = true;
			this.delay = 0;
			this.volume = 1.0F;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			this.x = pos.getX() + 0.5;
			this.y = pos.getY() + 0.5;
			this.z = pos.getZ() + 0.5;
		}

		void arrêter() {
			this.stop();
		}

		@Override
		public void tick() {
			Minecraft client = Minecraft.getInstance();
			if (client.level == null || Radio.station(client.level.getBlockState(this.pos)) != this.station) {
				this.stop();
			}
		}
	}

	/** Un son qui suit une entité, et se tait quand elle s'en va. */
	private abstract static class SonDEntité<T extends Entity> extends AbstractTickableSoundInstance {
		protected final T source;

		SonDEntité(SoundEvent son, T source) {
			super(son, SoundSource.NEUTRAL, RandomSource.create());
			this.source = source;
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			this.x = source.getX();
			this.y = source.getY();
			this.z = source.getZ();
		}

		/** Le volume voulu à cet instant ; zéro fait taire sans arrêter. */
		protected abstract float volumeVoulu();

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public boolean canPlaySound() {
			return !this.source.isSilent();
		}

		@Override
		public void tick() {
			if (this.source.isRemoved()) {
				this.stop();
				return;
			}
			this.x = this.source.getX();
			this.y = this.source.getY() + this.source.getBbHeight() / 2.0;
			this.z = this.source.getZ();
			this.volume = this.volumeVoulu();
		}
	}

	private static final class BourdonDeFaille extends SonDEntité<RiftEntity> {
		BourdonDeFaille(RiftEntity faille) {
			super(ModSounds.RIFT_IDLE, faille);
		}

		@Override
		protected float volumeVoulu() {
			return this.source.ouverture(0.0F);
		}
	}

	private static final class MoteurDeCabine extends SonDEntité<PlatformEntity> {
		MoteurDeCabine(PlatformEntity cabine) {
			super(ModSounds.ELEVATOR_MOVE, cabine);
		}

		@Override
		protected float volumeVoulu() {
			return this.source.enMouvement() ? 0.55F : 0.0F;
		}
	}
}
