package dev.pyrofetch.jn1.lab;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.LabDoorBlock;
import dev.pyrofetch.jn1.mixin.SinglePoolElementAccessor;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.structure.StructureSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Les laboratoires du vide : un par maison, à l'aplomb de la porte de sa cave.
 *
 * <p>Le vide est un parallèle de la surface. Ce qui s'y trouve est aux mêmes x
 * et z que ce qui lui répond en haut : on passe la porte de la cave en (x, z),
 * on sort de celle du chemin en (x, z) ; on reprend celle-ci, on se retrouve
 * dans la cave. Chaque maison a donc son laboratoire, et non plus toutes la
 * même salle.
 *
 * <p>Rien n'est généré d'avance. Le vide n'a pas de terrain où poser une
 * structure, et c'est une maison d'un autre monde qui décide de l'emplacement.
 * Un laboratoire est bâti quand on en a besoin — en passant la porte de sa
 * maison, ou en s'en approchant à la nage — et l'on retient lesquels le sont
 * déjà ({@link VoidLabState}).
 *
 * <p>Où ils seront se calcule sans rien bâtir. Les maisons suivent le placement
 * de leur jeu de structures, qui ne propose qu'un chunk par région ; pousser ce
 * chunk jusqu'aux départs de structures — ce que fait {@code /locate} — dit s'il
 * porte une maison et comment elle est tournée, sans générer de terrain. C'est
 * ce que lit le lidar, y compris sous des régions que personne n'a vues.
 */
public final class VoidLabs {
	/** L'étage des laboratoires : celui du tout premier, à y = 40. */
	private static final int SOL = 40;
	/**
	 * La porte, dans le gabarit du chemin. C'est elle qu'on aligne sur la porte
	 * de la maison : tout le reste s'en déduit.
	 */
	private static final int PORTE_X = 41;
	private static final int PORTE_Z = 10;
	/** Emprises des gabarits bâtis en jeu, relevées par tools/import_void.py. */
	private static final int PATH_LARGEUR = 44;
	private static final int LAB_LARGEUR = 20;
	private static final int TREE_LARGEUR = 19;
	private static final int PROFONDEUR = 19;

	/**
	 * On arrive face au laboratoire, de l'autre côté de la porte de retour, comme
	 * si on venait de la franchir (0.81.5). Jusque-là, on arrivait du
	 * côté du chemin, tourné vers l'arbre.
	 */
	public static final float REGARD = Direction.EAST.asRotation();

	/** À cette distance d'un nageur, un laboratoire se bâtit. Sa lumière se voit de loin. */
	private static final int PORTÉE_DE_CONSTRUCTION = 160;
	private static final int PÉRIODE = 100;

	private static final Identifier JEU_DES_MAISONS = JN1.id("houses");

	/**
	 * Ce qu'on sait déjà de chaque région : les ancres de son chunk candidat.
	 *
	 * <p>Tout y est déterminé par la graine, rien ne change en cours de partie :
	 * on ne le calcule qu'une fois par lancement.
	 */
	private static final Map<Long, List<BlockPos>> RÉGIONS = new HashMap<>();

	private VoidLabs() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(VoidLabs::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RÉGIONS.clear());
	}

	// --- L'ancre : la porte, ramenée à un seul point -------------------------

	/**
	 * Le point qui représente une porte : le milieu de ses battants, au plus bas.
	 *
	 * <p>La porte de la cave est double. Sans ce point commun, passer par le
	 * battant de gauche ou par celui de droite mènerait à deux laboratoires
	 * décalés d'un bloc. Seuls x et z comptent ; y est celui de la porte.
	 *
	 * <p>Ce qui n'est pas une porte — un joueur envoyé par commande — est sa
	 * propre ancre.
	 */
	public static BlockPos ancre(World monde, BlockPos pos) {
		BlockPos bas = pos;
		BlockState état = monde.getBlockState(pos);
		if (état.getBlock() instanceof LabDoorBlock && état.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			bas = pos.down();
		}
		if (!battantBas(monde.getBlockState(bas))) {
			return pos;
		}
		// Les battants voisins, de proche en proche sur la même couche.
		Set<BlockPos> battants = new HashSet<>();
		Deque<BlockPos> àVoir = new ArrayDeque<>();
		àVoir.add(bas);
		while (!àVoir.isEmpty() && battants.size() < 8) {
			BlockPos courant = àVoir.poll();
			if (!battants.add(courant)) {
				continue;
			}
			for (Direction côté : Direction.Type.HORIZONTAL) {
				BlockPos voisin = courant.offset(côté);
				if (!battants.contains(voisin) && battantBas(monde.getBlockState(voisin))) {
					àVoir.add(voisin);
				}
			}
		}
		return milieu(battants);
	}

	private static boolean battantBas(BlockState état) {
		return état.getBlock() instanceof LabDoorBlock && état.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
	}

	/** Arrondi vers le bas, des deux côtés : les deux calculs doivent tomber pareil. */
	private static BlockPos milieu(Iterable<BlockPos> positions) {
		long x = 0;
		long z = 0;
		int n = 0;
		int y = 0;
		for (BlockPos pos : positions) {
			x += pos.getX();
			z += pos.getZ();
			y = pos.getY();
			n++;
		}
		return new BlockPos((int) Math.floorDiv(x, n), y, (int) Math.floorDiv(z, n));
	}

	/** Les battants d'une liste, regroupés en portes, et chaque porte en son ancre. */
	private static List<BlockPos> regrouper(List<BlockPos> battants) {
		List<BlockPos> ancres = new ArrayList<>();
		Set<BlockPos> restants = new HashSet<>(battants);
		for (BlockPos départ : battants) {
			if (!restants.remove(départ)) {
				continue;
			}
			List<BlockPos> porte = new ArrayList<>();
			Deque<BlockPos> àVoir = new ArrayDeque<>();
			àVoir.add(départ);
			while (!àVoir.isEmpty()) {
				BlockPos courant = àVoir.poll();
				porte.add(courant);
				for (Direction côté : Direction.Type.HORIZONTAL) {
					BlockPos voisin = courant.offset(côté);
					if (restants.remove(voisin)) {
						àVoir.add(voisin);
					}
				}
			}
			ancres.add(milieu(porte));
		}
		return ancres;
	}

	// --- Dans le vide -----------------------------------------------------------

	/** La porte du chemin, pour cette ancre. */
	public static BlockPos porteDuVide(BlockPos ancre) {
		return new BlockPos(ancre.getX(), SOL + 1, ancre.getZ());
	}

	/**
	 * Où l'on pose le pied en arrivant : un pas derrière la porte, côté
	 * laboratoire (son entrée commence deux blocs plus loin, `PATH_LARGEUR`).
	 * Pas sur la porte même : on repartirait aussitôt.
	 */
	public static Vec3d arrivée(BlockPos ancre) {
		return Vec3d.ofBottomCenter(new BlockPos(ancre.getX() + 1, SOL + 1, ancre.getZ()));
	}

	/**
	 * Où Gaster attend : derrière l'arbre, d'où il épie le chemin.
	 *
	 * <p>Dans le gabarit de l'arbre, le tronc tient x 7 à 9, z 9 à 10 au pied,
	 * et une racine en sort en (8, 11) sur trois de haut ; on arrive par l'est
	 * (x 18). Il se tient dans le coin que la racine fait avec le tronc, caché
	 * du chemin, et se penche hors de l'arête sud-est de la racine, les doigts
	 * sur sa face est. Sa pose est calculée pour cette place précise
	 * ({@code tools/gen_gaster.py}, {@code ÉPIE}) : les deux doivent dire la même.
	 */
	public static Vec3d derrièreLArbre(BlockPos ancre) {
		BlockPos chemin = new BlockPos(ancre.getX() - PORTE_X, SOL, ancre.getZ() - PORTE_Z);
		BlockPos arbre = chemin.add(-TREE_LARGEUR, 0, 0);
		return new Vec3d(arbre.getX() + 7.6, SOL + 1, arbre.getZ() + 11.5);
	}

	/**
	 * Le milieu du tronc de l'arbre, au niveau du sol : x 7 à 9, z 8 à 10 du
	 * gabarit, sur trois de haut au-dessus du chemin. Gaster s'y cache derrière
	 * quand on approche.
	 */
	public static Vec3d centreDuTronc(BlockPos ancre) {
		BlockPos chemin = new BlockPos(ancre.getX() - PORTE_X, SOL, ancre.getZ() - PORTE_Z);
		BlockPos arbre = chemin.add(-TREE_LARGEUR, 0, 0);
		return new Vec3d(arbre.getX() + 8.5, SOL + 1, arbre.getZ() + 9.5);
	}

	/** Bâtit le laboratoire de cette ancre, s'il ne l'est pas déjà. */
	public static void bâtir(ServerWorld vide, BlockPos ancre) {
		VoidLabState état = VoidLabState.of(vide);
		if (état.bâti(ancre)) {
			return;
		}
		BlockPos chemin = new BlockPos(ancre.getX() - PORTE_X, SOL, ancre.getZ() - PORTE_Z);
		// Le laboratoire se colle derrière la porte, l'arbre à l'autre bout du
		// chemin, dans la direction du regard.
		BlockPos labo = chemin.add(PATH_LARGEUR, 0, 0);
		BlockPos arbre = chemin.add(-TREE_LARGEUR, 0, 0);

		// Poser une structure n'entraîne pas le chargement du terrain : sans
		// cela, la salle tombe dans des chunks absents et rien n'est écrit.
		ChunkPos début = new ChunkPos(arbre.add(-1, 0, -1));
		ChunkPos fin = new ChunkPos(labo.add(LAB_LARGEUR + 1, 0, PROFONDEUR + 1));
		for (int x = début.x; x <= fin.x; x++) {
			for (int z = début.z; z <= fin.z; z++) {
				vide.getChunk(x, z);
			}
		}
		poser(vide, "void_structure_lab", labo);
		poser(vide, "void_structure_path", chemin);
		poser(vide, "void_structure_tree", arbre);
		état.marquer(ancre);
		JN1.LOGGER.info("Laboratoire du vide bâti sous la porte ({}, {}).", ancre.getX(), ancre.getZ());

	}

	private static void poser(ServerWorld vide, String nom, BlockPos origine) {
		Optional<StructureTemplate> gabarit = vide.getStructureTemplateManager().getTemplate(JN1.id(nom));
		if (gabarit.isEmpty()) {
			JN1.LOGGER.error("Gabarit « {} » introuvable.", nom);
			return;
		}
		gabarit.get().place(vide, origine, origine, new StructurePlacementData(), Random.create(0L), 2);
	}

	/** Bâtit, pour chaque nageur, le laboratoire vers lequel il s'approche. */
	private static void tick(MinecraftServer server) {
		if (server.getTicks() % PÉRIODE != 0) {
			return;
		}
		ServerWorld vide = server.getWorld(LabDimension.LAB);
		if (vide == null || vide.getPlayers().isEmpty()) {
			return;
		}
		VoidLabState état = VoidLabState.of(vide);
		for (ServerPlayerEntity joueur : vide.getPlayers()) {
			for (BlockPos ancre : repérer(server, joueur.getBlockPos(), PORTÉE_DE_CONSTRUCTION)) {
				if (!état.bâti(ancre)) {
					// Un seul par passage : bâtir charge des chunks.
					bâtir(vide, ancre);
					return;
				}
			}
		}
	}

	// --- Depuis la surface -------------------------------------------------------

	/**
	 * Les ancres des laboratoires dans ce rayon, du plus proche au plus loin.
	 *
	 * <p>Ce sont celles des maisons de la surface, qu'elles soient déjà sorties de
	 * terre ou non. Seuls x et z comptent.
	 */
	public static List<BlockPos> repérer(MinecraftServer server, BlockPos centre, int rayon) {
		ServerWorld surface = server.getOverworld();
		StructureSet jeu = server.getRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(JEU_DES_MAISONS);
		if (jeu == null || !(jeu.placement() instanceof RandomSpreadStructurePlacement placement)) {
			JN1.LOGGER.error("Jeu de structures « {} » introuvable : aucun laboratoire à repérer.", JEU_DES_MAISONS);
			return List.of();
		}
		long graine = surface.getChunkManager().getStructurePlacementCalculator().getStructureSeed();
		int espacement = placement.getSpacing();

		int deX = Math.floorDiv((centre.getX() - rayon) >> 4, espacement);
		int àX = Math.floorDiv((centre.getX() + rayon) >> 4, espacement);
		int deZ = Math.floorDiv((centre.getZ() - rayon) >> 4, espacement);
		int àZ = Math.floorDiv((centre.getZ() + rayon) >> 4, espacement);
		long rayonCarré = (long) rayon * rayon;

		List<BlockPos> trouvées = new ArrayList<>();
		for (int rx = deX; rx <= àX; rx++) {
			for (int rz = deZ; rz <= àZ; rz++) {
				ChunkPos candidat = placement.getStartChunk(graine, rx * espacement, rz * espacement);
				for (BlockPos ancre : RÉGIONS.computeIfAbsent(candidat.toLong(),
						clé -> lireAncres(surface, jeu, candidat))) {
					if (distanceCarrée(ancre, centre) <= rayonCarré) {
						trouvées.add(ancre);
					}
				}
			}
		}
		trouvées.sort(Comparator.comparingLong(ancre -> distanceCarrée(ancre, centre)));
		return trouvées;
	}

	private static long distanceCarrée(BlockPos a, BlockPos b) {
		long dx = a.getX() - b.getX();
		long dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/**
	 * Les portes de cave que portera ce chunk candidat.
	 *
	 * <p>Le chunk n'est poussé que jusqu'aux départs de structures : on sait alors
	 * quelle maison s'y tient, et comment elle est tournée, sans que le terrain
	 * existe. Les portes se lisent dans son gabarit, placées comme le jeu les
	 * placera.
	 */
	private static List<BlockPos> lireAncres(ServerWorld surface, StructureSet jeu, ChunkPos candidat) {
		Set<Structure> maisons = new HashSet<>();
		for (StructureSet.WeightedEntry entrée : jeu.structures()) {
			maisons.add(entrée.structure().value());
		}
		Chunk chunk = surface.getChunk(candidat.x, candidat.z, ChunkStatus.STRUCTURE_STARTS);
		List<BlockPos> ancres = new ArrayList<>();
		for (Map.Entry<Structure, StructureStart> départ : chunk.getStructureStarts().entrySet()) {
			if (!maisons.contains(départ.getKey()) || !départ.getValue().hasChildren()) {
				continue;
			}
			for (StructurePiece pièce : départ.getValue().getChildren()) {
				if (!(pièce instanceof PoolStructurePiece pool)
						|| !(pool.getPoolElement() instanceof SinglePoolElement élément)) {
					continue;
				}
				Optional<Identifier> nom = ((SinglePoolElementAccessor) élément).jn1$location().left();
				Optional<StructureTemplate> gabarit = nom.flatMap(
						id -> surface.getStructureTemplateManager().getTemplate(id));
				if (gabarit.isEmpty()) {
					continue;
				}
				StructurePlacementData pose = new StructurePlacementData().setRotation(pool.getRotation());
				List<BlockPos> battants = gabarit.get()
						.getInfosForBlock(pool.getPos(), pose, ModBlocks.LAB_DOOR).stream()
						.filter(info -> info.state().get(DoorBlock.HALF) == DoubleBlockHalf.LOWER)
						.map(StructureTemplate.StructureBlockInfo::pos)
						.toList();
				ancres.addAll(regrouper(battants));
			}
		}
		return List.copyOf(ancres);
	}

	/**
	 * La porte de cave qui répond à cette ancre, à la surface.
	 *
	 * <p>On la cherche dans les colonnes de l'ancre et de ses voisines — une
	 * porte double déborde d'un bloc —, sur toute la hauteur du monde, puisque la
	 * cave est à la hauteur du terrain. On retient celle dont l'ancre est la même.
	 */
	public static Optional<BlockPos> porteDeLaMaison(ServerWorld surface, BlockPos ancre) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int x = ancre.getX() + dx;
				int z = ancre.getZ() + dz;
				for (int y = surface.getBottomY(); y < surface.getTopY(); y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!battantBas(surface.getBlockState(pos))) {
						continue;
					}
					BlockPos sienne = ancre(surface, pos);
					if (sienne.getX() == ancre.getX() && sienne.getZ() == ancre.getZ()) {
						return Optional.of(pos);
					}
				}
			}
		}
		return Optional.empty();
	}
}
