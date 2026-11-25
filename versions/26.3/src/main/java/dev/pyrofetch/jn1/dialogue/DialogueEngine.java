package dev.pyrofetch.jn1.dialogue;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.config.JN1Config;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.network.ProgressSyncPayload;
import dev.pyrofetch.jn1.network.SpeakPayload;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import dev.pyrofetch.jn1.registry.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Choisit ce que Jenny dit, et à qui.
 *
 * <p>Le protocole est celui de la spec : on ramasse les entrées éligibles, on
 * écarte celles en cooldown ou déjà consommées, on ne garde que la priorité la
 * plus haute, puis on tire au poids. Tout se passe côté serveur ; le client
 * reçoit un texte déjà résolu.
 */
public final class DialogueEngine {
	/**
	 * Anti-bavardage : après une réplique, deux minutes sans réplique d'ambiance
	 * (0.56.0 : elles ont désormais de quoi parler ; à dix secondes, elles ne se
	 * taisaient plus).
	 */
	private static final int GLOBAL_AMBIENT_COOLDOWN = 2400;
	/** Durée d'affichage de base, allongée pour les répliques longues. */
	private static final int BASE_DISPLAY_TICKS = 70;
	private static final int TICKS_PER_CHARACTER = 1;
	private static final int MAX_DISPLAY_TICKS = 220;

	/** Cooldowns par joueur puis par entrée, en ticks absolus. Volatile. */
	private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
	/** Fin du silence imposé après une réplique, par joueur. */
	private static final Map<UUID, Long> GLOBAL_COOLDOWN = new HashMap<>();
	/** Répliques planifiées mais pas encore dites. */
	private static final List<PendingLine> PENDING = new ArrayList<>();

	private DialogueEngine() {
	}

	/**
	 * Tente de faire parler Jenny à ce joueur.
	 *
	 * @return vrai si une entrée a été retenue et planifiée
	 */
	public static boolean trySpeak(ServerPlayer player, CompanionEntity jenny, DialogueTrigger trigger) {
		return parler(player, jenny, trigger, null, null, null) != null;
	}

	/** Elle réagit à un événement ({@code hug}, {@code whip}, {@code after}…), si elle a de quoi. */
	public static boolean réagir(ServerPlayer player, CompanionEntity jenny, String event) {
		return parler(player, jenny, DialogueTrigger.EVENT, null, null, event) != null;
	}

	/**
	 * Fait parler une compagne, et rend sa première réplique (ou {@code null} si
	 * rien ne convient) : l'écran « Talk » l'affiche, la bulle la répète.
	 *
	 * @param talk    l'interaction, pour {@link DialogueTrigger#TALK}
	 * @param outcome son issue, ou celle d'un cadeau
	 * @param event   l'événement, pour {@link DialogueTrigger#EVENT}
	 */
	@org.jetbrains.annotations.Nullable
	public static Component parler(ServerPlayer player, CompanionEntity jenny, DialogueTrigger trigger,
	                          @org.jetbrains.annotations.Nullable String talk,
	                          @org.jetbrains.annotations.Nullable String outcome,
	                          @org.jetbrains.annotations.Nullable String event) {
		if (!(jenny.level() instanceof ServerLevel world)) {
			return null;
		}

		long now = world.getServer().getTickCount();
		UUID playerId = player.getUUID();

		if (trigger == DialogueTrigger.AMBIENT) {
			if (!JN1Config.get().ambientDialogueEnabled()) {
				return null;
			}
			if (now < GLOBAL_COOLDOWN.getOrDefault(playerId, 0L)) {
				return null;
			}
		}

		PlayerProgress progress = ModAttachments.get(player);
		DialogueContext context = new DialogueContext(
				world, player, jenny, jenny.blockPosition(), progress, trigger).with(talk, outcome, event);

		List<DialogueEntry> eligible = collectEligible(context, playerId, now);
		if (eligible.isEmpty()) {
			return null;
		}

		RandomSource random = jenny.getRandom();
		DialogueEntry chosen = eligible.size() == 1
				? eligible.getFirst()
				: eligible.get(random.nextInt(eligible.size()));

		Component première = schedule(player, jenny, chosen, now, random);
		commit(player, jenny, chosen, playerId, now);
		return première;
	}

	/** Force une entrée précise, en ignorant conditions et cooldowns. */
	public static boolean forceSpeak(ServerPlayer player, CompanionEntity jenny, String entryId) {
		DialogueEntry entry = DialogueLoader.getInstance().byId(entryId);
		if (entry == null || !(jenny.level() instanceof ServerLevel world)) {
			return false;
		}
		long now = world.getServer().getTickCount();
		schedule(player, jenny, entry, now, jenny.getRandom());
		commit(player, jenny, entry, player.getUUID(), now);
		return true;
	}

	private static List<DialogueEntry> collectEligible(DialogueContext context, UUID playerId, long now) {
		Map<String, Long> playerCooldowns = COOLDOWNS.getOrDefault(playerId, Map.of());
		PlayerProgress progress = context.progress();

		List<DialogueEntry> best = new ArrayList<>();
		int bestPriority = Integer.MIN_VALUE;

		for (DialogueEntry entry : DialogueLoader.getInstance().entries()) {
			// La liste est déjà triée par priorité décroissante : dès qu'on
			// passe sous le meilleur score trouvé, plus rien ne peut gagner.
			if (!best.isEmpty() && entry.priority() < bestPriority) {
				break;
			}
			if (entry.once() && progress.hasSpoken(entry.id())) {
				continue;
			}
			if (now < playerCooldowns.getOrDefault(entry.id(), 0L)) {
				continue;
			}
			if (!entry.conditions().matches(context)) {
				continue;
			}

			if (entry.priority() > bestPriority) {
				bestPriority = entry.priority();
				best.clear();
			}
			best.add(entry);
		}
		return best;
	}

	/** Planifie les paliers de l'entrée dans le temps ; rend la première réplique. */
	private static Component schedule(ServerPlayer player, CompanionEntity jenny, DialogueEntry entry,
	                             long now, RandomSource random) {
		Component première = null;
		for (int delay : entry.delaySteps()) {
			List<DialogueLine> variants = entry.linesAt(delay);
			DialogueLine line = pickWeighted(variants, random);
			if (line == null || line.isEmpty()) {
				continue;
			}
			Component texte = remplir(line.toText(), player);
			if (première == null) {
				première = texte;
			}
			PENDING.add(new PendingLine(player.getUUID(), jenny.getId(), texte, now + delay));
		}
		return première;
	}

	/**
	 * {@code {player}} devient le nom du joueur, {@code {pet}} un petit nom selon
	 * son genre ({@code {boy}} : boy / girl). Seulement dans les répliques écrites
	 * en clair ({@code text}).
	 */
	private static Component remplir(Component texte, ServerPlayer player) {
		if (!(texte.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents)) {
			return texte;
		}
		String brut = texte.getString();
		if (brut.indexOf('{') < 0) {
			return texte;
		}
		boolean femme = dev.pyrofetch.jn1.gender.Gender.femme(player);
		String fait = brut.replace("{player}", player.getName().getString())
				.replace("{boy}", femme ? "girl" : "boy");
		return Component.literal(fait).setStyle(texte.getStyle());
	}

	/** Applique les effets de l'entrée : drapeaux, confiance, cooldown, once. */
	private static void commit(ServerPlayer player, CompanionEntity jenny, DialogueEntry entry,
	                           UUID playerId, long now) {
		PlayerProgress progress = ModAttachments.get(player);
		PlayerProgress updated = entry.conditions().applyEffects(progress, jenny.kind().id());
		if (entry.once()) {
			updated = updated.markSpoken(entry.id());
		}
		if (updated != progress) {
			ModAttachments.set(player, updated);
			syncProgress(player, updated);
		}

		if (entry.cooldown() > 0) {
			COOLDOWNS.computeIfAbsent(playerId, key -> new HashMap<>())
					.put(entry.id(), now + entry.cooldown());
		}
		GLOBAL_COOLDOWN.put(playerId, now + GLOBAL_AMBIENT_COOLDOWN);
	}

	private static DialogueLine pickWeighted(List<DialogueLine> lines, RandomSource random) {
		if (lines.isEmpty()) {
			return null;
		}
		if (lines.size() == 1) {
			return lines.getFirst();
		}
		int total = lines.stream().mapToInt(DialogueLine::weight).sum();
		int roll = random.nextInt(total);
		for (DialogueLine line : lines) {
			roll -= line.weight();
			if (roll < 0) {
				return line;
			}
		}
		return lines.getLast();
	}

	/** Appelé à chaque tick serveur : sort les répliques arrivées à échéance. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		long now = server.getTickCount();

		Iterator<PendingLine> iterator = PENDING.iterator();
		while (iterator.hasNext()) {
			PendingLine pending = iterator.next();
			if (now < pending.dueTick()) {
				continue;
			}
			iterator.remove();

			ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
			if (player == null) {
				continue;
			}
			deliver(player, pending);
		}
	}

	private static void deliver(ServerPlayer player, PendingLine pending) {
		int duration = displayTicks(pending.text());

		if (player.level() instanceof ServerLevel world) {
			if (world.getEntity(pending.entityId()) instanceof CompanionEntity jenny) {
				jenny.setTalkingFor(duration);
				// Une note brève, à peine audible : elle parle, on ne l'entend pas.
				world.playSound(null, jenny.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
						SoundSource.NEUTRAL, 0.25F, 1.7F);
			}
		}

		ServerPlayNetworking.send(player, new SpeakPayload(pending.entityId(), pending.text(), duration));
	}

	private static int displayTicks(Component text) {
		int length = text.getString().length();
		int base = Math.min(MAX_DISPLAY_TICKS, BASE_DISPLAY_TICKS + length * TICKS_PER_CHARACTER);
		return Math.round(base * JN1Config.get().dialogueDurationScale());
	}

	public static void syncProgress(ServerPlayer player, PlayerProgress progress) {
		ServerPlayNetworking.send(player,
				new ProgressSyncPayload(List.copyOf(progress.flags())));
	}

	/** Ajoute de la confiance et prévient le joueur si le palier change. */
	public static void grantTrust(ServerPlayer player, String companion, int delta) {
		PlayerProgress progress = ModAttachments.get(player);
		PlayerProgress updated = progress.addTrust(companion, delta);
		if (updated.trust(companion) == progress.trust(companion)) {
			return;
		}
		ModAttachments.set(player, updated);
		syncProgress(player, updated);
		if (updated.trust(companion) >= PlayerProgress.MAX_TRUST) {
			dev.pyrofetch.jn1.progress.Succes.accorder(player, "coeur");
		}
	}

	public static void setFlag(ServerPlayer player, String flag) {
		PlayerProgress progress = ModAttachments.get(player);
		PlayerProgress updated = progress.withFlag(flag);
		if (updated == progress) {
			return;
		}
		ModAttachments.set(player, updated);
		syncProgress(player, updated);
		JN1.LOGGER.debug("Drapeau « {} » posé pour {}", flag, player.getName().getString());
	}

	/** Vide l'état volatile — utilisé à l'arrêt du serveur. */
	public static void clear() {
		COOLDOWNS.clear();
		GLOBAL_COOLDOWN.clear();
		PENDING.clear();
	}

	private record PendingLine(UUID playerId, int entityId, Component text, long dueTick) {
	}
}
