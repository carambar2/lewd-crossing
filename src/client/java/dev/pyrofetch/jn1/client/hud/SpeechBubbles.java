package dev.pyrofetch.jn1.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Les bulles actuellement à l'écran, indexées par entité. Purement client :
 * le serveur a déjà décidé du texte, on ne fait que le tenir affiché.
 */
public final class SpeechBubbles {
	/** Largeur maximale d'une bulle, en pixels de police. */
	public static final int MAX_WIDTH = 180;
	/** Durée du fondu de sortie. */
	private static final int FADE_TICKS = 12;

	private static final Map<Integer, Bubble> ACTIVE = new HashMap<>();
	private static long clientTicks;

	private SpeechBubbles() {
	}

	public static void show(int entityId, Text text, int durationTicks) {
		TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
		List<OrderedText> lines = renderer.wrapLines(text, MAX_WIDTH);
		ACTIVE.put(entityId, new Bubble(lines, clientTicks + durationTicks));
	}

	@Nullable
	public static Bubble get(int entityId) {
		return ACTIVE.get(entityId);
	}

	public static void tick() {
		clientTicks++;
		Iterator<Map.Entry<Integer, Bubble>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			if (clientTicks > iterator.next().getValue().expiryTick()) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static long clientTicks() {
		return clientTicks;
	}

	public record Bubble(List<OrderedText> lines, long expiryTick) {
		/** 1 au plus fort, puis décroît sur les derniers ticks. */
		public float opacity() {
			long remaining = this.expiryTick - clientTicks;
			if (remaining >= FADE_TICKS) {
				return 1.0F;
			}
			return Math.max(0.0F, remaining / (float) FADE_TICKS);
		}
	}
}
