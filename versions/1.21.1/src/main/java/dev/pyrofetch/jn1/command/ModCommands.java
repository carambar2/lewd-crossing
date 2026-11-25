package dev.pyrofetch.jn1.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.pyrofetch.jn1.dialogue.DialogueEngine;
import dev.pyrofetch.jn1.dialogue.DialogueLoader;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.entity.CompanionScenes;
import dev.pyrofetch.jn1.entity.JennyMode;
import dev.pyrofetch.jn1.progress.PlayerProgress;
import dev.pyrofetch.jn1.registry.ModAttachments;
import dev.pyrofetch.jn1.block.PhotoFrameBlockEntity;
import dev.pyrofetch.jn1.photo.PhotoArchive;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.photo.PhotoImage;
import dev.pyrofetch.jn1.photo.PhotoItem;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Outils de test. Rien ici n'est nécessaire pour jouer : c'est ce qui permet de
 * sauter à un point précis de l'arc sans le rejouer en entier.
 */
public final class ModCommands {
	private static final int PERMISSION_LEVEL = 2;

	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("jn1")
						.requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL))

						// Franchit la porte sans avoir à la chercher : sert à
						// vérifier le passage, et à ressortir si l'on s'est
						// retrouvé coincé.
						.then(CommandManager.literal("lab")
								.executes(ctx -> {
									dev.pyrofetch.jn1.lab.LabDimension.cross(
											ctx.getSource().getPlayerOrThrow(),
											ctx.getSource().getPlayerOrThrow().getBlockPos(),
											net.minecraft.util.math.Direction.NORTH);
									return 1;
								}))

						// Les laboratoires du vide autour d'ici, tels que le lidar les
						// voit ; bâtit le plus proche et cherche sa porte de cave. C'est
						// ainsi qu'on éprouve le parallèle sur le serveur de test.
						// Appuie sur un bouton d'appel sans joueur : c'est ainsi qu'on
						// éprouve la cabine et ses portes au serveur de test.
						.then(CommandManager.literal("lift")
								.then(CommandManager.literal("call")
										.then(CommandManager.argument("palier", BlockPosArgumentType.blockPos())
												.executes(ModCommands::appelerAscenseur)))
								.then(CommandManager.literal("assemble")
										.then(CommandManager.argument("palier", BlockPosArgumentType.blockPos())
												.executes(ctx -> {
													BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "palier");
													dev.pyrofetch.jn1.lift.Platforms.assembler(ctx.getSource().getWorld(), pos);
													int n = dev.pyrofetch.jn1.lift.Platforms.groupe(ctx.getSource().getWorld(), pos).size();
													ctx.getSource().sendFeedback(() -> Text.literal("plateforme assemblée : " + n + " blocs"), false);
													return n;
												}))))

						.then(CommandManager.literal("void")
								.then(CommandManager.literal("labs")
										.executes(ModCommands::laboratoires))
								.then(CommandManager.literal("gaster")
										.executes(ModCommands::gaster)))

						.then(CommandManager.literal("trust")
								.then(CommandManager.literal("get")
										.then(CommandManager.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.executes(ctx -> trustGet(ctx, ctx.getSource().getPlayerOrThrow(),
														StringArgumentType.getString(ctx, "companion")))
												.then(CommandManager.argument("player", EntityArgumentType.player())
														.executes(ctx -> trustGet(ctx,
																EntityArgumentType.getPlayer(ctx, "player"),
																StringArgumentType.getString(ctx, "companion"))))))
								.then(CommandManager.literal("set")
										.then(CommandManager.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.then(CommandManager.argument("value", IntegerArgumentType.integer(
																PlayerProgress.MIN_TRUST, PlayerProgress.MAX_TRUST))
														.executes(ctx -> trustSet(ctx,
																ctx.getSource().getPlayerOrThrow(),
																StringArgumentType.getString(ctx, "companion"),
																IntegerArgumentType.getInteger(ctx, "value")))
														.then(CommandManager.argument("player",
																		EntityArgumentType.player())
																.executes(ctx -> trustSet(ctx,
																		EntityArgumentType.getPlayer(ctx, "player"),
																		StringArgumentType.getString(ctx, "companion"),
																		IntegerArgumentType.getInteger(ctx, "value")))))))
								.then(CommandManager.literal("add")
										.then(CommandManager.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.then(CommandManager.argument("value",
																IntegerArgumentType.integer(-10, 10))
														.executes(ctx -> {
															ServerPlayerEntity player =
																	ctx.getSource().getPlayerOrThrow();
															String nom = StringArgumentType.getString(
																	ctx, "companion");
															DialogueEngine.grantTrust(player, nom,
																	IntegerArgumentType.getInteger(ctx, "value"));
															return trustGet(ctx, player, nom);
														})))))

						.then(CommandManager.literal("flag")
								.then(CommandManager.literal("add")
										.then(CommandManager.argument("flag", StringArgumentType.word())
												.executes(ctx -> flagAdd(ctx,
														StringArgumentType.getString(ctx, "flag")))))
								.then(CommandManager.literal("remove")
										.then(CommandManager.argument("flag", StringArgumentType.word())
												.executes(ctx -> flagRemove(ctx,
														StringArgumentType.getString(ctx, "flag")))))
								.then(CommandManager.literal("list")
										.executes(ModCommands::flagList)))

						.then(CommandManager.literal("say")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											DialogueLoader.getInstance().entries()
													.forEach(entry -> builder.suggest(entry.id()));
											return builder.buildFuture();
										})
										.executes(ctx -> say(ctx, StringArgumentType.getString(ctx, "id")))))

						.then(CommandManager.literal("mode")
								.then(CommandManager.argument("mode", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (JennyMode mode : JennyMode.values()) {
												builder.suggest(mode.name().toLowerCase(java.util.Locale.ROOT));
											}
											return builder.buildFuture();
										})
										.executes(ctx -> setMode(ctx, StringArgumentType.getString(ctx, "mode")))))

						.then(CommandManager.literal("unsettled")
								.executes(ModCommands::toggleUnsettled))

						// La transformation, sans potion ni écran : « none » la défait.
						.then(CommandManager.literal("morph")
								.then(CommandManager.argument("joueur", EntityArgumentType.player())
										.then(CommandManager.argument("compagne", StringArgumentType.word())
												.suggests((ctx, builder) -> {
													builder.suggest("none");
													CompanionKind.all().forEach(kind -> builder.suggest(kind.id()));
													return builder.buildFuture();
												})
												.executes(ctx -> morph(ctx, ""))
												.then(CommandManager.argument("tenue", StringArgumentType.word())
														.suggests(TENUES)
														.executes(ctx -> morph(ctx,
																StringArgumentType.getString(ctx, "tenue")))))))

						// La tenue d'une compagne, sans passer par son menu.
						.then(CommandManager.literal("outfit")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.then(CommandManager.argument("tenue", StringArgumentType.word())
												.suggests(TENUES)
												.executes(ModCommands::tenue))))

						// Ce que fait la télécommande, sans joueur ni écran : c'est
						// ainsi qu'on éprouve les scènes au serveur de test.
						.then(CommandManager.literal("scene")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.then(CommandManager.literal("stop")
												.executes(ctx -> scène(ctx, "")))
										.then(CommandManager.literal("prop")
												.then(CommandManager.argument("visible",
																com.mojang.brigadier.arguments.BoolArgumentType.bool())
														.executes(ModCommands::pièceDeScène)))
										.then(CommandManager.argument("nom", StringArgumentType.word())
												.suggests(SCENES)
												.executes(ctx -> scène(ctx,
														StringArgumentType.getString(ctx, "nom"))))))

						// Une photo d'essai : un dégradé en diagonale, rangé dans
						// l'archive comme une vraie. Remise en main, ou accrochée
						// directement au cadre désigné.
						.then(CommandManager.literal("photo")
								.then(CommandManager.argument("format", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (PhotoFormat format : PhotoFormat.values()) {
												builder.suggest(format.asString());
											}
											return builder.buildFuture();
										})
										.executes(ctx -> photo(ctx, StringArgumentType.getString(ctx, "format"), null))
										.then(CommandManager.argument("cadre", BlockPosArgumentType.blockPos())
												.executes(ctx -> photo(ctx, StringArgumentType.getString(ctx, "format"),
														BlockPosArgumentType.getLoadedBlockPos(ctx, "cadre"))))))

						// Les formes d'une compagne ou d'un joueur, comme une potion
						// les fixerait : l'épreuve au serveur de test.
						.then(CommandManager.literal("shape")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.then(CommandManager.argument("forme", StringArgumentType.word())
												.suggests((ctx, builder) -> {
													for (var part : dev.pyrofetch.jn1.shape.Shapes.Part.values()) {
														builder.suggest(part.id());
													}
													return builder.buildFuture();
												})
												.then(CommandManager.argument("niveau", IntegerArgumentType.integer(-9, 9))
														.executes(ModCommands::forme)))))

						// Pose une fille derrière une cloison à trou, sans joueur :
						// côté de la face du panneau.
						.then(CommandManager.literal("wall")
								.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
										.then(CommandManager.argument("cible", EntityArgumentType.entity())
												.executes(ModCommands::cloison))))

						// La fin d'une boucle de scène « cu », sans attendre la scène.
						// Homme ou femme, sans écran ni potion.
						.then(CommandManager.literal("gender")
								.then(CommandManager.argument("joueur", EntityArgumentType.player())
										.then(CommandManager.argument("genre", StringArgumentType.word())
												.suggests((ctx, builder) -> builder.suggest("homme").suggest("femme").buildFuture())
												.executes(ctx -> {
													String genre = StringArgumentType.getString(ctx, "genre");
													if (!genre.equals(dev.pyrofetch.jn1.gender.Gender.HOMME)
															&& !genre.equals(dev.pyrofetch.jn1.gender.Gender.FEMME)) {
														ctx.getSource().sendError(Text.literal("REFUS : homme ou femme"));
														return 0;
													}
													ServerPlayerEntity joueur = EntityArgumentType.getPlayer(ctx, "joueur");
													dev.pyrofetch.jn1.gender.Gender.fixer(joueur, genre);
													ctx.getSource().sendFeedback(() -> Text.literal(
															joueur.getName().getString() + " : " + genre), false);
													return 1;
												}))))
						// Menotter ou libérer, sans joueur.
						.then(CommandManager.literal("cuff")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.then(CommandManager.argument("menottée",
														com.mojang.brigadier.arguments.BoolArgumentType.bool())
												.executes(ctx -> {
													if (!(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
														ctx.getSource().sendError(Text.literal("REFUS : pas une fille"));
														return 0;
													}
													fille.setCuffed(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "menottée"));
													ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString()
															+ " menottée : " + fille.isCuffed()), false);
													return 1;
												}))))
						// Faire danser une fille à une barre, sans joueur.
						.then(CommandManager.literal("pole")
								.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
										.then(CommandManager.argument("cible", EntityArgumentType.entity())
												.executes(ctx -> {
													if (!(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
														ctx.getSource().sendError(Text.literal("REFUS : pas une fille"));
														return 0;
													}
													net.minecraft.util.math.BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
													dev.pyrofetch.jn1.jouets.DancePoleBlock.danser(fille, pos,
															net.minecraft.util.math.Vec3d.ofBottomCenter(pos).add(0, 0, 3));
													ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString()
															+ " danse : " + fille.getScene() + String.format(java.util.Locale.ROOT,
															" à %.2f %.2f %.2f, lacet %.0f", fille.getX(), fille.getY(), fille.getZ(),
															fille.getYaw())), false);
													return 1;
												}))))
						// Le vrai chemin du réveil, au serveur de test : un joueur factice,
						// compagnon de la fille, s'endort et se réveille à deux blocs d'elle.
						.then(CommandManager.literal("sleeptest")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.executes(ctx -> {
											if (!(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendError(Text.literal("REFUS : pas une fille"));
												return 0;
											}
											net.minecraft.server.world.ServerWorld monde = (net.minecraft.server.world.ServerWorld) fille.getWorld();
											ServerPlayerEntity dormeur = net.fabricmc.fabric.api.entity.FakePlayer.get(monde);
											dormeur.refreshPositionAndAngles(fille.getX() + 2, fille.getY(), fille.getZ(), 90, 0);
											fille.setCompanion(dormeur);
											// Un lit, tête à l'est, à trois blocs d'elle.
											net.minecraft.util.math.BlockPos pied = fille.getBlockPos().east(3);
											net.minecraft.util.math.BlockPos tête = pied.east();
											monde.setBlockState(pied, net.minecraft.block.Blocks.RED_BED.getDefaultState()
													.with(net.minecraft.block.BedBlock.FACING, net.minecraft.util.math.Direction.EAST)
													.with(net.minecraft.block.BedBlock.PART, net.minecraft.block.enums.BedPart.FOOT));
											monde.setBlockState(tête, net.minecraft.block.Blocks.RED_BED.getDefaultState()
													.with(net.minecraft.block.BedBlock.FACING, net.minecraft.util.math.Direction.EAST)
													.with(net.minecraft.block.BedBlock.PART, net.minecraft.block.enums.BedPart.HEAD));
											// Le joueur factice de Fabric refuse de dormir : on lance
											// l'événement de réveil comme le jeu le fait, avec ce lit.
											net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.STOP_SLEEPING.invoker()
													.onStopSleeping(dormeur, tête);
											ctx.getSource().sendFeedback(() -> Text.literal("dormeur réveillé, lit : pied "
													+ pied.toShortString() + ", tête " + tête.toShortString()), false);
											return 1;
										})))
						// Le réveil, sans dormir : une fille vient le branler.
						// /jn1 demande <fille> <demande> [brutale] : une demande du menu,
						// sans menu ni seuil d'affection, au joueur qui tape la commande.
						.then(CommandManager.literal("demande")
								.then(CommandManager.argument("fille", EntityArgumentType.entity())
										.then(CommandManager.argument("demande", StringArgumentType.word())
												.suggests((ctx, b) -> net.minecraft.command.CommandSource.suggestMatching(
														dev.pyrofetch.jn1.entity.Demande.TOUTES.stream()
																.map(dev.pyrofetch.jn1.entity.Demande::id), b))
												.executes(ctx -> demande(ctx, false))
												.then(CommandManager.literal("brutale")
														.executes(ctx -> demande(ctx, true))))))
						.then(CommandManager.literal("wakeup")
								.then(CommandManager.argument("joueur", EntityArgumentType.player())
										.executes(ctx -> {
											ServerPlayerEntity joueur = EntityArgumentType.getPlayer(ctx, "joueur");
											CompanionEntity fille = dev.pyrofetch.jn1.excitation.Reveil.réveiller(joueur, true);
											ctx.getSource().sendFeedback(() -> Text.literal(fille == null ? "personne"
													: fille.getName().getString() + " : " + fille.getScene()), false);
											return fille == null ? 0 : 1;
										})))
						// La barre d'excitation pleine, sans joueur : la scène passe à sa fin.
						.then(CommandManager.literal("finish")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.executes(ctx -> {
											if (!(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendError(Text.literal("REFUS : pas une fille"));
												return 0;
											}
											String avant = fille.getScene();
											String fin = fille.finishScene();
											ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString()
													+ " : " + avant + " → " + (fin.isEmpty() ? "arrêtée" : fin)), false);
											return 1;
										})))
						.then(CommandManager.literal("cum")
								.then(CommandManager.argument("cible", EntityArgumentType.entity())
										.executes(ctx -> {
											if (!(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendError(Text.literal("REFUS : pas une fille"));
												return 0;
											}
											dev.pyrofetch.jn1.cum.Cum.jaillir(fille);
											ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString()
													+ " : traces " + fille.getTraces()), false);
											return 1;
										})))

						// Ce que le serveur voit en vitrine d'un distributeur : c'est
						// l'épreuve des rayons garnis, sans joueur ni écran.
						.then(CommandManager.literal("vending")
								.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
										.executes(ctx -> vitrine(ctx,
												BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")))))

						.then(CommandManager.literal("dialogue")
								.then(CommandManager.literal("count")
										.executes(ctx -> {
											int count = DialogueLoader.getInstance().entries().size();
											ctx.getSource().sendFeedback(
													() -> Text.translatable("command.jn1.dialogue.count", count),
													false);
											return count;
										})))));
	}

	/** Les compagnes connues, proposées à la saisie. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<ServerCommandSource>
			COMPANIONS = (ctx, builder) -> {
		for (dev.pyrofetch.jn1.shop.GirlEntry entry
				: dev.pyrofetch.jn1.shop.GirlEntry.catalogue()) {
			builder.suggest(entry.id());
		}
		return builder.buildFuture();
	};

	/** Les scènes de toutes les compagnes qui en ont. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<ServerCommandSource>
			SCENES = (ctx, builder) -> {
		for (CompanionKind kind : CompanionKind.all()) {
			CompanionScenes.of(kind).forEach(builder::suggest);
		}
		return builder.buildFuture();
	};

	private static int demande(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx, boolean brutale)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity joueur = ctx.getSource().getPlayerOrThrow();
		if (!(EntityArgumentType.getEntity(ctx, "fille") instanceof CompanionEntity fille)) {
			ctx.getSource().sendError(Text.literal("pas une compagne"));
			return 0;
		}
		dev.pyrofetch.jn1.entity.Demande demande = dev.pyrofetch.jn1.entity.Demande.byId(
				StringArgumentType.getString(ctx, "demande"));
		if (demande == null || !demande.possible(fille.kind())) {
			ctx.getSource().sendError(Text.literal("demande inconnue, ou qu'elle ne sait pas jouer"));
			return 0;
		}
		fille.commencerDemande(joueur, demande, brutale);
		ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString() + " : " + demande.id()
				+ (brutale ? " (brutale)" : "") + ", " + fille.getScene()), false);
		return 1;
	}

	private static int scène(CommandContext<ServerCommandSource> ctx, String nom)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.entity.Entity cible = EntityArgumentType.getEntity(ctx, "cible");
		if (!(cible instanceof CompanionEntity jenny)) {
			ctx.getSource().sendError(Text.literal("REFUS : " + cible.getName().getString()
					+ " n'est pas une compagne"));
			return 0;
		}
		String qui = jenny.getName().getString();
		if (nom.isEmpty()) {
			jenny.stopScene();
			ctx.getSource().sendFeedback(() -> Text.literal(qui + " : scène arrêtée"), false);
			return 1;
		}
		List<String> scènes = CompanionScenes.of(jenny.kind());
		if (!scènes.contains(nom)) {
			ctx.getSource().sendError(Text.literal("REFUS : « " + nom + " » inconnue de " + qui
					+ " ; connues : " + (scènes.isEmpty() ? "aucune" : String.join(", ", scènes))));
			return 0;
		}
		jenny.playScene(nom);
		net.minecraft.util.math.Box boîte = jenny.getBoundingBox();
		ctx.getSource().sendFeedback(() -> Text.literal(qui + " joue " + jenny.getScene()
				+ " (" + jenny.kind().sceneAnimation(jenny.getScene()) + ") — corps à "
				+ Math.round(jenny.bodyYaw) + "°, boîte x " + String.format(java.util.Locale.ROOT, "%.2f…%.2f", boîte.minX, boîte.maxX)
				+ " z " + String.format(java.util.Locale.ROOT, "%.2f…%.2f", boîte.minZ, boîte.maxZ)), false);
		return 1;
	}

	/** Toutes les tenues de toutes les compagnes. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<ServerCommandSource>
			TENUES = (ctx, builder) -> {
		CompanionKind.all().forEach(kind -> kind.outfits().forEach(builder::suggest));
		return builder.buildFuture();
	};

	private static int morph(CommandContext<ServerCommandSource> ctx, String tenue)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity joueur = EntityArgumentType.getPlayer(ctx, "joueur");
		String nom = StringArgumentType.getString(ctx, "compagne");
		if (nom.equals("none")) {
			dev.pyrofetch.jn1.morph.MorphHandler.set(joueur, dev.pyrofetch.jn1.morph.MorphState.NONE);
			ctx.getSource().sendFeedback(() -> Text.literal(joueur.getName().getString() + " redevient lui-même"), false);
			return 1;
		}
		CompanionKind kind = CompanionKind.byId(nom);
		if (kind == null) {
			ctx.getSource().sendError(Text.literal("REFUS : compagne inconnue « " + nom + " »"));
			return 0;
		}
		String portée = kind.outfit(tenue);
		dev.pyrofetch.jn1.morph.MorphHandler.set(joueur,
				dev.pyrofetch.jn1.registry.ModAttachments.morph(joueur).with(kind.id(), portée, false));
		ctx.getSource().sendFeedback(() -> Text.literal(joueur.getName().getString() + " prend l'apparence de "
				+ kind.id() + " en tenue " + portée), false);
		return 1;
	}

	private static int tenue(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.entity.Entity cible = EntityArgumentType.getEntity(ctx, "cible");
		String tenue = StringArgumentType.getString(ctx, "tenue");
		if (!(cible instanceof CompanionEntity jenny) || !jenny.kind().outfits().contains(tenue)) {
			ctx.getSource().sendError(Text.literal("REFUS : « " + tenue + " » n'est pas une tenue de "
					+ cible.getName().getString()));
			return 0;
		}
		String avant = jenny.getOutfit();
		jenny.setOutfit(tenue);
		dev.pyrofetch.jn1.jouets.Lingerie.changer(jenny, avant, jenny.getOutfit(), null);
		ctx.getSource().sendFeedback(() -> Text.literal(jenny.getName().getString() + " porte " + jenny.getOutfit()), false);
		return 1;
	}

	private static int pièceDeScène(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.entity.Entity cible = EntityArgumentType.getEntity(ctx, "cible");
		if (!(cible instanceof CompanionEntity jenny) || !jenny.kind().hasSceneProp()) {
			ctx.getSource().sendError(Text.literal("REFUS : " + cible.getName().getString()
					+ " n'a pas de pièce de scène"));
			return 0;
		}
		jenny.setScenePropShown(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "visible"));
		ctx.getSource().sendFeedback(() -> Text.literal(jenny.getName().getString() + " : « "
				+ jenny.kind().sceneProp() + " » " + (jenny.isScenePropShown() ? "affiché" : "masqué")
				+ " pendant les scènes, dessiné maintenant : " + jenny.showsSceneProp()), false);
		return 1;
	}

	private static int trustGet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player,
	                            String companion) {
		PlayerProgress progress = ModAttachments.get(player);
		int trust = progress.trust(companion);
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.trust.get",
				player.getName(), companion, trust, PlayerProgress.MAX_TRUST), false);
		return trust;
	}

	private static int trustSet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player,
	                            String companion, int value) {
		PlayerProgress updated = ModAttachments.get(player).withTrust(companion, value);
		ModAttachments.set(player, updated);
		DialogueEngine.syncProgress(player, updated);
		return trustGet(ctx, player, companion);
	}

	private static int flagAdd(CommandContext<ServerCommandSource> ctx, String flag)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		DialogueEngine.setFlag(player, flag);
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.flag.added", flag), false);
		return 1;
	}

	private static int flagRemove(CommandContext<ServerCommandSource> ctx, String flag)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		PlayerProgress updated = ModAttachments.get(player).withoutFlag(flag);
		ModAttachments.set(player, updated);
		DialogueEngine.syncProgress(player, updated);
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.flag.removed", flag), false);
		return 1;
	}

	private static int flagList(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		PlayerProgress progress = ModAttachments.get(ctx.getSource().getPlayerOrThrow());
		String joined = progress.flags().isEmpty() ? "—" : String.join(", ", progress.flags());
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.flag.list", joined), false);
		return progress.flags().size();
	}

	private static int say(CommandContext<ServerCommandSource> ctx, String entryId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		CompanionEntity jenny = findNearestJenny(player);
		if (jenny == null) {
			ctx.getSource().sendError(Text.translatable("command.jn1.no_jenny"));
			return 0;
		}
		if (!DialogueEngine.forceSpeak(player, jenny, entryId)) {
			ctx.getSource().sendError(Text.translatable("command.jn1.unknown_entry", entryId));
			return 0;
		}
		return 1;
	}

	private static int setMode(CommandContext<ServerCommandSource> ctx, String modeName)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		CompanionEntity jenny = findNearestJenny(player);
		if (jenny == null) {
			ctx.getSource().sendError(Text.translatable("command.jn1.no_jenny"));
			return 0;
		}
		JennyMode mode;
		try {
			mode = JennyMode.valueOf(modeName.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			ctx.getSource().sendError(Text.translatable("command.jn1.unknown_mode", modeName));
			return 0;
		}
		jenny.setCompanion(player);
		jenny.setMode(mode);
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.mode.set", mode.name()), false);
		return 1;
	}

	/**
	 * {@code /jn1 lift call <sélecteur>} : appelle la plateforme à l'étage de ce
	 * sélecteur, sans joueur, et rend ce que le serveur voit — l'épreuve au
	 * serveur de test.
	 */
	private static int appelerAscenseur(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "palier");
		ServerWorld monde = ctx.getSource().getWorld();
		if (!(monde.getBlockEntity(pos) instanceof dev.pyrofetch.jn1.lift.SelectorBlockEntity sélecteur)) {
			ctx.getSource().sendError(Text.literal("pas de sélecteur en " + pos.toShortString()));
			return 0;
		}
		dev.pyrofetch.jn1.lift.Platforms.Plateforme p = dev.pyrofetch.jn1.lift.Platforms.desservie(monde, pos);
		String relevé = p == null ? "aucune plateforme" : "plateforme de " + p.taille() + " blocs, couche " + p.niveau()
				+ (p.enRoute() ? " (en route)" : "") + ", étages " + dev.pyrofetch.jn1.lift.Platforms.étages(monde, p)
						.stream().map(é -> é.niveau() + é.nom().map(n -> " " + n.getString()).orElse("")).toList();
		dev.pyrofetch.jn1.lift.SelectorBlock.appeler(monde, pos, null);
		ctx.getSource().sendFeedback(() -> Text.literal("appel à la couche " + sélecteur.niveau() + " : " + relevé), false);
		return 1;
	}

	private static int laboratoires(CommandContext<ServerCommandSource> ctx) {
		net.minecraft.server.MinecraftServer serveur = ctx.getSource().getServer();
		BlockPos centre = BlockPos.ofFloored(ctx.getSource().getPosition());
		java.util.List<BlockPos> ancres = dev.pyrofetch.jn1.lab.VoidLabs.repérer(
				serveur, centre, dev.pyrofetch.jn1.lab.LidarItem.PORTÉE);
		ctx.getSource().sendFeedback(() -> Text.literal(ancres.size() + " laboratoire(s) à "
				+ dev.pyrofetch.jn1.lab.LidarItem.PORTÉE + " blocs de " + centre.toShortString()), false);
		for (BlockPos ancre : ancres) {
			ctx.getSource().sendFeedback(() -> Text.literal("  ancre " + ancre.getX() + " " + ancre.getZ()), false);
		}
		if (ancres.isEmpty()) {
			return 0;
		}
		BlockPos proche = ancres.getFirst();
		ServerWorld vide = serveur.getWorld(dev.pyrofetch.jn1.lab.LabDimension.LAB);
		if (vide != null) {
			dev.pyrofetch.jn1.lab.VoidLabs.bâtir(vide, proche);
		}
		java.util.Optional<BlockPos> cave = dev.pyrofetch.jn1.lab.VoidLabs.porteDeLaMaison(serveur.getOverworld(), proche);
		ctx.getSource().sendFeedback(() -> Text.literal(cave
				.map(porte -> "porte de cave en " + porte.toShortString() + ", laboratoire bâti dessous")
				.orElse("REFUS : aucune porte de cave au-dessus de " + proche.getX() + " " + proche.getZ())), false);
		return ancres.size();
	}

	/**
	 * Fait paraître Gaster derrière l'arbre du laboratoire le plus proche, sans
	 * tirage au sort — sauf s'il y attend déjà : c'est l'épreuve de la règle
	 * « jamais deux fois ».
	 */
	private static int gaster(CommandContext<ServerCommandSource> ctx) {
		net.minecraft.server.MinecraftServer serveur = ctx.getSource().getServer();
		BlockPos centre = BlockPos.ofFloored(ctx.getSource().getPosition());
		java.util.List<BlockPos> ancres = dev.pyrofetch.jn1.lab.VoidLabs.repérer(
				serveur, centre, dev.pyrofetch.jn1.lab.LidarItem.PORTÉE);
		ServerWorld vide = serveur.getWorld(dev.pyrofetch.jn1.lab.LabDimension.LAB);
		if (ancres.isEmpty() || vide == null) {
			ctx.getSource().sendError(Text.literal("aucun laboratoire à portée"));
			return 0;
		}
		BlockPos ancre = ancres.getFirst();
		dev.pyrofetch.jn1.lab.VoidLabs.bâtir(vide, ancre);
		dev.pyrofetch.jn1.gaster.GasterEntity gaster = dev.pyrofetch.jn1.gaster.GasterEntity.apparaître(vide, ancre, true);
		net.minecraft.util.math.Vec3d place = dev.pyrofetch.jn1.lab.VoidLabs.derrièreLArbre(ancre);
		ctx.getSource().sendFeedback(() -> Text.literal(gaster == null
				? "Gaster attend déjà derrière l'arbre (" + ancre.getX() + " " + ancre.getZ() + ")"
				: String.format("Gaster paru en %.1f %.1f %.1f", place.x, place.y, place.z)), false);
		return gaster == null ? 0 : 1;
	}

	private static int photo(CommandContext<ServerCommandSource> ctx, String nomFormat, @Nullable BlockPos cadre)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerWorld monde = ctx.getSource().getWorld();
		PhotoFormat format = null;
		for (PhotoFormat candidat : PhotoFormat.values()) {
			if (candidat.asString().equals(nomFormat)) {
				format = candidat;
				break;
			}
		}
		if (format == null) {
			ctx.getSource().sendError(Text.translatable("command.jn1.unknown_format", nomFormat));
			return 0;
		}

		byte[] pixels = new byte[format.octets()];
		for (int y = 0; y < format.hauteurEnPixels(); y++) {
			for (int x = 0; x < format.largeurEnPixels(); x++) {
				pixels[y * format.largeurEnPixels() + x] = (byte) (4 + (x + y) / 16 % 60 * 4);
			}
		}

		PhotoArchive archive = PhotoArchive.of(monde);
		int numéro = archive.ajouter(new PhotoImage(format, pixels, ctx.getSource().getName()));
		PhotoFormat retenu = format;
		if (cadre == null) {
			ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
			ItemStack tirage = PhotoItem.tirage(ModItems.PHOTO, numéro, format);
			if (!player.getInventory().insertStack(tirage)) {
				player.dropItem(tirage, false);
			}
			ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.photo.made", numéro,
					retenu.asString(), retenu.largeurEnPixels(), retenu.hauteurEnPixels(), archive.compte()), false);
			return numéro;
		}
		PhotoFrameBlockEntity cible = PhotoFrameBlockEntity.at(monde, cadre);
		boolean posée = cible != null && cible.accrocher(numéro, format);
		ctx.getSource().sendFeedback(() -> Text.literal(posée
				? "photo " + numéro + " accrochée en " + cadre.toShortString()
				: "REFUS : pas la place en " + cadre.toShortString()), false);
		return posée ? numéro : 0;
	}

	private static int forme(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		var cible = EntityArgumentType.getEntity(ctx, "cible");
		String nom = StringArgumentType.getString(ctx, "forme");
		var part = java.util.Arrays.stream(dev.pyrofetch.jn1.shape.Shapes.Part.values())
				.filter(p -> p.id().equals(nom)).findFirst().orElse(null);
		if (part == null || !(cible instanceof net.minecraft.entity.LivingEntity vivant)) {
			ctx.getSource().sendError(Text.literal("REFUS : forme « " + nom + " » inconnue, ou cible sans corps"));
			return 0;
		}
		int n = dev.pyrofetch.jn1.shape.Shapes.fixer(vivant, part, IntegerArgumentType.getInteger(ctx, "niveau"));
		ctx.getSource().sendFeedback(() -> Text.literal(cible.getName().getString() + " : " + part.id() + " = " + n
				+ " (relu " + dev.pyrofetch.jn1.shape.Shapes.niveau(vivant, part) + "), échelle " + vivant.getScale()
				+ ", boîte " + String.format(java.util.Locale.ROOT, "%.2f × %.2f", vivant.getWidth(), vivant.getHeight())), false);
		return 1;
	}

	private static int cloison(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(ctx, "pos");
		var état = ctx.getSource().getWorld().getBlockState(pos);
		if (!(état.getBlock() instanceof dev.pyrofetch.jn1.gloryhole.GloryWallBlock)
				|| !(EntityArgumentType.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
			ctx.getSource().sendError(Text.literal("REFUS : pas de cloison basse en " + pos.toShortString() + ", ou pas une fille"));
			return 0;
		}
		// La moitié basse porte le trou, comme au clic.
		if (état.get(dev.pyrofetch.jn1.gloryhole.GloryWallBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.UPPER) {
			pos = pos.down();
		}
		var normale = état.get(dev.pyrofetch.jn1.gloryhole.GloryWallBlock.FACING);
		String scène = dev.pyrofetch.jn1.gloryhole.GloryWall.installer(fille, pos, normale, normale);
		BlockPos bas = pos;
		double plan = fille.getPos().subtract(net.minecraft.util.math.Vec3d.ofBottomCenter(bas))
				.dotProduct(net.minecraft.util.math.Vec3d.of(normale.getVector()));
		ctx.getSource().sendFeedback(() -> Text.literal(fille.getName().getString() + " joue " + scène
				+ String.format(java.util.Locale.ROOT, " à %.3f bloc du panneau, en %.2f %.2f %.2f, lacet %.0f",
				plan, fille.getX(), fille.getY(), fille.getZ(), fille.getYaw())), false);
		return scène == null ? 0 : 1;
	}

	private static int vitrine(CommandContext<ServerCommandSource> ctx, BlockPos pos) {
		ServerWorld monde = ctx.getSource().getWorld();
		if (!(monde.getBlockState(pos).getBlock() instanceof dev.pyrofetch.jn1.block.VendingMachineBlock)) {
			ctx.getSource().sendError(Text.literal("pas de distributeur en " + pos.toShortString()));
			return 0;
		}
		BlockPos bas = dev.pyrofetch.jn1.block.VendingMachineBlock.bas(pos, monde.getBlockState(pos));
		var machine = monde.getBlockEntity(bas) instanceof dev.pyrofetch.jn1.block.VendingMachineBlockEntity m ? m : null;
		var offres = dev.pyrofetch.jn1.block.VendingMachineBlockEntity.vitrine(machine, monde);
		String état = machine == null ? "sans entité" : machine.garnie() ? "garni" : "catalogue";
		ctx.getSource().sendFeedback(() -> Text.literal("vitrine " + état + ", " + offres.size() + " articles"), false);
		for (var offre : offres) {
			ctx.getSource().sendFeedback(() -> Text.literal("  " + offre.place() + " " + offre.id() + " "
					+ offre.stack().getCount() + "x" + net.minecraft.registry.Registries.ITEM.getId(offre.stack().getItem())
					+ " = " + offre.price()), false);
		}
		return offres.size();
	}

	private static int toggleUnsettled(CommandContext<ServerCommandSource> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		CompanionEntity jenny = findNearestJenny(ctx.getSource().getPlayerOrThrow());
		if (jenny == null) {
			ctx.getSource().sendError(Text.translatable("command.jn1.no_jenny"));
			return 0;
		}
		jenny.setUnsettled(!jenny.isUnsettled());
		boolean now = jenny.isUnsettled();
		ctx.getSource().sendFeedback(() -> Text.translatable("command.jn1.unsettled", now), false);
		return 1;
	}

	@Nullable
	public static CompanionEntity findNearestJenny(ServerPlayerEntity player) {
		Box box = player.getBoundingBox().expand(64.0);
		List<CompanionEntity> found = player.getServerWorld()
				.getEntitiesByClass(CompanionEntity.class, box, entity -> true);
		return found.stream()
				.min(Comparator.comparingDouble(player::squaredDistanceTo))
				.orElse(null);
	}
}
