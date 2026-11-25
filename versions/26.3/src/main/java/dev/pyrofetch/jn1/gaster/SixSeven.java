package dev.pyrofetch.jn1.gaster;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerPlayer;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * « Six seven » : si on écrit 67 (6-7, six seven, et tous les dérivés) dans le
 * chat à côté de Gaster, il fait la danse du six seven, avec ses mains.
 *
 * <p>Le message est ramené à une forme simple (minuscules, sans accents ni
 * émojis de touches, séparateurs → espaces) puis confronté à : 67 seul (pas
 * dans 1675), 6 7, 6-7, 6.7, six seven, sixseven, six 7, 6 seven, six sept,
 * soixante-sept, sixty seven. Chaque Gaster à {@link #PORTÉE} blocs du joueur danse.
 */
public final class SixSeven {
	/** À cette distance, Gaster entend. */
	public static final double PORTÉE = 24.0;

	private static final Pattern SIX_SEVEN = Pattern.compile(
			"(?<![0-9])(?:6 ?7 ?)+(?![0-9])"
			+ "|\\b(?:six|6) ?(?:seven|sept|7)\\b"
			+ "|\\bsix ?seven\\b"
			+ "|\\bsoixante ?sept\\b"
			+ "|\\bsixty ?seven\\b");

	private SixSeven() {
	}

	public static void register() {
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			if (reconnaît(message.decoratedContent().getString())) {
				sender.level().getServer().execute(() -> danser(sender));
			}
		});
	}

	/** Vrai si le message dit « six seven », sous une de ses formes. */
	public static boolean reconnaît(String message) {
		String simple = Normalizer.normalize(message.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")                 // accents, et la touche des émojis 6️⃣ 7️⃣
				.replaceAll("[\\uFE0F\\u20E3]", "")
				.replaceAll("[-_.,/|+~*'’]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return SIX_SEVEN.matcher(simple).find();
	}

	private static void danser(ServerPlayer joueur) {
		for (GasterEntity gaster : joueur.level().getEntitiesOfClass(GasterEntity.class,
				joueur.getBoundingBox().inflate(PORTÉE), g -> g.isAlive())) {
			gaster.danser(joueur);
		}
	}
}
