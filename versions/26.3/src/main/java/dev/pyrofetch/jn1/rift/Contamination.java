package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.lab.VoidSwim;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * La contamination : ce qu'on attrape à s'approcher d'une brèche sans la tenue.
 *
 * <p>Elle monte avec le temps passé près de la brèche, en trois paliers. On a
 * d'abord faim ; au bout de dix secondes, on s'affaiblit ; au bout de vingt-cinq,
 * elle ronge — un demi-cœur toutes les deux secondes, jamais le dernier, comme
 * le fond du vide. Elle dure trente secondes après qu'on s'est éloigné, et la
 * douche du sas la lave d'un coup (voir
 * {@link dev.pyrofetch.jn1.block.DeconShowerBlock}), comme le lait.
 *
 * <p>La tenue du vide en protège entièrement : c'est pour cela qu'elle est
 * rangée dans le vestiaire du site.
 */
public final class Contamination {
	public static final Holder<MobEffect> EFFET = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, JN1.id("contamination"), new Effet());

	/** Ce qu'elle dure une fois qu'on ne s'expose plus. */
	private static final int DURÉE = 20 * 30;
	/** Temps d'exposition, en ticks, avant chaque palier. */
	private static final int PALIER_AFFAIBLI = 20 * 10;
	private static final int PALIER_RONGÉ = 20 * 25;

	/**
	 * Le temps d'exposition de chacun.
	 *
	 * <p>Il ne compte que tant que l'effet est là : une fois qu'il s'est dissipé,
	 * ou qu'on l'a lavé, on repart de zéro.
	 */
	private static final Map<UUID, Integer> EXPOSITION = new HashMap<>();

	private Contamination() {
	}

	public static void register() {
		JN1.LOGGER.debug("Contamination enregistrée : {}.", EFFET.getRegisteredName());
	}

	/**
	 * Contamine les joueurs à portée d'une brèche.
	 *
	 * @param pas le temps écoulé depuis le dernier appel, en ticks
	 */
	public static void exposer(ServerLevel monde, Vec3 centre, double rayon, int pas) {
		double rayonCarré = rayon * rayon;
		for (ServerPlayer joueur : monde.getPlayers(j -> j.distanceToSqr(centre) <= rayonCarré)) {
			if (joueur.isSpectator() || joueur.isCreative() || VoidSwim.tenueComplète(joueur)) {
				continue;
			}
			boolean déjà = joueur.hasEffect(EFFET);
			int exposition = déjà ? EXPOSITION.getOrDefault(joueur.getUUID(), 0) + pas : pas;
			EXPOSITION.put(joueur.getUUID(), exposition);

			int palier = exposition >= PALIER_RONGÉ ? 2 : exposition >= PALIER_AFFAIBLI ? 1 : 0;
			joueur.addEffect(new MobEffectInstance(EFFET, DURÉE, palier));
			if (palier >= 1) {
				joueur.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURÉE, 0));
			}
			if (!déjà) {
				joueur.sendOverlayMessage(Component.translatable("effect.jn1.contamination.warning"));
				// Une entrée du carnet de terrain s'écrit à la première fois.
				dev.pyrofetch.jn1.dialogue.DialogueEngine.setFlag(joueur, dev.pyrofetch.jn1.notebook.FieldNotebook.FLAG_BREACH);
			}
		}
	}

	/** Lave la contamination, et l'affaiblissement qui l'accompagne. */
	public static boolean laver(Player joueur) {
		boolean contaminé = joueur.hasEffect(EFFET);
		joueur.removeEffect(EFFET);
		if (contaminé) {
			joueur.removeEffect(MobEffects.WEAKNESS);
		}
		EXPOSITION.remove(joueur.getUUID());
		return contaminé;
	}

	/**
	 * L'effet lui-même.
	 *
	 * <p>Il n'ajoute pas d'autre effet de lui-même : le jeu parcourt la liste des
	 * effets pendant qu'il les applique, et y ajouter quelque chose à ce moment-là
	 * la ferait lever une exception. L'affaiblissement est donc posé par la brèche.
	 */
	private static final class Effet extends MobEffect {
		Effet() {
			super(MobEffectCategory.HARMFUL, 0x3A1F5C);
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int durée, int palier) {
			return durée % 40 == 0;
		}

		@Override
		public boolean applyEffectTick(net.minecraft.server.level.ServerLevel monde, LivingEntity entité, int palier) {
			if (entité instanceof Player joueur) {
				joueur.causeFoodExhaustion(0.8F * (palier + 1));
			}
			if (palier >= 2 && entité.getHealth() > 2.0F) {
				entité.hurtServer(monde, entité.damageSources().magic(), 1.0F);
			}
			return true;
		}
	}
}
