package dev.pyrofetch.jn1.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Ce que Jenny sait d'un joueur donné. Persisté par joueur via la Data
 * Attachment API : dans une partie multijoueur, deux personnes peuvent être à
 * des actes différents de l'histoire.
 *
 * @param trust      l'affection, par compagne. Chacune la gagne pour son propre
 *                   compte : la partager reviendrait à dire que gâter l'une
 *                   rapproche les autres.
 * @param flags      progression narrative (« act2_started », « saw_glyph_wall »…)
 * @param spokenOnce identifiants des répliques marquées {@code once} déjà dites
 * @param labReturnWorld monde d'où l'on est entré au laboratoire, {@code null}
 *                       si l'on n'y est jamais allé
 * @param labReturnPos   où reposer les pieds en ressortant
 */
public record PlayerProgress(Map<String, Integer> trust, Set<String> flags, Set<String> spokenOnce,
                             String labReturnWorld, net.minecraft.core.BlockPos labReturnPos) {
	public static final int MIN_TRUST = 0;
	/**
	 * Sur 20 depuis 0.52.0 (10 avant) : au-delà des tenues, ce qu'on peut lui
	 * demander se débloque de 12 à 18 ({@link dev.pyrofetch.jn1.entity.Demande}),
	 * et la brutalité au maximum. Les valeurs des mondes existants restent telles quelles.
	 */
	public static final int MAX_TRUST = 20;

	/**
	 * Seuils d'affection auxquels les gestes du menu se débloquent. Ils sont ici
	 * plutôt que dans l'écran : le serveur revérifie chaque demande, et deux
	 * chiffres tenus à deux endroits finissent toujours par diverger.
	 */
	public static final int HUG_TRUST = 5;

	/**
	 * Le seuil du câlin pour cette compagne. Celui d'Aura est nul : son câlin
	 * établit la liaison, sans laquelle ni lidar ni terminal ne répondent — le
	 * soumettre à l'affection bloquerait tout le reste.
	 */
	public static int hugTrust(dev.pyrofetch.jn1.entity.CompanionKind kind) {
		return kind == dev.pyrofetch.jn1.entity.CompanionKind.AURA ? 0 : HUG_TRUST;
	}
	/** Les tenues : 10, l'ancien maximum. */
	public static final int CHANGE_TRUST = 10;
	/** Demander « brutalement » : l'affection pleine. */
	public static final int BRUTAL_TRUST = MAX_TRUST;

	private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf()
			.xmap(LinkedHashSet::new, List::copyOf);
	/** La seule compagne qui existait quand l'affection était unique. */
	private static final String LEGACY_COMPANION = "jenny";

	private static final Codec<Map<String, Integer>> TRUST_MAP =
			Codec.unboundedMap(Codec.STRING, Codec.INT);

	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			TRUST_MAP.optionalFieldOf("trust_by_companion", Map.of()).forGetter(PlayerProgress::trust),
			// Ancien champ, du temps où l'affection était unique. Il est relu
			// pour ne pas effacer la progression des parties existantes, et
			// n'est plus jamais écrit.
			Codec.INT.optionalFieldOf("trust", 0).forGetter(p -> 0),
			STRING_SET.optionalFieldOf("flags", Set.of()).forGetter(PlayerProgress::flags),
			STRING_SET.optionalFieldOf("spoken_once", Set.of()).forGetter(PlayerProgress::spokenOnce),
			// Le retour est absent tant qu'on n'a pas franchi la porte : une
			// sauvegarde d'avant cette version se relit donc sans rien perdre.
			Codec.STRING.optionalFieldOf("lab_return_world")
					.forGetter(p -> java.util.Optional.ofNullable(p.labReturnWorld())),
			net.minecraft.core.BlockPos.CODEC.optionalFieldOf("lab_return_pos")
					.forGetter(p -> java.util.Optional.ofNullable(p.labReturnPos()))
	).apply(instance, (trust, ancien, flags, spoken, monde, pos) -> {
		Map<String, Integer> table = new LinkedHashMap<>(trust);
		if (ancien > 0 && !table.containsKey(LEGACY_COMPANION)) {
			table.put(LEGACY_COMPANION, ancien);
		}
		return new PlayerProgress(table, flags, spoken, monde.orElse(null), pos.orElse(null));
	}));


	public PlayerProgress {
		Map<String, Integer> borne = new LinkedHashMap<>();
		trust.forEach((nom, valeur) -> borne.put(nom, Math.clamp(valeur, MIN_TRUST, MAX_TRUST)));
		trust = Map.copyOf(borne);
		flags = Set.copyOf(flags);
		spokenOnce = Set.copyOf(spokenOnce);
	}

	/** L'affection portée à cette compagne-là, zéro si l'on ne s'est jamais croisés. */
	public int trust(String companion) {
		return this.trust.getOrDefault(companion, 0);
	}

	public PlayerProgress withTrust(String companion, int valeur) {
		Map<String, Integer> table = new LinkedHashMap<>(this.trust);
		table.put(companion, Math.clamp(valeur, MIN_TRUST, MAX_TRUST));
		return new PlayerProgress(table, this.flags, this.spokenOnce,
				this.labReturnWorld, this.labReturnPos);
	}

	public PlayerProgress addTrust(String companion, int delta) {
		return this.withTrust(companion, this.trust(companion) + delta);
	}

	/** Le retour au laboratoire, retenu au moment où l'on franchit la porte. */
	public PlayerProgress withLabReturn(String monde, net.minecraft.core.BlockPos pos) {
		return new PlayerProgress(this.trust, this.flags, this.spokenOnce, monde, pos);
	}

	public static PlayerProgress empty() {
		return new PlayerProgress(Map.of(), Set.of(), Set.of(), null, null);
	}



	public boolean hasFlag(String flag) {
		return this.flags.contains(flag);
	}

	public PlayerProgress withFlag(String flag) {
		if (this.flags.contains(flag)) {
			return this;
		}
		Set<String> updated = new LinkedHashSet<>(this.flags);
		updated.add(flag);
		return new PlayerProgress(this.trust, updated, this.spokenOnce,
				this.labReturnWorld, this.labReturnPos);
	}

	public PlayerProgress withoutFlag(String flag) {
		if (!this.flags.contains(flag)) {
			return this;
		}
		Set<String> updated = new LinkedHashSet<>(this.flags);
		updated.remove(flag);
		return new PlayerProgress(this.trust, updated, this.spokenOnce,
				this.labReturnWorld, this.labReturnPos);
	}

	public boolean hasSpoken(String entryId) {
		return this.spokenOnce.contains(entryId);
	}

	public PlayerProgress markSpoken(String entryId) {
		if (this.spokenOnce.contains(entryId)) {
			return this;
		}
		Set<String> updated = new LinkedHashSet<>(this.spokenOnce);
		updated.add(entryId);
		return new PlayerProgress(this.trust, this.flags, updated,
				this.labReturnWorld, this.labReturnPos);
	}
}
