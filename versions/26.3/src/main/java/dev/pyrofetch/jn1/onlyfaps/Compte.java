package dev.pyrofetch.jn1.onlyfaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Le compte OnlyFaps d'un joueur : ses abonnés, et ce dont ils sont lassés.
 *
 * @param abonnés   ils font monter les prix, jusqu'au double à mille
 * @param lassitude combien de fois chaque contenu (fille et scène, ou fille et
 *                  tenue) a été publié ; chaque fois retire 40 % au suivant
 * @param jour      le jour du monde où la lassitude a été comptée : elle
 *                  retombe d'un cran par jour
 */
public record Compte(int abonnés, Map<String, Integer> lassitude, long jour) {
	public static final Compte NEUF = new Compte(0, Map.of(), 0);
	public static final Codec<Compte> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("subscribers", 0).forGetter(Compte::abonnés),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("fatigue", Map.of()).forGetter(Compte::lassitude),
			Codec.LONG.optionalFieldOf("day", 0L).forGetter(Compte::jour)
	).apply(i, Compte::new));

	/** Le compte au jour dit : la lassitude a retombé d'un cran par jour passé. */
	public Compte au(long aujourdHui) {
		long passés = aujourdHui - this.jour;
		if (passés <= 0) {
			return this;
		}
		Map<String, Integer> reste = new HashMap<>();
		this.lassitude.forEach((clé, fois) -> {
			long n = fois - passés;
			if (n > 0) {
				reste.put(clé, (int) n);
			}
		});
		return new Compte(this.abonnés, Map.copyOf(reste), aujourdHui);
	}

	public int lassitude(String clé) {
		return this.lassitude.getOrDefault(clé, 0);
	}

	public Compte publié(String clé, int nouveaux) {
		Map<String, Integer> suite = new HashMap<>(this.lassitude);
		suite.merge(clé, 1, Integer::sum);
		return new Compte(this.abonnés + nouveaux, Map.copyOf(suite), this.jour);
	}
}
