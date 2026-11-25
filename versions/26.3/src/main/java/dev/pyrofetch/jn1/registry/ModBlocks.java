package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.BarricadeBlock;
import dev.pyrofetch.jn1.block.PaperNoteBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import dev.pyrofetch.jn1.block.CableBlock;
import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.block.LabBlock;
import dev.pyrofetch.jn1.block.LabDoorBlock;

public final class ModBlocks {
	public static final Block COMPUTER = register("computer", ComputerBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.5F)
					.requiresCorrectToolForDrops()
					.noOcclusion()
					.sound(SoundType.METAL)
					// L'écran reste allumé quand personne ne s'en sert.
					.lightLevel(state -> 7)
					.pushReaction(PushReaction.IMMOVEABLE));

	/** Paillasse : plan de travail et rangements. */
	public static final Block LAB_BENCH = register("lab_bench", LabBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.METAL));

	/** Baie de calcul : c'est elle qui fait tourner le terminal. */
	public static final Block SERVER_RACK = register("server_rack", LabBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
					// Les voyants restent allumés, sans éclairer la pièce.
					.lightLevel(state -> 5));

	/** Tube de stase : c'est là que sont gardées les capsules. */
	public static final Block CAPSULE_POD = register("capsule_pod", dev.pyrofetch.jn1.block.StasisPodBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresCorrectToolForDrops().noOcclusion()
					.sound(SoundType.GLASS).lightLevel(state -> 8));

	/**
	 * Conduit souple : il se raccorde tout seul, dans les six directions.
	 *
	 * <p>Aucune entité de bloc : les six côtés tiennent dans l'état, que le jeu
	 * stocke et synchronise déjà. Voir {@link CableBlock}.
	 */
	public static final Block CABLE = register("cable", CableBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected())
					.strength(2.0F).requiresCorrectToolForDrops().noOcclusion()
					.sound(SoundType.COPPER));

	/** Note : une feuille qu'on lit en s'en approchant. */
	public static final Block NOTE = register("note", PaperNoteBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
					.strength(0.2F).noOcclusion().noCollision()
					.sound(SoundType.WOOD)
					.pushReaction(PushReaction.POPPED));

	/** Barricade : des planches clouées contre la face qu'on vise. */
	public static final Block BARRICADE = register("barricade", BarricadeBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
					.strength(1.5F).noOcclusion()
					.sound(SoundType.WOOD)
					.pushReaction(PushReaction.POPPED));

	/** Conduit de flux : il relie l'appareillage au reste. */
	public static final Block FLUX_CONDUIT = register("flux_conduit", LabBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.COPPER)
					.lightLevel(state -> 3));

	/** La porte du laboratoire : on la traverse, on ne l'ouvre pas. */
	public static final Block LAB_DOOR = register("lab_door",
			p -> new LabDoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.COPPER, p),
					BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR)
							.strength(-1.0F)  // indestructible : c'est un passage, pas du mobilier
							.noOcclusion()
							.pushReaction(PushReaction.IMMOVEABLE));

	/** Le chemin du vide : trois teintes de violet, du cœur au bord. */
	public static final Block VOID_PATH = pierreDuVide("void_path");
	public static final Block VOID_PATH_EDGE = pierreDuVide("void_path_edge");
	public static final Block VOID_PATH_DARK = pierreDuVide("void_path_dark");
	/**
	 * La dalle d'arrivée, plus claire que tout le reste du chemin.
	 *
	 * <p>C'est sur elle qu'on pose le pied en entrant dans la dimension. Elle
	 * se distingue à l'œil pour qu'on retrouve le point de départ depuis le
	 * fond du chemin, sans avoir à compter ses pas.
	 */
	public static final Block VOID_PATH_PALE = pierreDuVide("void_path_pale");

	/**
	 * L'arbre du vide : sa frondaison, son cœur plus sombre, et son fût.
	 *
	 * <p>Le feuillage est ajouré, donc déclaré non opaque : sans cela le jeu
	 * cesse de dessiner les faces qu'il touche, et les trous de la planche
	 * laisseraient voir un vide noir au lieu du ciel.
	 */
	public static final Block VOID_LEAVES = feuillageDuVide("void_leaves");
	public static final Block VOID_LEAVES_DEEP = feuillageDuVide("void_leaves_deep");
	public static final Block VOID_LOG = register("void_log", net.minecraft.world.level.block.RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
					.strength(1.6F).sound(SoundType.STONE));

	/**
	 * Les néons, un par teinte.
	 *
	 * <p>Un seul modèle les dessine tous : la teinte est appliquée par le client
	 * (voir {@code JN1Client}), à partir de {@link #NEON_COLOURS}. Ajouter une
	 * couleur, c'est donc une ligne ici et une clé de langue, pas une texture.
	 */
	public static final java.util.Map<String, Integer> NEON_COLOURS;
	public static final java.util.Map<String, Block> NEONS;

	static {
		java.util.LinkedHashMap<String, Integer> teintes = new java.util.LinkedHashMap<>();
		teintes.put("white", 0xF4F4FF);
		teintes.put("red", 0xFF3B4A);
		teintes.put("amber", 0xFFB23C);
		teintes.put("green", 0x5CFF7A);
		teintes.put("cyan", 0x3CF0FF);
		teintes.put("violet", 0xA05CFF);
		teintes.put("magenta", 0xFF4FD8);
		NEON_COLOURS = java.util.Collections.unmodifiableMap(teintes);

		java.util.LinkedHashMap<String, Block> néons = new java.util.LinkedHashMap<>();
		for (String teinte : teintes.keySet()) {
			néons.put(teinte, luminaire("neon_" + teinte, 15, 0, 1, 7, 16, 3, 9));
		}
		NEONS = java.util.Collections.unmodifiableMap(néons);
	}

	/** Plafonnier : une dalle lumineuse, faite pour le plafond du laboratoire. */
	public static final Block CEILING_LIGHT = luminaire("ceiling_light", 15, 2, 0, 2, 14, 1.5, 14);

	/** Applique : une lampe grillagée, plus douce que le plafonnier. */
	public static final Block WALL_LAMP = luminaire("wall_lamp", 13, 5, 0, 5, 11, 7, 11);

	/** Le trépied : on y pose l'appareil photo, et l'on vise depuis lui. */
	public static final Block CAMERA_STAND = register("camera_stand", dev.pyrofetch.jn1.block.CameraStandBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(1.0F).noOcclusion()
					.sound(SoundType.METAL)
					.lightLevel(state -> 3)
					.pushReaction(PushReaction.POPPED));

	/** Le cadre photo : un tirage accroché au mur, sur un ou plusieurs blocs. */
	public static final Block PHOTO_FRAME = register("photo_frame", dev.pyrofetch.jn1.block.PhotoFrameBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
					.strength(0.2F).noOcclusion().noCollision()
					.sound(SoundType.WOOD)
					.pushReaction(PushReaction.POPPED));

	/** Le distributeur : deux blocs de haut, sa vitrine éclaire un peu. */
	public static final Block VENDING_MACHINE = register("vending_machine", dev.pyrofetch.jn1.block.VendingMachineBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresCorrectToolForDrops().noOcclusion()
					.sound(SoundType.METAL)
					.lightLevel(state -> 9)
					.pushReaction(PushReaction.IMMOVEABLE));

	/** L'écran mural : une dalle mince, qui luit faiblement même éteinte. */
	public static final Block WALL_SCREEN = register("wall_screen", dev.pyrofetch.jn1.block.WallScreenBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(1.5F).noOcclusion()
					.sound(SoundType.GLASS)
					.lightLevel(state -> 4)
					.pushReaction(PushReaction.IMMOVEABLE));

	/**
	 * L'ascenseur en deux blocs (0.83.0, voir {@link dev.pyrofetch.jn1.lift.Platforms}) :
	 * la plateforme, qui prend l'apparence de n'importe quel bloc et s'assemble à
	 * ses voisines ; le sélecteur d'étage, posé à chaque palier.
	 */
	public static final Block LIFT_PLATFORM = register("lift_platform", dev.pyrofetch.jn1.lift.PlatformBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)
					.pushReaction(PushReaction.IMMOVEABLE));

	public static final Block LIFT_SELECTOR = register("lift_selector", dev.pyrofetch.jn1.lift.SelectorBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
					.lightLevel(state -> state.getValue(dev.pyrofetch.jn1.lift.SelectorBlock.POWERED) ? 7 : 3));

	/**
	 * La brèche d'un site de confinement : on ne la referme pas, on ne la casse
	 * pas. Voir {@link dev.pyrofetch.jn1.rift.BreachBlock}.
	 */
	/**
	 * Le projecteur holographique : il fait paraître Aura, en veille. On le pose
	 * dans le bâtiment de la faille. Voir {@link dev.pyrofetch.jn1.rift.AuraProjectorBlock}.
	 */
	public static final Block AURA_PROJECTOR = register("aura_projector", dev.pyrofetch.jn1.rift.AuraProjectorBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F, 6.0F)
					.lightLevel(state -> 7)
					.noOcclusion()
					.sound(SoundType.METAL));

	/**
	 * Le confinement de la faille, en décor (0.83.8) : le stabilisateur, qui
	 * tire un faisceau vers elle ; le cadre, une poutre d'acier à bandes de
	 * danger qui s'oriente comme une bûche.
	 */
	public static final Block RIFT_STABILIZER = register("rift_stabilizer", dev.pyrofetch.jn1.rift.RiftStabilizerBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(4.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)
					.lightLevel(state -> 7));

	public static final Block CONTAINMENT_FRAME = register("containment_frame", net.minecraft.world.level.block.RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(5.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));

	public static final Block BREACH = register("breach", dev.pyrofetch.jn1.rift.BreachBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE)
					.strength(-1.0F, 3600000.0F)
					.noLootTable()
					.lightLevel(state -> 6)
					.sound(SoundType.DEEPSLATE)
					.pushReaction(PushReaction.IMMOVEABLE));

	/** La douche du sas : elle lave la contamination. */
	public static final Block DECON_SHOWER = register("decon_shower", dev.pyrofetch.jn1.block.DeconShowerBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(1.5F)
					.noOcclusion()
					.noCollision()
					.sound(SoundType.METAL)
					.pushReaction(PushReaction.POPPED));

	/** Une trace blanche sur une face de bloc : posée par la fiole, ou par une scène « cu ». */
	public static final Block CUM_TRACE = register("cum_trace", dev.pyrofetch.jn1.cum.CumTraceBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(net.minecraft.world.level.material.MapColor.SNOW)
					.replaceable()
					.noCollision()
					.instabreak()
					.noOcclusion()
					.sound(SoundType.HONEY_BLOCK)
					.pushReaction(PushReaction.POPPED));

	/** Un chaudron de cum, qui caille en fromage : pas d'objet, c'est un chaudron. */
	public static final Block CUM_CAULDRON = register("cum_cauldron", dev.pyrofetch.jn1.cum.CumCauldronBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));

	/** Le Cummy piston : un piston collant, au cum séché. Sa tête est à part, sans objet. */
	public static final Block CUMMY_PISTON = register("cummy_piston", dev.pyrofetch.jn1.cum.CummyPistonBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.STICKY_PISTON));
	public static final Block CUMMY_PISTON_HEAD = register("cummy_piston_head", dev.pyrofetch.jn1.cum.CummyPistonHeadBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.PISTON_HEAD));

	/** Le dildo : un end rod collé au Cummy piston. Il n'éclaire pas. */
	public static final Block DILDO = register("dildo", dev.pyrofetch.jn1.cum.DildoBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD)
					.lightLevel(state -> 0)
					.sound(SoundType.HONEY_BLOCK));

	/** Quatre dildos : celui de cheval. */
	public static final Block HORSE_DILDO = register("horse_dildo", p -> new dev.pyrofetch.jn1.cum.DildoBlock(p, 4.0),
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD)
					.lightLevel(state -> 0).sound(SoundType.HONEY_BLOCK));

	/** Quatre de cheval : celui d'éléphant, Excalibur. */
	public static final Block EXCALIBUR = register("excalibur", p -> new dev.pyrofetch.jn1.cum.DildoBlock(p, 5.0),
			BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD)
					.lightLevel(state -> 0).sound(SoundType.HONEY_BLOCK));

	/** La barre de pole dance : un tube d'acier qui s'empile. */
	public static final Block DANCE_POLE = register("dance_pole", dev.pyrofetch.jn1.jouets.DancePoleBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)
					.strength(2.0F)
					.noOcclusion()
					.sound(SoundType.METAL));

	/** Le bocal à cum : 69 fioles et une figurine, pour le compresseur. Son objet est à part (contenu). */
	public static final Block CUM_JAR = register("cum_jar", dev.pyrofetch.jn1.cum.CumJarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
					.strength(0.3F)
					.noOcclusion()
					.pushReaction(PushReaction.POPPED));

	/** Le compresseur : il écrase un bocal plein en Cum Crystal, la monnaie de Gaster. */
	public static final Block COMPRESSOR = register("compressor", dev.pyrofetch.jn1.cum.CompressorBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK)
					.strength(5.0F, 6.0F)
					.noOcclusion()
					.sound(SoundType.STONE)
					.lightLevel(dev.pyrofetch.jn1.cum.CompressorBlock::lumière)
					.pushReaction(PushReaction.IMMOVEABLE));

	/** La cloison à trou : un panneau mince de deux blocs de haut. */
	public static final Block GLORY_WALL = register("glory_wall", dev.pyrofetch.jn1.gloryhole.GloryWallBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE))
					.strength(1.5F)
					.noOcclusion()
					.sound(SoundType.WOOD)
					.pushReaction(PushReaction.POPPED));

	/**
	 * Un luminaire, décrit par son emprise posé au sol.
	 *
	 * <p>Éteint, il ne rend plus rien : on n'y voit que le verre.
	 */
	private static Block luminaire(String name, int lumière, double x1, double y1, double z1,
	                               double x2, double y2, double z2) {
		return register(name,
				p -> new dev.pyrofetch.jn1.block.LightFixtureBlock(p, x1, y1, z1, x2, y2, z2),
				BlockBehaviour.Properties.of()
						.strength(0.3F)
						.sound(SoundType.GLASS)
						.noOcclusion()
						.noCollision()
						.lightLevel(state -> dev.pyrofetch.jn1.block.LightFixtureBlock.luminosité(state, lumière))
						.pushReaction(PushReaction.POPPED));
	}

	/**
	 * Comme la pierre, mais ajouré : la lumière traverse et les faces voisines
	 * restent dessinées.
	 */
	private static Block feuillageDuVide(String name) {
		return register(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
				.strength(1.6F)
				.sound(SoundType.STONE)
				.noOcclusion()
				.isRedstoneConductor((state, view, pos) -> false)
				.isSuffocating((state, view, pos) -> false)
				.isViewBlocking((state, view, pos, zone) -> false));
	}

	/** Elles se comportent toutes comme de la pierre : seule la teinte change. */
	private static Block pierreDuVide(String name) {
		return register(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
				.strength(1.6F)
				.sound(SoundType.STONE));
	}

	private ModBlocks() {
	}

	/**
	 * Enregistre un bloc. Depuis 1.21.2, les propriétés doivent porter
	 * l'identifiant du bloc avant sa construction (description, butin) : on
	 * passe donc une fabrique, comme {@code Blocks.register} du jeu.
	 */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> fabrique,
	                              BlockBehaviour.Properties propriétés) {
		ResourceKey<Block> clé = ResourceKey.create(Registries.BLOCK, JN1.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, clé, fabrique.apply(propriétés.setId(clé)));
	}

	public static void register() {
		JN1.LOGGER.debug("Blocs enregistrés.");
	}

	public static Block[] all() {
		java.util.List<Block> blocs = new java.util.ArrayList<>(java.util.List.of(
				COMPUTER, LAB_BENCH, SERVER_RACK, CAPSULE_POD, FLUX_CONDUIT, CABLE, BARRICADE, NOTE,
				LAB_DOOR, VOID_PATH, VOID_PATH_EDGE, VOID_PATH_DARK, VOID_PATH_PALE,
				VOID_LEAVES, VOID_LEAVES_DEEP, VOID_LOG, PHOTO_FRAME, CAMERA_STAND));
		blocs.addAll(NEONS.values());
		blocs.add(CEILING_LIGHT);
		blocs.add(WALL_LAMP);
		blocs.add(VENDING_MACHINE);
		blocs.add(WALL_SCREEN);
		blocs.add(LIFT_PLATFORM);
		blocs.add(LIFT_SELECTOR);
		blocs.add(BREACH);
		blocs.add(RIFT_STABILIZER);
		blocs.add(CONTAINMENT_FRAME);
		blocs.add(AURA_PROJECTOR);
		blocs.add(DECON_SHOWER);
		blocs.add(GLORY_WALL);
		blocs.add(CUMMY_PISTON);
		blocs.add(DANCE_POLE);
		blocs.add(COMPRESSOR);
		return blocs.toArray(Block[]::new);
	}

	public static Identifier idOf(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}
}
