package dev.pyrofetch.jn1.dialogue;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tout ce dont une condition a besoin pour se prononcer, plus un cache : un
 * cycle d'évaluation peut traverser des dizaines d'entrées qui interrogent le
 * même bloc alentour, et on ne veut scanner le volume qu'une fois.
 */
public final class DialogueContext {
	private final ServerLevel world;
	private final ServerPlayer player;
	private final CompanionEntity jenny;
	private final BlockPos pos;
	private final PlayerProgress progress;
	private final DialogueTrigger trigger;
	private java.util.Optional<String> talk = java.util.Optional.empty();
	private java.util.Optional<String> outcome = java.util.Optional.empty();
	private java.util.Optional<String> event = java.util.Optional.empty();

	private final Map<String, Boolean> nearbyCache = new HashMap<>();
	private final Map<String, Boolean> structureCache = new HashMap<>();

	public DialogueContext(ServerLevel world, ServerPlayer player, CompanionEntity jenny,
	                       BlockPos pos, PlayerProgress progress, DialogueTrigger trigger) {
		this.world = world;
		this.player = player;
		this.jenny = jenny;
		this.pos = pos;
		this.progress = progress;
		this.trigger = trigger;
	}

	public ServerLevel world() {
		return this.world;
	}

	public ServerPlayer player() {
		return this.player;
	}

	public CompanionEntity jenny() {
		return this.jenny;
	}

	public BlockPos pos() {
		return this.pos;
	}

	public PlayerProgress progress() {
		return this.progress;
	}

	public DialogueTrigger trigger() {
		return this.trigger;
	}

	/** L'interaction de « Talk », son issue, ou l'événement : ce qui l'a fait parler. */
	public DialogueContext with(@org.jetbrains.annotations.Nullable String talk,
	                            @org.jetbrains.annotations.Nullable String outcome,
	                            @org.jetbrains.annotations.Nullable String event) {
		this.talk = java.util.Optional.ofNullable(talk);
		this.outcome = java.util.Optional.ofNullable(outcome);
		this.event = java.util.Optional.ofNullable(event);
		return this;
	}

	public java.util.Optional<String> talk() {
		return this.talk;
	}

	public java.util.Optional<String> outcome() {
		return this.outcome;
	}

	public java.util.Optional<String> event() {
		return this.event;
	}

	public Map<String, Boolean> nearbyCache() {
		return this.nearbyCache;
	}

	public Map<String, Boolean> structureCache() {
		return this.structureCache;
	}
}
