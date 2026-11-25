package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.registry.ModSounds;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ce qui tombe au fond du vide est recraché.
 *
 * <p>Sous un certain seuil, le vide rend le joueur au monde d'où il venait —
 * là où il était entré par une faille, ou devant la porte de sa cave. Sans la
 * tenue, la chute se paie : quelques cœurs, jamais le dernier. Avec, c'est la
 * façon de repartir : on plonge, et l'on ressort.
 *
 * <p>La mort par le vide du jeu n'intervient que bien plus bas, soixante-quatre
 * blocs sous le fond de la dimension. On passe donc toujours par ici avant.
 */
public final class VoidFall {
	/** En dessous, on est rendu. Le chemin et le laboratoire sont à 40. */
	public static final double SEUIL = 8.0;
	/** Ce que coûte la chute sans la tenue, en points de vie. */
	private static final float PRIX = 6.0F;
	/** Le temps pendant lequel une faille ne reprend pas celui qu'elle vient de rendre. */
	public static final int RÉPIT = 100;

	private VoidFall() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(VoidFall::tick);
	}

	private static void tick(MinecraftServer server) {
		ServerLevel vide = server.getLevel(LabDimension.LAB);
		if (vide == null) {
			return;
		}
		// Copie : renvoyer un joueur le retire de la liste qu'on parcourt.
		for (ServerPlayer joueur : List.copyOf(vide.players())) {
			if (joueur.getY() < SEUIL && !joueur.isSpectator()) {
				rendre(joueur);
			}
		}
	}

	private static void rendre(ServerPlayer joueur) {
		boolean enTenue = VoidSwim.tenueComplète(joueur);
		LabDimension.leave(joueur);
		joueur.setDeltaMovement(Vec3.ZERO);
		joueur.fallDistance = 0.0F;
		ModAttachments.setRiftCooldown(joueur, RÉPIT);

		ServerLevel monde = joueur.level();
		monde.playSound(null, joueur.blockPosition(), ModSounds.RIFT_EJECT, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (!enTenue && !joueur.isCreative()) {
			float dégâts = Math.min(PRIX, joueur.getHealth() - 1.0F);
			if (dégâts > 0.0F) {
				joueur.hurt(monde.damageSources().fall(), dégâts);
			}
		}
	}
}
