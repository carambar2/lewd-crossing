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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Outils de test. Rien ici n'est nécessaire pour jouer : c'est ce qui permet de
 * sauter à un point précis de l'arc sans le rejouer en entier.
 */
public final class ModCommands {
	/** Niveau 2 en 1.21.1 : celui des maîtres du jeu (26.3 : {@code Commands.LEVEL_GAMEMASTERS}). */

	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("jn1")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

						// Franchit la porte sans avoir à la chercher : sert à
						// vérifier le passage, et à ressortir si l'on s'est
						// retrouvé coincé.
						.then(Commands.literal("lab")
								.executes(ModCommands::passerLaPorte))

						// Les laboratoires du vide autour d'ici, tels que le lidar les
						// voit ; bâtit le plus proche et cherche sa porte de cave. C'est
						// ainsi qu'on éprouve le parallèle sur le serveur de test.
						// Appuie sur un bouton d'appel sans joueur : c'est ainsi qu'on
						// éprouve la cabine et ses portes au serveur de test.
						.then(Commands.literal("lift")
								.then(Commands.literal("call")
										.then(Commands.argument("palier", BlockPosArgument.blockPos())
												.executes(ModCommands::appelerAscenseur)))
								.then(Commands.literal("assemble")
										.then(Commands.argument("palier", BlockPosArgument.blockPos())
												.executes(ctx -> {
													BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "palier");
													dev.pyrofetch.jn1.lift.Platforms.assembler(ctx.getSource().getLevel(), pos);
													int n = dev.pyrofetch.jn1.lift.Platforms.groupe(ctx.getSource().getLevel(), pos).size();
													ctx.getSource().sendSuccess(() -> Component.literal("plateforme assemblée : " + n + " blocs"), false);
													return n;
												}))))

						.then(Commands.literal("void")
								.then(Commands.literal("labs")
										.executes(ModCommands::laboratoires))
								.then(Commands.literal("gaster")
										.executes(ModCommands::gaster)))

						.then(Commands.literal("trust")
								.then(Commands.literal("get")
										.then(Commands.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.executes(ctx -> trustGet(ctx, ctx.getSource().getPlayerOrException(),
														StringArgumentType.getString(ctx, "companion")))
												.then(Commands.argument("player", EntityArgument.player())
														.executes(ctx -> trustGet(ctx,
																EntityArgument.getPlayer(ctx, "player"),
																StringArgumentType.getString(ctx, "companion"))))))
								.then(Commands.literal("set")
										.then(Commands.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.then(Commands.argument("value", IntegerArgumentType.integer(
																PlayerProgress.MIN_TRUST, PlayerProgress.MAX_TRUST))
														.executes(ctx -> trustSet(ctx,
																ctx.getSource().getPlayerOrException(),
																StringArgumentType.getString(ctx, "companion"),
																IntegerArgumentType.getInteger(ctx, "value")))
														.then(Commands.argument("player",
																		EntityArgument.player())
																.executes(ctx -> trustSet(ctx,
																		EntityArgument.getPlayer(ctx, "player"),
																		StringArgumentType.getString(ctx, "companion"),
																		IntegerArgumentType.getInteger(ctx, "value")))))))
								.then(Commands.literal("add")
										.then(Commands.argument("companion", StringArgumentType.word())
												.suggests(COMPANIONS)
												.then(Commands.argument("value",
																IntegerArgumentType.integer(-10, 10))
														.executes(ctx -> {
															ServerPlayer player =
																	ctx.getSource().getPlayerOrException();
															String nom = StringArgumentType.getString(
																	ctx, "companion");
															DialogueEngine.grantTrust(player, nom,
																	IntegerArgumentType.getInteger(ctx, "value"));
															return trustGet(ctx, player, nom);
														})))))

						.then(Commands.literal("flag")
								.then(Commands.literal("add")
										.then(Commands.argument("flag", StringArgumentType.word())
												.executes(ctx -> flagAdd(ctx,
														StringArgumentType.getString(ctx, "flag")))))
								.then(Commands.literal("remove")
										.then(Commands.argument("flag", StringArgumentType.word())
												.executes(ctx -> flagRemove(ctx,
														StringArgumentType.getString(ctx, "flag")))))
								.then(Commands.literal("list")
										.executes(ModCommands::flagList)))

						.then(Commands.literal("say")
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											DialogueLoader.getInstance().entries()
													.forEach(entry -> builder.suggest(entry.id()));
											return builder.buildFuture();
										})
										.executes(ctx -> say(ctx, StringArgumentType.getString(ctx, "id")))))

						.then(Commands.literal("mode")
								.then(Commands.argument("mode", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (JennyMode mode : JennyMode.values()) {
												builder.suggest(mode.name().toLowerCase(java.util.Locale.ROOT));
											}
											return builder.buildFuture();
										})
										.executes(ctx -> setMode(ctx, StringArgumentType.getString(ctx, "mode")))))

						.then(Commands.literal("unsettled")
								.executes(ModCommands::toggleUnsettled))

						// La transformation, sans potion ni écran : « none » la défait.
						.then(Commands.literal("morph")
								.then(Commands.argument("joueur", EntityArgument.player())
										.then(Commands.argument("compagne", StringArgumentType.word())
												.suggests((ctx, builder) -> {
													builder.suggest("none");
													CompanionKind.all().forEach(kind -> builder.suggest(kind.id()));
													return builder.buildFuture();
												})
												.executes(ctx -> morph(ctx, ""))
												.then(Commands.argument("tenue", StringArgumentType.word())
														.suggests(TENUES)
														.executes(ctx -> morph(ctx,
																StringArgumentType.getString(ctx, "tenue")))))))

						// La tenue d'une compagne, sans passer par son menu.
						.then(Commands.literal("outfit")
								.then(Commands.argument("cible", EntityArgument.entity())
										.then(Commands.argument("tenue", StringArgumentType.word())
												.suggests(TENUES)
												.executes(ModCommands::tenue))))

						// Ce que fait la télécommande, sans joueur ni écran : c'est
						// ainsi qu'on éprouve les scènes au serveur de test.
						.then(Commands.literal("scene")
								.then(Commands.argument("cible", EntityArgument.entity())
										.then(Commands.literal("stop")
												.executes(ctx -> scène(ctx, "")))
										.then(Commands.literal("prop")
												.then(Commands.argument("visible",
																com.mojang.brigadier.arguments.BoolArgumentType.bool())
														.executes(ModCommands::pièceDeScène)))
										.then(Commands.argument("nom", StringArgumentType.word())
												.suggests(SCENES)
												.executes(ctx -> scène(ctx,
														StringArgumentType.getString(ctx, "nom"))))))

						// Une photo d'essai : un dégradé en diagonale, rangé dans
						// l'archive comme une vraie. Remise en main, ou accrochée
						// directement au cadre désigné.
						.then(Commands.literal("photo")
								.then(Commands.argument("format", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (PhotoFormat format : PhotoFormat.values()) {
												builder.suggest(format.getSerializedName());
											}
											return builder.buildFuture();
										})
										.executes(ctx -> photo(ctx, StringArgumentType.getString(ctx, "format"), null))
										.then(Commands.argument("cadre", BlockPosArgument.blockPos())
												.executes(ctx -> photo(ctx, StringArgumentType.getString(ctx, "format"),
														BlockPosArgument.getLoadedBlockPos(ctx, "cadre"))))))

						// Les formes d'une compagne ou d'un joueur, comme une potion
						// les fixerait : l'épreuve au serveur de test.
						.then(Commands.literal("shape")
								.then(Commands.argument("cible", EntityArgument.entity())
										.then(Commands.argument("forme", StringArgumentType.word())
												.suggests((ctx, builder) -> {
													for (var part : dev.pyrofetch.jn1.shape.Shapes.Part.values()) {
														builder.suggest(part.id());
													}
													return builder.buildFuture();
												})
												.then(Commands.argument("niveau", IntegerArgumentType.integer(-9, 9))
														.executes(ModCommands::forme)))))

						// Pose une fille derrière une cloison à trou, sans joueur :
						// côté de la face du panneau.
						.then(Commands.literal("wall")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.then(Commands.argument("cible", EntityArgument.entity())
												.executes(ModCommands::cloison))))

						// La fin d'une boucle de scène « cu », sans attendre la scène.
						// Homme ou femme, sans écran ni potion.
						.then(Commands.literal("gender")
								.then(Commands.argument("joueur", EntityArgument.player())
										.then(Commands.argument("genre", StringArgumentType.word())
												.suggests((ctx, builder) -> builder.suggest("homme").suggest("femme").buildFuture())
												.executes(ctx -> {
													String genre = StringArgumentType.getString(ctx, "genre");
													if (!genre.equals(dev.pyrofetch.jn1.gender.Gender.HOMME)
															&& !genre.equals(dev.pyrofetch.jn1.gender.Gender.FEMME)) {
														ctx.getSource().sendFailure(Component.literal("REFUS : homme ou femme"));
														return 0;
													}
													ServerPlayer joueur = EntityArgument.getPlayer(ctx, "joueur");
													dev.pyrofetch.jn1.gender.Gender.fixer(joueur, genre);
													ctx.getSource().sendSuccess(() -> Component.literal(
															joueur.getName().getString() + " : " + genre), false);
													return 1;
												}))))
						// Menotter ou libérer, sans joueur.
						.then(Commands.literal("cuff")
								.then(Commands.argument("cible", EntityArgument.entity())
										.then(Commands.argument("menottée",
														com.mojang.brigadier.arguments.BoolArgumentType.bool())
												.executes(ctx -> {
													if (!(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
														ctx.getSource().sendFailure(Component.literal("REFUS : pas une fille"));
														return 0;
													}
													fille.setCuffed(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "menottée"));
													ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString()
															+ " menottée : " + fille.isCuffed()), false);
													return 1;
												}))))
						// Faire danser une fille à une barre, sans joueur.
						.then(Commands.literal("pole")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.then(Commands.argument("cible", EntityArgument.entity())
												.executes(ctx -> {
													if (!(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
														ctx.getSource().sendFailure(Component.literal("REFUS : pas une fille"));
														return 0;
													}
													net.minecraft.core.BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
													dev.pyrofetch.jn1.jouets.DancePoleBlock.danser(fille, pos,
															net.minecraft.world.phys.Vec3.atBottomCenterOf(pos).add(0, 0, 3));
													ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString()
															+ " danse : " + fille.getScene() + String.format(java.util.Locale.ROOT,
															" à %.2f %.2f %.2f, lacet %.0f", fille.getX(), fille.getY(), fille.getZ(),
															fille.getYRot())), false);
													return 1;
												}))))
						// Le vrai chemin du réveil, au serveur de test : un joueur factice,
						// compagnon de la fille, s'endort et se réveille à deux blocs d'elle.
						.then(Commands.literal("sleeptest")
								.then(Commands.argument("cible", EntityArgument.entity())
										.executes(ctx -> {
											if (!(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendFailure(Component.literal("REFUS : pas une fille"));
												return 0;
											}
											net.minecraft.server.level.ServerLevel monde = (net.minecraft.server.level.ServerLevel) fille.level();
											ServerPlayer dormeur = net.fabricmc.fabric.api.entity.FakePlayer.get(monde);
											dormeur.snapTo(fille.getX() + 2, fille.getY(), fille.getZ(), 90, 0);
											fille.setCompanion(dormeur);
											// Un lit, tête à l'est, à trois blocs d'elle.
											net.minecraft.core.BlockPos pied = fille.blockPosition().east(3);
											net.minecraft.core.BlockPos tête = pied.east();
											monde.setBlockAndUpdate(pied, net.minecraft.world.level.block.Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState()
													.setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST)
													.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
											monde.setBlockAndUpdate(tête, net.minecraft.world.level.block.Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState()
													.setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST)
													.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
											// Le joueur factice de Fabric refuse de dormir : on lance
											// l'événement de réveil comme le jeu le fait, avec ce lit.
											net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.STOP_SLEEPING.invoker()
													.onStopSleeping(dormeur, tête);
											ctx.getSource().sendSuccess(() -> Component.literal("dormeur réveillé, lit : pied "
													+ pied.toShortString() + ", tête " + tête.toShortString()), false);
											return 1;
										})))
						// Le réveil, sans dormir : une fille vient le branler.
						// /jn1 demande <fille> <demande> [brutale] : une demande du menu,
						// sans menu ni seuil d'affection, au joueur qui tape la commande.
						.then(Commands.literal("demande")
								.then(Commands.argument("fille", EntityArgument.entity())
										.then(Commands.argument("demande", StringArgumentType.word())
												.suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
														dev.pyrofetch.jn1.entity.Demande.TOUTES.stream()
																.map(dev.pyrofetch.jn1.entity.Demande::id), b))
												.executes(ctx -> demande(ctx, false))
												.then(Commands.literal("brutale")
														.executes(ctx -> demande(ctx, true))))))
						.then(Commands.literal("wakeup")
								.then(Commands.argument("joueur", EntityArgument.player())
										.executes(ctx -> {
											ServerPlayer joueur = EntityArgument.getPlayer(ctx, "joueur");
											CompanionEntity fille = dev.pyrofetch.jn1.excitation.Reveil.réveiller(joueur, true);
											ctx.getSource().sendSuccess(() -> Component.literal(fille == null ? "personne"
													: fille.getName().getString() + " : " + fille.getScene()), false);
											return fille == null ? 0 : 1;
										})))
						// La barre d'excitation pleine, sans joueur : la scène passe à sa fin.
						.then(Commands.literal("finish")
								.then(Commands.argument("cible", EntityArgument.entity())
										.executes(ctx -> {
											if (!(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendFailure(Component.literal("REFUS : pas une fille"));
												return 0;
											}
											String avant = fille.getScene();
											String fin = fille.finishScene();
											ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString()
													+ " : " + avant + " → " + (fin.isEmpty() ? "arrêtée" : fin)), false);
											return 1;
										})))
						.then(Commands.literal("cum")
								.then(Commands.argument("cible", EntityArgument.entity())
										.executes(ctx -> {
											if (!(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
												ctx.getSource().sendFailure(Component.literal("REFUS : pas une fille"));
												return 0;
											}
											dev.pyrofetch.jn1.cum.Cum.jaillir(fille);
											ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString()
													+ " : traces " + fille.getTraces()), false);
											return 1;
										})))

						// Ce que le serveur voit en vitrine d'un distributeur : c'est
						// l'épreuve des rayons garnis, sans joueur ni écran.
						.then(Commands.literal("vending")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> vitrine(ctx,
												BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))

						.then(Commands.literal("dialogue")
								.then(Commands.literal("count")
										.executes(ctx -> {
											int count = DialogueLoader.getInstance().entries().size();
											ctx.getSource().sendSuccess(
													() -> Component.translatable("command.jn1.dialogue.count", count),
													false);
											return count;
										})))));
	}

	/** Les compagnes connues, proposées à la saisie. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
			COMPANIONS = (ctx, builder) -> {
		for (dev.pyrofetch.jn1.shop.GirlEntry entry
				: dev.pyrofetch.jn1.shop.GirlEntry.catalogue()) {
			builder.suggest(entry.id());
		}
		return builder.buildFuture();
	};

	/** Les scènes de toutes les compagnes qui en ont. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
			SCENES = (ctx, builder) -> {
		for (CompanionKind kind : CompanionKind.all()) {
			CompanionScenes.of(kind).forEach(builder::suggest);
		}
		return builder.buildFuture();
	};

	private static int demande(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, boolean brutale)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer joueur = ctx.getSource().getPlayerOrException();
		if (!(EntityArgument.getEntity(ctx, "fille") instanceof CompanionEntity fille)) {
			ctx.getSource().sendFailure(Component.literal("pas une compagne"));
			return 0;
		}
		dev.pyrofetch.jn1.entity.Demande demande = dev.pyrofetch.jn1.entity.Demande.byId(
				StringArgumentType.getString(ctx, "demande"));
		if (demande == null || !demande.possible(fille.kind())) {
			ctx.getSource().sendFailure(Component.literal("demande inconnue, ou qu'elle ne sait pas jouer"));
			return 0;
		}
		fille.commencerDemande(joueur, demande, brutale);
		ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString() + " : " + demande.id()
				+ (brutale ? " (brutale)" : "") + ", " + fille.getScene()), false);
		return 1;
	}

	private static int scène(CommandContext<CommandSourceStack> ctx, String nom)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.world.entity.Entity cible = EntityArgument.getEntity(ctx, "cible");
		if (!(cible instanceof CompanionEntity jenny)) {
			ctx.getSource().sendFailure(Component.literal("REFUS : " + cible.getName().getString()
					+ " n'est pas une compagne"));
			return 0;
		}
		String qui = jenny.getName().getString();
		if (nom.isEmpty()) {
			jenny.stopScene();
			ctx.getSource().sendSuccess(() -> Component.literal(qui + " : scène arrêtée"), false);
			return 1;
		}
		List<String> scènes = CompanionScenes.of(jenny.kind());
		if (!scènes.contains(nom)) {
			ctx.getSource().sendFailure(Component.literal("REFUS : « " + nom + " » inconnue de " + qui
					+ " ; connues : " + (scènes.isEmpty() ? "aucune" : String.join(", ", scènes))));
			return 0;
		}
		jenny.playScene(nom);
		net.minecraft.world.phys.AABB boîte = jenny.getBoundingBox();
		ctx.getSource().sendSuccess(() -> Component.literal(qui + " joue " + jenny.getScene()
				+ " (" + jenny.kind().sceneAnimation(jenny.getScene()) + ") — corps à "
				+ Math.round(jenny.yBodyRot) + "°, boîte x " + String.format(java.util.Locale.ROOT, "%.2f…%.2f", boîte.minX, boîte.maxX)
				+ " z " + String.format(java.util.Locale.ROOT, "%.2f…%.2f", boîte.minZ, boîte.maxZ)), false);
		return 1;
	}

	/** Toutes les tenues de toutes les compagnes. */
	private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
			TENUES = (ctx, builder) -> {
		CompanionKind.all().forEach(kind -> kind.outfits().forEach(builder::suggest));
		return builder.buildFuture();
	};

	private static int morph(CommandContext<CommandSourceStack> ctx, String tenue)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer joueur = EntityArgument.getPlayer(ctx, "joueur");
		String nom = StringArgumentType.getString(ctx, "compagne");
		if (nom.equals("none")) {
			dev.pyrofetch.jn1.morph.MorphHandler.set(joueur, dev.pyrofetch.jn1.morph.MorphState.NONE);
			ctx.getSource().sendSuccess(() -> Component.literal(joueur.getName().getString() + " redevient lui-même"), false);
			return 1;
		}
		CompanionKind kind = CompanionKind.byId(nom);
		if (kind == null) {
			ctx.getSource().sendFailure(Component.literal("REFUS : compagne inconnue « " + nom + " »"));
			return 0;
		}
		String portée = kind.outfit(tenue);
		dev.pyrofetch.jn1.morph.MorphHandler.set(joueur,
				dev.pyrofetch.jn1.registry.ModAttachments.morph(joueur).with(kind.id(), portée, false));
		ctx.getSource().sendSuccess(() -> Component.literal(joueur.getName().getString() + " prend l'apparence de "
				+ kind.id() + " en tenue " + portée), false);
		return 1;
	}

	private static int tenue(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.world.entity.Entity cible = EntityArgument.getEntity(ctx, "cible");
		String tenue = StringArgumentType.getString(ctx, "tenue");
		if (!(cible instanceof CompanionEntity jenny) || !jenny.kind().outfits().contains(tenue)) {
			ctx.getSource().sendFailure(Component.literal("REFUS : « " + tenue + " » n'est pas une tenue de "
					+ cible.getName().getString()));
			return 0;
		}
		String avant = jenny.getOutfit();
		jenny.setOutfit(tenue);
		dev.pyrofetch.jn1.jouets.Lingerie.changer(jenny, avant, jenny.getOutfit(), null);
		ctx.getSource().sendSuccess(() -> Component.literal(jenny.getName().getString() + " porte " + jenny.getOutfit()), false);
		return 1;
	}

	private static int pièceDeScène(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		net.minecraft.world.entity.Entity cible = EntityArgument.getEntity(ctx, "cible");
		if (!(cible instanceof CompanionEntity jenny) || !jenny.kind().hasSceneProp()) {
			ctx.getSource().sendFailure(Component.literal("REFUS : " + cible.getName().getString()
					+ " n'a pas de pièce de scène"));
			return 0;
		}
		jenny.setScenePropShown(com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "visible"));
		ctx.getSource().sendSuccess(() -> Component.literal(jenny.getName().getString() + " : « "
				+ jenny.kind().sceneProp() + " » " + (jenny.isScenePropShown() ? "affiché" : "masqué")
				+ " pendant les scènes, dessiné maintenant : " + jenny.showsSceneProp()), false);
		return 1;
	}

	private static int trustGet(CommandContext<CommandSourceStack> ctx, ServerPlayer player,
	                            String companion) {
		PlayerProgress progress = ModAttachments.get(player);
		int trust = progress.trust(companion);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.trust.get",
				player.getName(), companion, trust, PlayerProgress.MAX_TRUST), false);
		return trust;
	}

	private static int trustSet(CommandContext<CommandSourceStack> ctx, ServerPlayer player,
	                            String companion, int value) {
		PlayerProgress updated = ModAttachments.get(player).withTrust(companion, value);
		ModAttachments.set(player, updated);
		DialogueEngine.syncProgress(player, updated);
		return trustGet(ctx, player, companion);
	}

	private static int flagAdd(CommandContext<CommandSourceStack> ctx, String flag)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		DialogueEngine.setFlag(player, flag);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.flag.added", flag), false);
		return 1;
	}

	private static int flagRemove(CommandContext<CommandSourceStack> ctx, String flag)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		PlayerProgress updated = ModAttachments.get(player).withoutFlag(flag);
		ModAttachments.set(player, updated);
		DialogueEngine.syncProgress(player, updated);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.flag.removed", flag), false);
		return 1;
	}

	private static int flagList(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		PlayerProgress progress = ModAttachments.get(ctx.getSource().getPlayerOrException());
		String joined = progress.flags().isEmpty() ? "—" : String.join(", ", progress.flags());
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.flag.list", joined), false);
		return progress.flags().size();
	}

	private static int say(CommandContext<CommandSourceStack> ctx, String entryId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		CompanionEntity jenny = findNearestJenny(player);
		if (jenny == null) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.no_jenny"));
			return 0;
		}
		if (!DialogueEngine.forceSpeak(player, jenny, entryId)) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.unknown_entry", entryId));
			return 0;
		}
		return 1;
	}

	private static int setMode(CommandContext<CommandSourceStack> ctx, String modeName)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		CompanionEntity jenny = findNearestJenny(player);
		if (jenny == null) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.no_jenny"));
			return 0;
		}
		JennyMode mode;
		try {
			mode = JennyMode.valueOf(modeName.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.unknown_mode", modeName));
			return 0;
		}
		jenny.setCompanion(player);
		jenny.setMode(mode);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.mode.set", mode.name()), false);
		return 1;
	}

	/**
	 * {@code /jn1 lab} : le passage de la porte, sans la chercher — à la surface
	 * vers le vide, dans le vide vers la surface.
	 *
	 * <p>Trois précautions (26.3). Une porte de cave <em>ouverte</em> fait déjà
	 * passer qui s'y tient : se téléporter dedans puis taper la commande faisait
	 * l'aller par la porte et aussitôt le retour par la commande — on se
	 * croyait renvoyé. La commande respecte donc le délai de la porte
	 * ({@link dev.pyrofetch.jn1.lab.LabDoorWatcher#COOLDOWN_TICKS}) et le note
	 * à son tour. Elle prend la porte où l'on se tient, ou à un pas, avec son
	 * sens : sans cela, l'ancre était le bloc des pieds, qui pouvait être le
	 * voisin de la porte (un second laboratoire bâti de travers), et le retour
	 * se faisait un pas au nord, parfois dans un mur. Et elle dit ce qu'elle a fait.
	 */
	private static int passerLaPorte(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer joueur = ctx.getSource().getPlayerOrException();
		int depuis = dev.pyrofetch.jn1.lab.LabDoorWatcher.depuisLaPorte(joueur);
		if (depuis < dev.pyrofetch.jn1.lab.LabDoorWatcher.COOLDOWN_TICKS) {
			ctx.getSource().sendFailure(Component.literal("la porte ouverte vient de faire passer, il y a " + depuis
					+ " ticks : rien de plus (sinon, ce serait aussitôt le retour)"));
			return 0;
		}
		BlockPos porte = dev.pyrofetch.jn1.lab.LabDoorWatcher.porteProche(joueur);
		net.minecraft.core.Direction sens = porte != null
				? joueur.level().getBlockState(porte).getValue(net.minecraft.world.level.block.DoorBlock.FACING)
				: net.minecraft.core.Direction.NORTH;
		String départ = joueur.level().dimension().identifier().toString();
		dev.pyrofetch.jn1.lab.LabDimension.cross(joueur, porte != null ? porte : joueur.blockPosition(), sens);
		dev.pyrofetch.jn1.lab.LabDoorWatcher.noterPassage(joueur);
		String arrivée = joueur.level().dimension().identifier() + " " + BlockPos.containing(joueur.position()).toShortString();
		ctx.getSource().sendSuccess(() -> Component.literal("passage de " + départ + " vers " + arrivée
				+ (porte != null ? " (porte en " + porte.toShortString() + ")" : " (pas de porte à portée : ancre aux pieds)")), false);
		return 1;
	}

	/**
	 * {@code /jn1 lift call <sélecteur>} : appelle la plateforme à l'étage de ce
	 * sélecteur, sans joueur, et rend ce que le serveur voit — l'épreuve au
	 * serveur de test.
	 */
	private static int appelerAscenseur(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "palier");
		ServerLevel monde = ctx.getSource().getLevel();
		if (!(monde.getBlockEntity(pos) instanceof dev.pyrofetch.jn1.lift.SelectorBlockEntity sélecteur)) {
			ctx.getSource().sendFailure(Component.literal("pas de sélecteur en " + pos.toShortString()));
			return 0;
		}
		dev.pyrofetch.jn1.lift.Platforms.Plateforme p = dev.pyrofetch.jn1.lift.Platforms.desservie(monde, pos);
		String relevé = p == null ? "aucune plateforme" : "plateforme de " + p.taille() + " blocs, couche " + p.niveau()
				+ (p.enRoute() ? " (en route)" : "") + ", étages " + dev.pyrofetch.jn1.lift.Platforms.étages(monde, p)
						.stream().map(é -> é.niveau() + é.nom().map(n -> " " + n.getString()).orElse("")).toList();
		// Sans joueur, un refus ne se dit nulle part : on le rend ici (chemin encombré, déjà là…).
		String refus = p == null ? "aucune plateforme" : dev.pyrofetch.jn1.lift.Platforms.refus(monde, p, sélecteur.niveau());
		dev.pyrofetch.jn1.lift.SelectorBlock.appeler(monde, pos, null);
		ctx.getSource().sendSuccess(() -> Component.literal("appel à la couche " + sélecteur.niveau() + " : " + relevé
				+ (refus == null ? " — partie" : " — refusé : " + refus)), false);
		return 1;
	}

	private static int laboratoires(CommandContext<CommandSourceStack> ctx) {
		net.minecraft.server.MinecraftServer serveur = ctx.getSource().getServer();
		BlockPos centre = BlockPos.containing(ctx.getSource().getPosition());
		java.util.List<BlockPos> ancres = dev.pyrofetch.jn1.lab.VoidLabs.repérer(
				serveur, centre, dev.pyrofetch.jn1.lab.LidarItem.PORTÉE);
		ctx.getSource().sendSuccess(() -> Component.literal(ancres.size() + " laboratoire(s) à "
				+ dev.pyrofetch.jn1.lab.LidarItem.PORTÉE + " blocs de " + centre.toShortString()), false);
		for (BlockPos ancre : ancres) {
			ctx.getSource().sendSuccess(() -> Component.literal("  ancre " + ancre.getX() + " " + ancre.getZ()), false);
		}
		if (ancres.isEmpty()) {
			return 0;
		}
		BlockPos proche = ancres.getFirst();
		ServerLevel vide = serveur.getLevel(dev.pyrofetch.jn1.lab.LabDimension.LAB);
		if (vide != null) {
			dev.pyrofetch.jn1.lab.VoidLabs.bâtir(vide, proche);
		}
		java.util.Optional<BlockPos> cave = dev.pyrofetch.jn1.lab.VoidLabs.porteDeLaMaison(serveur.overworld(), proche);
		ctx.getSource().sendSuccess(() -> Component.literal(cave
				.map(porte -> "porte de cave en " + porte.toShortString() + ", laboratoire bâti dessous")
				.orElse("REFUS : aucune porte de cave au-dessus de " + proche.getX() + " " + proche.getZ())), false);
		return ancres.size();
	}

	/**
	 * Fait paraître Gaster derrière l'arbre du laboratoire le plus proche, sans
	 * tirage au sort — sauf s'il y attend déjà : c'est l'épreuve de la règle
	 * « jamais deux fois ».
	 */
	private static int gaster(CommandContext<CommandSourceStack> ctx) {
		net.minecraft.server.MinecraftServer serveur = ctx.getSource().getServer();
		BlockPos centre = BlockPos.containing(ctx.getSource().getPosition());
		java.util.List<BlockPos> ancres = dev.pyrofetch.jn1.lab.VoidLabs.repérer(
				serveur, centre, dev.pyrofetch.jn1.lab.LidarItem.PORTÉE);
		ServerLevel vide = serveur.getLevel(dev.pyrofetch.jn1.lab.LabDimension.LAB);
		if (ancres.isEmpty() || vide == null) {
			ctx.getSource().sendFailure(Component.literal("aucun laboratoire à portée"));
			return 0;
		}
		BlockPos ancre = ancres.getFirst();
		dev.pyrofetch.jn1.lab.VoidLabs.bâtir(vide, ancre);
		dev.pyrofetch.jn1.gaster.GasterEntity gaster = dev.pyrofetch.jn1.gaster.GasterEntity.apparaître(vide, ancre, true);
		net.minecraft.world.phys.Vec3 place = dev.pyrofetch.jn1.lab.VoidLabs.derrièreLArbre(ancre);
		ctx.getSource().sendSuccess(() -> Component.literal(gaster == null
				? "Gaster attend déjà derrière l'arbre (" + ancre.getX() + " " + ancre.getZ() + ")"
				: String.format("Gaster paru en %.1f %.1f %.1f", place.x, place.y, place.z)), false);
		return gaster == null ? 0 : 1;
	}

	private static int photo(CommandContext<CommandSourceStack> ctx, String nomFormat, @Nullable BlockPos cadre)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerLevel monde = ctx.getSource().getLevel();
		PhotoFormat format = null;
		for (PhotoFormat candidat : PhotoFormat.values()) {
			if (candidat.getSerializedName().equals(nomFormat)) {
				format = candidat;
				break;
			}
		}
		if (format == null) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.unknown_format", nomFormat));
			return 0;
		}

		byte[] pixels = new byte[format.octets()];
		for (int y = 0; y < format.hauteurEnPixels(); y++) {
			for (int x = 0; x < format.largeurEnPixels(); x++) {
				pixels[y * format.largeurEnPixels() + x] = (byte) (4 + (x + y) / 16 % 60 * 4);
			}
		}

		PhotoArchive archive = PhotoArchive.of(monde);
		int numéro = archive.ajouter(new PhotoImage(format, pixels, ctx.getSource().getTextName()));
		PhotoFormat retenu = format;
		if (cadre == null) {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			ItemStack tirage = PhotoItem.tirage(ModItems.PHOTO, numéro, format);
			if (!player.getInventory().add(tirage)) {
				player.drop(tirage, false, net.minecraft.util.Prediction.SERVER_ONLY);
			}
			ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.photo.made", numéro,
					retenu.getSerializedName(), retenu.largeurEnPixels(), retenu.hauteurEnPixels(), archive.compte()), false);
			return numéro;
		}
		PhotoFrameBlockEntity cible = PhotoFrameBlockEntity.at(monde, cadre);
		boolean posée = cible != null && cible.accrocher(numéro, format);
		ctx.getSource().sendSuccess(() -> Component.literal(posée
				? "photo " + numéro + " accrochée en " + cadre.toShortString()
				: "REFUS : pas la place en " + cadre.toShortString()), false);
		return posée ? numéro : 0;
	}

	private static int forme(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		var cible = EntityArgument.getEntity(ctx, "cible");
		String nom = StringArgumentType.getString(ctx, "forme");
		var part = java.util.Arrays.stream(dev.pyrofetch.jn1.shape.Shapes.Part.values())
				.filter(p -> p.id().equals(nom)).findFirst().orElse(null);
		if (part == null || !(cible instanceof net.minecraft.world.entity.LivingEntity vivant)) {
			ctx.getSource().sendFailure(Component.literal("REFUS : forme « " + nom + " » inconnue, ou cible sans corps"));
			return 0;
		}
		int n = dev.pyrofetch.jn1.shape.Shapes.fixer(vivant, part, IntegerArgumentType.getInteger(ctx, "niveau"));
		ctx.getSource().sendSuccess(() -> Component.literal(cible.getName().getString() + " : " + part.id() + " = " + n
				+ " (relu " + dev.pyrofetch.jn1.shape.Shapes.niveau(vivant, part) + "), échelle " + vivant.getScale()
				+ ", boîte " + String.format(java.util.Locale.ROOT, "%.2f × %.2f", vivant.getBbWidth(), vivant.getBbHeight())), false);
		return 1;
	}

	private static int cloison(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		var état = ctx.getSource().getLevel().getBlockState(pos);
		if (!(état.getBlock() instanceof dev.pyrofetch.jn1.gloryhole.GloryWallBlock)
				|| !(EntityArgument.getEntity(ctx, "cible") instanceof CompanionEntity fille)) {
			ctx.getSource().sendFailure(Component.literal("REFUS : pas de cloison basse en " + pos.toShortString() + ", ou pas une fille"));
			return 0;
		}
		// La moitié basse porte le trou, comme au clic.
		if (état.getValue(dev.pyrofetch.jn1.gloryhole.GloryWallBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
			pos = pos.below();
		}
		var normale = état.getValue(dev.pyrofetch.jn1.gloryhole.GloryWallBlock.FACING);
		String scène = dev.pyrofetch.jn1.gloryhole.GloryWall.installer(fille, pos, normale, normale);
		BlockPos bas = pos;
		double plan = fille.position().subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(bas))
				.dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(normale.getUnitVec3i()));
		ctx.getSource().sendSuccess(() -> Component.literal(fille.getName().getString() + " joue " + scène
				+ String.format(java.util.Locale.ROOT, " à %.3f bloc du panneau, en %.2f %.2f %.2f, lacet %.0f",
				plan, fille.getX(), fille.getY(), fille.getZ(), fille.getYRot())), false);
		return scène == null ? 0 : 1;
	}

	private static int vitrine(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		ServerLevel monde = ctx.getSource().getLevel();
		if (!(monde.getBlockState(pos).getBlock() instanceof dev.pyrofetch.jn1.block.VendingMachineBlock)) {
			ctx.getSource().sendFailure(Component.literal("pas de distributeur en " + pos.toShortString()));
			return 0;
		}
		BlockPos bas = dev.pyrofetch.jn1.block.VendingMachineBlock.bas(pos, monde.getBlockState(pos));
		var machine = monde.getBlockEntity(bas) instanceof dev.pyrofetch.jn1.block.VendingMachineBlockEntity m ? m : null;
		var offres = dev.pyrofetch.jn1.block.VendingMachineBlockEntity.vitrine(machine, monde);
		String état = machine == null ? "sans entité" : machine.garnie() ? "garni" : "catalogue";
		ctx.getSource().sendSuccess(() -> Component.literal("vitrine " + état + ", " + offres.size() + " articles"), false);
		for (var offre : offres) {
			ctx.getSource().sendSuccess(() -> Component.literal("  " + offre.place() + " " + offre.id() + " "
					+ offre.stack().getCount() + "x" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offre.stack().getItem())
					+ " = " + offre.price()), false);
		}
		return offres.size();
	}

	private static int toggleUnsettled(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		CompanionEntity jenny = findNearestJenny(ctx.getSource().getPlayerOrException());
		if (jenny == null) {
			ctx.getSource().sendFailure(Component.translatable("command.jn1.no_jenny"));
			return 0;
		}
		jenny.setUnsettled(!jenny.isUnsettled());
		boolean now = jenny.isUnsettled();
		ctx.getSource().sendSuccess(() -> Component.translatable("command.jn1.unsettled", now), false);
		return 1;
	}

	@Nullable
	public static CompanionEntity findNearestJenny(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(64.0);
		List<CompanionEntity> found = player.level()
				.getEntitiesOfClass(CompanionEntity.class, box, entity -> true);
		return found.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElse(null);
	}
}
