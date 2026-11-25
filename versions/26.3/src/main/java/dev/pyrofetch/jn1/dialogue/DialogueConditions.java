package dev.pyrofetch.jn1.dialogue;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Le filtre d'une entrée de dialogue. Tous les champs sont facultatifs : un
 * champ absent — ou explicitement {@code null}, comme dans la spec — ne
 * contraint rien.
 *
 * <p>{@code in_structure}, {@code biome} et {@code nearby_block} acceptent
 * aussi bien un identifiant (« minecraft:ancient_city ») qu'un tag préfixé de
 * dièse (« #minecraft:mineshaft »).
 */
public record DialogueConditions(
		int trustMin,
		int trustMax,
		Optional<String> inStructure,
		Optional<String> biome,
		Optional<String> nearbyBlock,
		int nearbyRadius,
		Optional<Integer> yBelow,
		Optional<Integer> yAbove,
		Optional<Integer> lightMax,
		Optional<Integer> lightMin,
		Optional<TimeOfDay> time,
		Optional<Identifier> dimension,
		DialogueTrigger trigger,
		List<String> requiresFlags,
		List<String> forbidsFlags,
		List<String> setsFlags,
		int trustDelta,
		Her her
) {
	/**
	 * Accepte indifféremment une chaîne unique ou une liste de chaînes. Déclaré
	 * avant {@link #ALWAYS}, qui initialise {@link Her} : sinon nul à sa lecture.
	 */
	private static final Codec<List<String>> FLEXIBLE_STRING_LIST = Codec.either(
			Codec.STRING, Codec.STRING.listOf()
	).xmap(
			either -> either.map(List::of, list -> list),
			list -> list.size() == 1
					? com.mojang.datafixers.util.Either.left(list.getFirst())
					: com.mojang.datafixers.util.Either.right(list)
	);

	public static final DialogueConditions ALWAYS = new DialogueConditions(
			PlayerProgress.MIN_TRUST, PlayerProgress.MAX_TRUST,
			Optional.empty(), Optional.empty(), Optional.empty(), 6,
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), DialogueTrigger.ANY,
			List.of(), List.of(), List.of(), 0, Her.ANY);

	/**
	 * Ce qui tient à elle et à l'instant (0.56.0, les personnalités) : qui elle
	 * est, ce qu'on vient de lui dire ou de lui faire, son humeur, sa tenue, ses
	 * traces, les menottes, et qui lui parle.
	 *
	 * @param girls   les compagnes à qui l'entrée va ({@code jenny}, {@code ellie},
	 *                {@code aura}) ; vide = toutes
	 * @param talk    l'interaction de « Talk » ({@code chat}, {@code flirt}…)
	 * @param outcome {@code success} / {@code fail} pour une interaction,
	 *                {@code liked} / {@code disliked} pour un cadeau
	 * @param event   l'événement qui l'a fait parler ({@link DialogueTrigger#EVENT})
	 * @param outfit  {@code dressed} (sa première tenue), {@code undressed} (une
	 *                autre), {@code naked} (sa dernière)
	 * @param gender  {@code homme} / {@code femme} : celui qui lui parle
	 */
	public record Her(List<String> girls, Optional<String> talk, Optional<String> outcome, Optional<String> event,
	                  int moodMin, int moodMax, Optional<String> outfit, int tracesMin, int tracesMax,
	                  Optional<Boolean> cuffed, Optional<String> gender, Optional<Boolean> following) {
		public static final Her ANY = new Her(List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
				-100, 100, Optional.empty(), 0, 100, Optional.empty(), Optional.empty(), Optional.empty());

		static final MapCodec<Her> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				FLEXIBLE_STRING_LIST.optionalFieldOf("girl", List.of()).forGetter(Her::girls),
				Codec.STRING.optionalFieldOf("talk").forGetter(Her::talk),
				Codec.STRING.optionalFieldOf("outcome").forGetter(Her::outcome),
				Codec.STRING.optionalFieldOf("event").forGetter(Her::event),
				Codec.INT.optionalFieldOf("mood_min", -100).forGetter(Her::moodMin),
				Codec.INT.optionalFieldOf("mood_max", 100).forGetter(Her::moodMax),
				Codec.STRING.optionalFieldOf("outfit").forGetter(Her::outfit),
				Codec.INT.optionalFieldOf("traces_min", 0).forGetter(Her::tracesMin),
				Codec.INT.optionalFieldOf("traces_max", 100).forGetter(Her::tracesMax),
				Codec.BOOL.optionalFieldOf("cuffed").forGetter(Her::cuffed),
				Codec.STRING.optionalFieldOf("gender").forGetter(Her::gender),
				Codec.BOOL.optionalFieldOf("following").forGetter(Her::following)
		).apply(instance, Her::new));

		boolean matches(DialogueContext ctx) {
			dev.pyrofetch.jn1.entity.CompanionEntity fille = ctx.jenny();
			if (!this.girls.isEmpty() && !this.girls.contains(fille.kind().id())) {
				return false;
			}
			// Une entrée d'interaction ou d'événement ne sort que pour celui-là ;
			// et une entrée qui n'en nomme pas ne sort pas pour eux.
			if (!this.talk.equals(ctx.talk()) || !this.event.equals(ctx.event())) {
				return false;
			}
			if (this.outcome.isPresent() && !this.outcome.equals(ctx.outcome())) {
				return false;
			}
			int humeur = fille.getMood();
			if (humeur < this.moodMin || humeur > this.moodMax) {
				return false;
			}
			if (this.outfit.isPresent()) {
				List<String> tenues = fille.kind().outfits();
				int rang = Math.max(0, tenues.indexOf(fille.getOutfit()));
				boolean ok = switch (this.outfit.get()) {
					case "dressed" -> rang == 0;
					case "undressed" -> rang > 0;
					case "naked" -> tenues.size() > 1 && rang == tenues.size() - 1;
					default -> false;
				};
				if (!ok) {
					return false;
				}
			}
			int traces = fille.getTraces();
			if (traces < this.tracesMin || traces > this.tracesMax) {
				return false;
			}
			if (this.cuffed.isPresent() && this.cuffed.get() != fille.isCuffed()) {
				return false;
			}
			if (this.following.isPresent()
					&& this.following.get() != (fille.getMode() == dev.pyrofetch.jn1.entity.JennyMode.FOLLOW)) {
				return false;
			}
			return this.gender.isEmpty()
					|| this.gender.get().equals(dev.pyrofetch.jn1.gender.Gender.femme(ctx.player())
					? dev.pyrofetch.jn1.gender.Gender.FEMME : dev.pyrofetch.jn1.gender.Gender.HOMME);
		}
	}

	// RecordCodecBuilder plafonne à seize champs. On décrit donc les conditions
	// en deux moitiés recollées par mapPair : le JSON reste parfaitement plat,
	// et il reste de la place pour de nouveaux critères.
	private record Where(int trustMin, int trustMax, Optional<String> inStructure, Optional<String> biome,
	                     Optional<String> nearbyBlock, int nearbyRadius,
	                     Optional<Integer> yBelow, Optional<Integer> yAbove) {
	}

	private record When(Optional<Integer> lightMax, Optional<Integer> lightMin, Optional<TimeOfDay> time,
	                    Optional<Identifier> dimension, DialogueTrigger trigger,
	                    List<String> requiresFlags, List<String> forbidsFlags,
	                    List<String> setsFlags, int trustDelta) {
	}

	private static final MapCodec<Where> WHERE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.optionalFieldOf("trust_min", PlayerProgress.MIN_TRUST).forGetter(Where::trustMin),
			Codec.INT.optionalFieldOf("trust_max", PlayerProgress.MAX_TRUST).forGetter(Where::trustMax),
			Codec.STRING.optionalFieldOf("in_structure").forGetter(Where::inStructure),
			Codec.STRING.optionalFieldOf("biome").forGetter(Where::biome),
			Codec.STRING.optionalFieldOf("nearby_block").forGetter(Where::nearbyBlock),
			Codec.INT.optionalFieldOf("nearby_radius", 6).forGetter(Where::nearbyRadius),
			Codec.INT.optionalFieldOf("y_below").forGetter(Where::yBelow),
			Codec.INT.optionalFieldOf("y_above").forGetter(Where::yAbove)
	).apply(instance, Where::new));

	private static final MapCodec<When> WHEN_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.optionalFieldOf("light_max").forGetter(When::lightMax),
			Codec.INT.optionalFieldOf("light_min").forGetter(When::lightMin),
			TimeOfDay.CODEC.optionalFieldOf("time").forGetter(When::time),
			Identifier.CODEC.optionalFieldOf("dimension").forGetter(When::dimension),
			DialogueTrigger.CODEC.optionalFieldOf("trigger", DialogueTrigger.ANY).forGetter(When::trigger),
			FLEXIBLE_STRING_LIST.optionalFieldOf("requires_flag", List.of()).forGetter(When::requiresFlags),
			FLEXIBLE_STRING_LIST.optionalFieldOf("forbids_flag", List.of()).forGetter(When::forbidsFlags),
			FLEXIBLE_STRING_LIST.optionalFieldOf("sets_flag", List.of()).forGetter(When::setsFlags),
			Codec.INT.optionalFieldOf("trust_delta", 0).forGetter(When::trustDelta)
	).apply(instance, When::new));

	public static final Codec<DialogueConditions> CODEC = Codec.mapPair(Codec.mapPair(WHERE_CODEC, WHEN_CODEC), Her.CODEC)
			.codec()
			.xmap(
					triple -> {
						Where where = triple.getFirst().getFirst();
						When when = triple.getFirst().getSecond();
						return new DialogueConditions(
								where.trustMin(), where.trustMax(), where.inStructure(), where.biome(),
								where.nearbyBlock(), where.nearbyRadius(), where.yBelow(), where.yAbove(),
								when.lightMax(), when.lightMin(), when.time(), when.dimension(),
								when.trigger(), when.requiresFlags(), when.forbidsFlags(),
								when.setsFlags(), when.trustDelta(), triple.getSecond());
					},
					conditions -> Pair.of(Pair.of(
							new Where(conditions.trustMin(), conditions.trustMax(), conditions.inStructure(),
									conditions.biome(), conditions.nearbyBlock(), conditions.nearbyRadius(),
									conditions.yBelow(), conditions.yAbove()),
							new When(conditions.lightMax(), conditions.lightMin(), conditions.time(),
									conditions.dimension(), conditions.trigger(), conditions.requiresFlags(),
									conditions.forbidsFlags(), conditions.setsFlags(), conditions.trustDelta())),
							conditions.her())
			);

	public boolean matches(DialogueContext ctx) {
		PlayerProgress progress = ctx.progress();

		int trust = progress.trust(ctx.jenny().kind().id());
		if (trust < this.trustMin || trust > this.trustMax) {
			return false;
		}
		if (!this.trigger.accepts(ctx.trigger())) {
			return false;
		}
		if (!this.her.matches(ctx)) {
			return false;
		}
		for (String flag : this.requiresFlags) {
			if (!progress.hasFlag(flag)) {
				return false;
			}
		}
		for (String flag : this.forbidsFlags) {
			if (progress.hasFlag(flag)) {
				return false;
			}
		}

		BlockPos pos = ctx.pos();
		if (this.yBelow.isPresent() && pos.getY() >= this.yBelow.get()) {
			return false;
		}
		if (this.yAbove.isPresent() && pos.getY() <= this.yAbove.get()) {
			return false;
		}
		if (this.lightMax.isPresent() && ctx.world().getMaxLocalRawBrightness(pos) > this.lightMax.get()) {
			return false;
		}
		if (this.lightMin.isPresent() && ctx.world().getMaxLocalRawBrightness(pos) < this.lightMin.get()) {
			return false;
		}
		if (this.dimension.isPresent()
				&& !ctx.world().dimension().identifier().equals(this.dimension.get())) {
			return false;
		}
		if (this.time.isPresent() && !this.time.get().matches(ctx.world())) {
			return false;
		}
		if (this.biome.isPresent() && !this.matchesBiome(ctx)) {
			return false;
		}
		if (this.inStructure.isPresent() && !this.matchesStructure(ctx)) {
			return false;
		}
		return this.nearbyBlock.isEmpty() || this.matchesNearbyBlock(ctx);
	}

	private boolean matchesBiome(DialogueContext ctx) {
		String spec = this.biome.orElseThrow();
		Holder<net.minecraft.world.level.biome.Biome> entry = ctx.world().getBiome(ctx.pos());
		if (spec.startsWith("#")) {
			Identifier tagId = Identifier.tryParse(spec.substring(1));
			return tagId != null && entry.is(TagKey.create(Registries.BIOME, tagId));
		}
		Identifier id = Identifier.tryParse(spec);
		return id != null && entry.is(id);
	}

	private boolean matchesStructure(DialogueContext ctx) {
		String spec = this.inStructure.orElseThrow();
		return ctx.structureCache().computeIfAbsent(spec, key -> {
			StructureStart start;
			if (key.startsWith("#")) {
				Identifier tagId = Identifier.tryParse(key.substring(1));
				if (tagId == null) {
					return false;
				}
				start = ctx.world().structureManager()
						.getStructureWithPieceAt(ctx.pos(), TagKey.create(Registries.STRUCTURE, tagId));
			} else {
				Identifier id = Identifier.tryParse(key);
				if (id == null) {
					return false;
				}
				ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, id);
				start = ctx.world().structureManager()
						.getStructureWithPieceAt(ctx.pos(), entry -> entry.is(structureKey));
			}
			return start != null && start.isValid();
		});
	}

	private boolean matchesNearbyBlock(DialogueContext ctx) {
		String spec = this.nearbyBlock.orElseThrow();
		String cacheKey = spec + "@" + this.nearbyRadius;
		return ctx.nearbyCache().computeIfAbsent(cacheKey, key -> {
			int radius = Math.clamp(this.nearbyRadius, 1, 16);
			BlockPos center = ctx.pos();

			if (spec.startsWith("#")) {
				Identifier tagId = Identifier.tryParse(spec.substring(1));
				if (tagId == null) {
					return false;
				}
				TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
				return scan(ctx, center, radius, state -> state.is(tag));
			}

			Identifier id = Identifier.tryParse(spec);
			if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
				return false;
			}
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			return scan(ctx, center, radius, state -> state.is(block));
		});
	}

	private static boolean scan(DialogueContext ctx, BlockPos center, int radius,
	                            java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> predicate) {
		for (BlockPos pos : BlockPos.withinBoxByManhattanDistance(center, radius, radius, radius)) {
			if (predicate.test(ctx.world().getBlockState(pos))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Applique les effets déclarés (drapeaux, confiance).
	 *
	 * @param companion à qui la confiance gagnée revient
	 */
	public PlayerProgress applyEffects(PlayerProgress progress, String companion) {
		PlayerProgress updated = progress;
		for (String flag : this.setsFlags) {
			updated = updated.withFlag(flag);
		}
		if (this.trustDelta != 0) {
			updated = updated.addTrust(companion, this.trustDelta);
		}
		return updated;
	}

	public enum TimeOfDay implements net.minecraft.util.StringRepresentable {
		DAY("day"),
		NIGHT("night");

		public static final Codec<TimeOfDay> CODEC =
				net.minecraft.util.StringRepresentable.fromEnum(TimeOfDay::values);

		private final String name;

		TimeOfDay(String name) {
			this.name = name;
		}

		public boolean matches(net.minecraft.world.level.Level world) {
			return this == DAY ? world.isBrightOutside() : world.isDarkOutside();
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
