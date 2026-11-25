package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.BarricadeBlock;
import dev.pyrofetch.jn1.block.PaperNoteBlock;
import dev.pyrofetch.jn1.block.CableBlock;
import dev.pyrofetch.jn1.block.ComputerBlock;
import dev.pyrofetch.jn1.block.LabBlock;
import dev.pyrofetch.jn1.block.LabDoorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	public static final Block COMPUTER = register("computer",
			new ComputerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.5F)
					.requiresTool()
					.nonOpaque()
					.sounds(BlockSoundGroup.METAL)
					// L'écran reste allumé quand personne ne s'en sert.
					.luminance(state -> 7)
					.pistonBehavior(PistonBehavior.BLOCK)));

	/** Paillasse : plan de travail et rangements. */
	public static final Block LAB_BENCH = register("lab_bench",
			new LabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresTool().sounds(BlockSoundGroup.METAL)));

	/** Baie de calcul : c'est elle qui fait tourner le terminal. */
	public static final Block SERVER_RACK = register("server_rack",
			new LabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresTool().sounds(BlockSoundGroup.METAL)
					// Les voyants restent allumés, sans éclairer la pièce.
					.luminance(state -> 5)));

	/** Tube de stase : c'est là que sont gardées les capsules. */
	public static final Block CAPSULE_POD = register("capsule_pod",
			new dev.pyrofetch.jn1.block.StasisPodBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresTool().nonOpaque()
					.sounds(BlockSoundGroup.GLASS).luminance(state -> 8)));

	/**
	 * Conduit souple : il se raccorde tout seul, dans les six directions.
	 *
	 * <p>Aucune entité de bloc : les six côtés tiennent dans l'état, que le jeu
	 * stocke et synchronise déjà. Voir {@link CableBlock}.
	 */
	public static final Block CABLE = register("cable",
			new CableBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)
					.strength(2.0F).requiresTool().nonOpaque()
					.sounds(BlockSoundGroup.COPPER)));

	/** Note : une feuille qu'on lit en s'en approchant. */
	public static final Block NOTE = register("note",
			new PaperNoteBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
					.strength(0.2F).nonOpaque().noCollision()
					.sounds(BlockSoundGroup.WOOD)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Barricade : des planches clouées contre la face qu'on vise. */
	public static final Block BARRICADE = register("barricade",
			new BarricadeBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
					.strength(1.5F).nonOpaque()
					.sounds(BlockSoundGroup.WOOD)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Conduit de flux : il relie l'appareillage au reste. */
	public static final Block FLUX_CONDUIT = register("flux_conduit",
			new LabBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(2.5F).requiresTool().sounds(BlockSoundGroup.COPPER)
					.luminance(state -> 3)));

	/** La porte du laboratoire : on la traverse, on ne l'ouvre pas. */
	public static final Block LAB_DOOR = register("lab_door",
			new LabDoorBlock(net.minecraft.block.BlockSetType.COPPER,
					AbstractBlock.Settings.copy(Blocks.IRON_DOOR)
							.strength(-1.0F)  // indestructible : c'est un passage, pas du mobilier
							.nonOpaque()
							.pistonBehavior(PistonBehavior.BLOCK)));

	/** Le chemin du vide : trois teintes de violet, du cœur au bord. */
	public static final Block VOID_PATH = register("void_path", pierreDuVide());
	public static final Block VOID_PATH_EDGE = register("void_path_edge", pierreDuVide());
	public static final Block VOID_PATH_DARK = register("void_path_dark", pierreDuVide());
	/**
	 * La dalle d'arrivée, plus claire que tout le reste du chemin.
	 *
	 * <p>C'est sur elle qu'on pose le pied en entrant dans la dimension. Elle
	 * se distingue à l'œil pour qu'on retrouve le point de départ depuis le
	 * fond du chemin, sans avoir à compter ses pas.
	 */
	public static final Block VOID_PATH_PALE = register("void_path_pale", pierreDuVide());

	/**
	 * L'arbre du vide : sa frondaison, son cœur plus sombre, et son fût.
	 *
	 * <p>Le feuillage est ajouré, donc déclaré non opaque : sans cela le jeu
	 * cesse de dessiner les faces qu'il touche, et les trous de la planche
	 * laisseraient voir un vide noir au lieu du ciel.
	 */
	public static final Block VOID_LEAVES = register("void_leaves", feuillageDuVide());
	public static final Block VOID_LEAVES_DEEP = register("void_leaves_deep", feuillageDuVide());
	public static final Block VOID_LOG = register("void_log",
			new net.minecraft.block.PillarBlock(AbstractBlock.Settings.copy(Blocks.STONE)
					.strength(1.6F).sounds(BlockSoundGroup.STONE)));

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
			néons.put(teinte, register("neon_" + teinte, luminaire(15, 0, 1, 7, 16, 3, 9)));
		}
		NEONS = java.util.Collections.unmodifiableMap(néons);
	}

	/** Plafonnier : une dalle lumineuse, faite pour le plafond du laboratoire. */
	public static final Block CEILING_LIGHT = register("ceiling_light", luminaire(15, 2, 0, 2, 14, 1.5, 14));

	/** Applique : une lampe grillagée, plus douce que le plafonnier. */
	public static final Block WALL_LAMP = register("wall_lamp", luminaire(13, 5, 0, 5, 11, 7, 11));

	/** Le trépied : on y pose l'appareil photo, et l'on vise depuis lui. */
	public static final Block CAMERA_STAND = register("camera_stand",
			new dev.pyrofetch.jn1.block.CameraStandBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(1.0F).nonOpaque()
					.sounds(BlockSoundGroup.METAL)
					.luminance(state -> 3)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Le cadre photo : un tirage accroché au mur, sur un ou plusieurs blocs. */
	public static final Block PHOTO_FRAME = register("photo_frame",
			new dev.pyrofetch.jn1.block.PhotoFrameBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
					.strength(0.2F).nonOpaque().noCollision()
					.sounds(BlockSoundGroup.WOOD)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Le distributeur : deux blocs de haut, sa vitrine éclaire un peu. */
	public static final Block VENDING_MACHINE = register("vending_machine",
			new dev.pyrofetch.jn1.block.VendingMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresTool().nonOpaque()
					.sounds(BlockSoundGroup.METAL)
					.luminance(state -> 9)
					.pistonBehavior(PistonBehavior.BLOCK)));

	/** L'écran mural : une dalle mince, qui luit faiblement même éteinte. */
	public static final Block WALL_SCREEN = register("wall_screen",
			new dev.pyrofetch.jn1.block.WallScreenBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(1.5F).nonOpaque()
					.sounds(BlockSoundGroup.GLASS)
					.luminance(state -> 4)
					.pistonBehavior(PistonBehavior.BLOCK)));

	/**
	 * L'ascenseur en deux blocs (0.83.0, voir {@link dev.pyrofetch.jn1.lift.Platforms}) :
	 * la plateforme, qui prend l'apparence de n'importe quel bloc et s'assemble à
	 * ses voisines ; le sélecteur d'étage, posé à chaque palier.
	 */
	public static final Block LIFT_PLATFORM = register("lift_platform",
			new dev.pyrofetch.jn1.lift.PlatformBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresTool().nonOpaque().sounds(BlockSoundGroup.METAL)
					.pistonBehavior(PistonBehavior.BLOCK)));

	public static final Block LIFT_SELECTOR = register("lift_selector",
			new dev.pyrofetch.jn1.lift.SelectorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.0F).requiresTool().sounds(BlockSoundGroup.METAL)
					.luminance(state -> state.get(dev.pyrofetch.jn1.lift.SelectorBlock.POWERED) ? 7 : 3)));

	/**
	 * La brèche d'un site de confinement : on ne la referme pas, on ne la casse
	 * pas. Voir {@link dev.pyrofetch.jn1.rift.BreachBlock}.
	 */
	/**
	 * Le projecteur holographique : il fait paraître Aura, en veille. On le pose
	 * dans le bâtiment de la faille. Voir {@link dev.pyrofetch.jn1.rift.AuraProjectorBlock}.
	 */
	public static final Block AURA_PROJECTOR = register("aura_projector",
			new dev.pyrofetch.jn1.rift.AuraProjectorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(3.0F, 6.0F)
					.luminance(state -> 7)
					.nonOpaque()
					.sounds(BlockSoundGroup.METAL)));

	/**
	 * Le confinement de la faille, en décor (0.83.8) : le stabilisateur, qui
	 * tire un faisceau vers elle ; le cadre, une poutre d'acier à bandes de
	 * danger qui s'oriente comme une bûche.
	 */
	public static final Block RIFT_STABILIZER = register("rift_stabilizer",
			new dev.pyrofetch.jn1.rift.RiftStabilizerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(4.0F).requiresTool().nonOpaque().sounds(BlockSoundGroup.METAL)
					.luminance(state -> 7)));

	public static final Block CONTAINMENT_FRAME = register("containment_frame",
			new net.minecraft.block.PillarBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(5.0F).requiresTool().sounds(BlockSoundGroup.METAL)));

	public static final Block BREACH = register("breach",
			new dev.pyrofetch.jn1.rift.BreachBlock(AbstractBlock.Settings.copy(Blocks.REINFORCED_DEEPSLATE)
					.strength(-1.0F, 3600000.0F)
					.dropsNothing()
					.luminance(state -> 6)
					.sounds(BlockSoundGroup.DEEPSLATE)
					.pistonBehavior(PistonBehavior.BLOCK)));

	/** La douche du sas : elle lave la contamination. */
	public static final Block DECON_SHOWER = register("decon_shower",
			new dev.pyrofetch.jn1.block.DeconShowerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
					.strength(1.5F)
					.nonOpaque()
					.noCollision()
					.sounds(BlockSoundGroup.METAL)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Une trace blanche sur une face de bloc : posée par la fiole, ou par une scène « cu ». */
	public static final Block CUM_TRACE = register("cum_trace",
			new dev.pyrofetch.jn1.cum.CumTraceBlock(AbstractBlock.Settings.create()
					.mapColor(net.minecraft.block.MapColor.WHITE)
					.replaceable()
					.noCollision()
					.breakInstantly()
					.nonOpaque()
					.sounds(BlockSoundGroup.HONEY)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Un chaudron de cum, qui caille en fromage : pas d'objet, c'est un chaudron. */
	public static final Block CUM_CAULDRON = register("cum_cauldron",
			new dev.pyrofetch.jn1.cum.CumCauldronBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON)));

	/** Le Cummy piston : un piston collant, au cum séché. Sa tête est à part, sans objet. */
	public static final Block CUMMY_PISTON = register("cummy_piston",
			new dev.pyrofetch.jn1.cum.CummyPistonBlock(AbstractBlock.Settings.copy(Blocks.STICKY_PISTON)));
	public static final Block CUMMY_PISTON_HEAD = register("cummy_piston_head",
			new dev.pyrofetch.jn1.cum.CummyPistonHeadBlock(AbstractBlock.Settings.copy(Blocks.PISTON_HEAD)));

	/** Le dildo : un end rod collé au Cummy piston. Il n'éclaire pas. */
	public static final Block DILDO = register("dildo",
			new dev.pyrofetch.jn1.cum.DildoBlock(AbstractBlock.Settings.copy(Blocks.END_ROD)
					.luminance(state -> 0)
					.sounds(BlockSoundGroup.HONEY)));

	/** Quatre dildos : celui de cheval. */
	public static final Block HORSE_DILDO = register("horse_dildo",
			new dev.pyrofetch.jn1.cum.DildoBlock(AbstractBlock.Settings.copy(Blocks.END_ROD)
					.luminance(state -> 0).sounds(BlockSoundGroup.HONEY), 4.0));

	/** Quatre de cheval : celui d'éléphant, Excalibur. */
	public static final Block EXCALIBUR = register("excalibur",
			new dev.pyrofetch.jn1.cum.DildoBlock(AbstractBlock.Settings.copy(Blocks.END_ROD)
					.luminance(state -> 0).sounds(BlockSoundGroup.HONEY), 5.0));

	/** La barre de pole dance : un tube d'acier qui s'empile. */
	public static final Block DANCE_POLE = register("dance_pole",
			new dev.pyrofetch.jn1.jouets.DancePoleBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS)
					.strength(2.0F)
					.nonOpaque()
					.sounds(BlockSoundGroup.METAL)));

	/** Le bocal à cum : 69 fioles et une figurine, pour le compresseur. Son objet est à part (contenu). */
	public static final Block CUM_JAR = register("cum_jar",
			new dev.pyrofetch.jn1.cum.CumJarBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
					.strength(0.3F)
					.nonOpaque()
					.pistonBehavior(PistonBehavior.DESTROY)));

	/** Le compresseur : il écrase un bocal plein en Cum Crystal, la monnaie de Gaster. */
	public static final Block COMPRESSOR = register("compressor",
			new dev.pyrofetch.jn1.cum.CompressorBlock(AbstractBlock.Settings.copy(Blocks.QUARTZ_BLOCK)
					.strength(5.0F, 6.0F)
					.nonOpaque()
					.sounds(BlockSoundGroup.STONE)
					.luminance(dev.pyrofetch.jn1.cum.CompressorBlock::lumière)
					.pistonBehavior(PistonBehavior.BLOCK)));

	/** La cloison à trou : un panneau mince de deux blocs de haut. */
	public static final Block GLORY_WALL = register("glory_wall",
			new dev.pyrofetch.jn1.gloryhole.GloryWallBlock(AbstractBlock.Settings.copy(Blocks.WHITE_CONCRETE)
					.strength(1.5F)
					.nonOpaque()
					.sounds(BlockSoundGroup.WOOD)
					.pistonBehavior(PistonBehavior.DESTROY)));

	/**
	 * Un luminaire, décrit par son emprise posé au sol.
	 *
	 * <p>Éteint, il ne rend plus rien : on n'y voit que le verre.
	 */
	private static Block luminaire(int lumière, double x1, double y1, double z1,
	                               double x2, double y2, double z2) {
		return new dev.pyrofetch.jn1.block.LightFixtureBlock(AbstractBlock.Settings.create()
				.strength(0.3F)
				.sounds(BlockSoundGroup.GLASS)
				.nonOpaque()
				.noCollision()
				.luminance(state -> dev.pyrofetch.jn1.block.LightFixtureBlock.luminosité(state, lumière))
				.pistonBehavior(PistonBehavior.DESTROY),
				x1, y1, z1, x2, y2, z2);
	}

	/**
	 * Comme la pierre, mais ajouré : la lumière traverse et les faces voisines
	 * restent dessinées.
	 */
	private static Block feuillageDuVide() {
		return new Block(AbstractBlock.Settings.copy(Blocks.STONE)
				.strength(1.6F)
				.sounds(BlockSoundGroup.STONE)
				.nonOpaque()
				.solidBlock((state, view, pos) -> false)
				.suffocates((state, view, pos) -> false)
				.blockVision((state, view, pos) -> false));
	}

	/** Elles se comportent toutes comme de la pierre : seule la teinte change. */
	private static Block pierreDuVide() {
		return new Block(AbstractBlock.Settings.copy(Blocks.STONE)
				.strength(1.6F)
				.sounds(BlockSoundGroup.STONE));
	}

	private ModBlocks() {
	}

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, JN1.id(name), block);
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
		return Registries.BLOCK.getId(block);
	}
}
