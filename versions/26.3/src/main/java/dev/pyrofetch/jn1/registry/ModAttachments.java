package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

public final class ModAttachments {
	/** Confiance et progression narrative, par joueur, conservées à la mort. */
	public static final AttachmentType<PlayerProgress> PROGRESS = AttachmentRegistry
			.<PlayerProgress>builder()
			.initializer(PlayerProgress::empty)
			.persistent(PlayerProgress.CODEC)
			.copyOnDeath()
			.buildAndRegister(JN1.id("progress"));

	/**
	 * Tick jusqu'auquel le sculk n'entend plus ce joueur (diapason).
	 * Volatile : un redémarrage annule le silence, c'est voulu.
	 */
	public static final AttachmentType<Long> SILENCED_UNTIL = AttachmentRegistry
			.<Long>builder()
			.initializer(() -> 0L)
			.buildAndRegister(JN1.id("silenced_until"));

	/**
	 * Tick jusqu'auquel une faille ne reprend pas ce joueur.
	 *
	 * <p>Sans ce répit, celui qu'une faille vient de rendre, posé à deux pas
	 * d'elle, pourrait y retomber d'un faux pas. Volatile, comme le silence.
	 */
	public static final AttachmentType<Long> RIFT_COOLDOWN_UNTIL = AttachmentRegistry
			.<Long>builder()
			.initializer(() -> 0L)
			.buildAndRegister(JN1.id("rift_cooldown_until"));

	/**
	 * La transformation : en quelle compagne il est, dans quelle tenue.
	 * Conservée à la mort — elle ne cesse que si le joueur l'annule.
	 */
	public static final AttachmentType<dev.pyrofetch.jn1.morph.MorphState> MORPH = AttachmentRegistry
			.<dev.pyrofetch.jn1.morph.MorphState>builder()
			.initializer(() -> dev.pyrofetch.jn1.morph.MorphState.NONE)
			.persistent(dev.pyrofetch.jn1.morph.MorphState.CODEC)
			.copyOnDeath()
			.buildAndRegister(JN1.id("morph"));

	/** Homme ou femme ({@link dev.pyrofetch.jn1.gender.Gender}) ; vide tant qu'il n'a pas répondu. */
	public static final AttachmentType<String> GENDER = AttachmentRegistry
			.<String>builder()
			.initializer(() -> "")
			.persistent(com.mojang.serialization.Codec.STRING)
			.copyOnDeath()
			.buildAndRegister(JN1.id("gender"));

	/** Le compte OnlyFaps : abonnés et lassitude ({@link dev.pyrofetch.jn1.onlyfaps.OnlyFaps}). */
	public static final AttachmentType<dev.pyrofetch.jn1.onlyfaps.Compte> ONLYFAPS = AttachmentRegistry
			.<dev.pyrofetch.jn1.onlyfaps.Compte>builder()
			.initializer(() -> dev.pyrofetch.jn1.onlyfaps.Compte.NEUF)
			.persistent(dev.pyrofetch.jn1.onlyfaps.Compte.CODEC)
			.copyOnDeath()
			.buildAndRegister(JN1.id("onlyfaps"));

	private ModAttachments() {
	}

	public static boolean riftReady(net.minecraft.world.entity.Entity entity) {
		Long until = entity.getAttached(RIFT_COOLDOWN_UNTIL);
		return until == null || entity.level().getGameTime() >= until;
	}

	/**
	 * Compté sur le temps du monde, que toutes les dimensions partagent — les
	 * autres le lisent dans celui du monde principal : le répit posé dans le
	 * vide vaut donc aussi à la surface.
	 */
	public static void setRiftCooldown(net.minecraft.world.entity.Entity entity, int ticks) {
		entity.setAttached(RIFT_COOLDOWN_UNTIL, entity.level().getGameTime() + ticks);
	}

	/** Vrai tant que le diapason couvre les vibrations du joueur. */
	public static boolean isSilenced(net.minecraft.world.entity.Entity entity) {
		Long until = entity.getAttached(SILENCED_UNTIL);
		return until != null && entity.level().getGameTime() < until;
	}

	public static PlayerProgress get(Player player) {
		return player.getAttachedOrCreate(PROGRESS);
	}

	public static void set(Player player, PlayerProgress progress) {
		player.setAttached(PROGRESS, progress);
	}

	public static dev.pyrofetch.jn1.morph.MorphState morph(Player player) {
		return player.getAttachedOrCreate(MORPH);
	}

	public static void setMorph(Player player, dev.pyrofetch.jn1.morph.MorphState state) {
		player.setAttached(MORPH, state);
	}

	public static void register() {
		// Le chargement de la classe suffit à enregistrer l'attachment.
	}
}
