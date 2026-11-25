package dev.pyrofetch.jn1.client.sound;

import dev.pyrofetch.jn1.lift.PlatformEntity;
import dev.pyrofetch.jn1.registry.ModSounds;
import dev.pyrofetch.jn1.rift.RiftEntity;
import dev.pyrofetch.jn1.terminal.Radio;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
	public static void radio(World monde, BlockPos pos, BlockState état) {
		Radio.Station station = Radio.station(état);
		RadioSound enCours = RADIOS.get(pos);
		// Un changement de dimension ou F3+T coupe tous les sons sans les dire
		// finis : sans le second test, la radio restait muette pour de bon.
		if (enCours != null && (enCours.isDone() || enCours.station != station
				|| !MinecraftClient.getInstance().getSoundManager().isPlaying(enCours))) {
			enCours.arrêter();
			RADIOS.remove(pos);
			enCours = null;
		}
		if (station != null && enCours == null) {
			RadioSound son = new RadioSound(station, pos.toImmutable());
			RADIOS.put(pos.toImmutable(), son);
			MinecraftClient.getInstance().getSoundManager().play(son);
		}
	}

	public static void clear() {
		RADIOS.clear();
	}

	/** À l'arrivée d'une entité chez le client : la faille bourdonne, la cabine ronronne. */
	public static void entitéChargée(Entity entité) {
		if (entité instanceof RiftEntity faille) {
			MinecraftClient.getInstance().getSoundManager().play(new BourdonDeFaille(faille));
		} else if (entité instanceof PlatformEntity cabine) {
			MinecraftClient.getInstance().getSoundManager().play(new MoteurDeCabine(cabine));
		}
	}

	/** Une station jouée par un terminal, en boucle, tant qu'il la joue. */
	private static final class RadioSound extends MovingSoundInstance {
		private final Radio.Station station;
		private final BlockPos pos;

		RadioSound(Radio.Station station, BlockPos pos) {
			super(station.son(), SoundCategory.RECORDS, Random.create());
			this.station = station;
			this.pos = pos;
			this.repeat = true;
			this.repeatDelay = 0;
			this.volume = 1.0F;
			this.attenuationType = SoundInstance.AttenuationType.LINEAR;
			this.x = pos.getX() + 0.5;
			this.y = pos.getY() + 0.5;
			this.z = pos.getZ() + 0.5;
		}

		void arrêter() {
			this.setDone();
		}

		@Override
		public void tick() {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null || Radio.station(client.world.getBlockState(this.pos)) != this.station) {
				this.setDone();
			}
		}
	}

	/** Un son qui suit une entité, et se tait quand elle s'en va. */
	private abstract static class SonDEntité<T extends Entity> extends MovingSoundInstance {
		protected final T source;

		SonDEntité(SoundEvent son, T source) {
			super(son, SoundCategory.NEUTRAL, Random.create());
			this.source = source;
			this.repeat = true;
			this.repeatDelay = 0;
			this.volume = 0.0F;
			this.x = source.getX();
			this.y = source.getY();
			this.z = source.getZ();
		}

		/** Le volume voulu à cet instant ; zéro fait taire sans arrêter. */
		protected abstract float volumeVoulu();

		@Override
		public boolean shouldAlwaysPlay() {
			return true;
		}

		@Override
		public boolean canPlay() {
			return !this.source.isSilent();
		}

		@Override
		public void tick() {
			if (this.source.isRemoved()) {
				this.setDone();
				return;
			}
			this.x = this.source.getX();
			this.y = this.source.getY() + this.source.getHeight() / 2.0;
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
