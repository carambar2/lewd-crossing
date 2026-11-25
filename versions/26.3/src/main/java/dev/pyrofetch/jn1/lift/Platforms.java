package dev.pyrofetch.jn1.lift;

import dev.pyrofetch.jn1.registry.ModEntities;
import dev.pyrofetch.jn1.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * L'ascenseur en deux blocs (0.83.0) : la plateforme ({@link PlatformBlock}), qu'on pose, qu'on
 * habille de n'importe quel bloc et qu'on assemble à ses voisines ; le
 * sélecteur d'étage ({@link SelectorBlock}), posé à chaque étage.
 *
 * <p><b>La connexion</b> : rien à relier. Une plateforme ne se déplace qu'à la
 * verticale ; un sélecteur la dessert s'il se tient dans sa colonne, à deux
 * blocs au plus de son emprise (à l'horizontale), et à 128 blocs au plus (à la
 * verticale). Les étages d'une plateforme sont donc tous les sélecteurs de sa
 * colonne. Et le sélecteur parle redstone : il émet un signal quand la
 * plateforme est à son étage (une porte de fer s'ouvre), et un signal reçu
 * l'appelle (un bouton, une plaque de pression font un appel).
 */
public final class Platforms {
	/** Combien de blocs une plateforme assemble au plus. */
	public static final int TAILLE_MAX = 100;
	/** L'écart horizontal entre un sélecteur et l'emprise de la plateforme qu'il dessert. */
	public static final int PORTÉE = 2;
	/** L'écart vertical au-delà duquel on ne cherche plus. */
	public static final int HAUTEUR = 128;

	private Platforms() {
	}

	/**
	 * Une plateforme, arrêtée (des blocs) ou en route (une entité).
	 *
	 * @param clé     son identité : son groupe, ou la position d'un bloc seul
	 * @param blocs   ses blocs, arrêtée ; vide en route
	 * @param entité  l'entité qui la porte, en route
	 * @param niveau  la couche de ses blocs (en route : celle qu'elle vise)
	 */
	public record Plateforme(Object clé, List<BlockPos> blocs, @Nullable PlatformEntity entité, int niveau,
	                         int x0, int z0, int x1, int z1) {
		public boolean enRoute() {
			return this.entité != null;
		}

		/** L'emprise élargie de {@link #PORTÉE} contient-elle cette colonne ? */
		public boolean dessert(BlockPos sélecteur) {
			return sélecteur.getX() >= this.x0 - PORTÉE && sélecteur.getX() <= this.x1 + PORTÉE
					&& sélecteur.getZ() >= this.z0 - PORTÉE && sélecteur.getZ() <= this.z1 + PORTÉE
					&& Math.abs(sélecteur.getY() - this.niveau) <= HAUTEUR;
		}

		public int taille() {
			return this.entité != null ? this.entité.nombreDeBlocs() : this.blocs.size();
		}
	}

	/** Un étage tel que le sélecteur l'affiche. */
	public record Étage(int niveau, Optional<Component> nom, BlockPos sélecteur) {
	}

	// --- Groupes ------------------------------------------------------------

	private static boolean estPlateforme(Level monde, BlockPos pos) {
		return monde.getBlockState(pos).getBlock() instanceof PlatformBlock;
	}

	@Nullable
	private static UUID groupeDe(Level monde, BlockPos pos) {
		return monde.getBlockEntity(pos) instanceof PlatformBlockEntity be ? be.groupe() : null;
	}

	/** Les blocs de plateforme reliés à celui-ci à l'horizontale ; {@code même} : du même groupe seulement. */
	private static List<BlockPos> parcourir(Level monde, BlockPos départ, boolean même) {
		UUID groupe = groupeDe(monde, départ);
		List<BlockPos> trouvés = new ArrayList<>();
		if (même && groupe == null) {
			trouvés.add(départ);
			return trouvés;
		}
		Set<BlockPos> vus = new HashSet<>();
		ArrayDeque<BlockPos> file = new ArrayDeque<>();
		file.add(départ);
		vus.add(départ);
		while (!file.isEmpty() && trouvés.size() < TAILLE_MAX) {
			BlockPos p = file.poll();
			trouvés.add(p);
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos q = p.relative(d);
				if (vus.add(q) && estPlateforme(monde, q) && (!même || groupe.equals(groupeDe(monde, q)))) {
					file.add(q);
				}
			}
		}
		return trouvés;
	}

	/** La plateforme assemblée dont fait partie ce bloc (lui seul s'il n'est pas assemblé). */
	public static List<BlockPos> groupe(Level monde, BlockPos pos) {
		return parcourir(monde, pos, true);
	}

	/** Tous les blocs de plateforme voisins, assemblés ou non : ce qu'« Assembler » réunirait. */
	public static List<BlockPos> voisines(Level monde, BlockPos pos) {
		return parcourir(monde, pos, false);
	}

	public static void assembler(Level monde, BlockPos pos) {
		UUID groupe = UUID.randomUUID();
		for (BlockPos p : voisines(monde, pos)) {
			if (monde.getBlockEntity(p) instanceof PlatformBlockEntity be) {
				be.grouper(groupe);
			}
		}
	}

	public static void désassembler(Level monde, BlockPos pos) {
		for (BlockPos p : groupe(monde, pos)) {
			if (monde.getBlockEntity(p) instanceof PlatformBlockEntity be) {
				be.grouper(null);
			}
		}
	}

	/** Donne à toute la plateforme l'apparence de ce bloc. */
	public static void habillerTout(Level monde, BlockPos pos) {
		BlockState apparence = monde.getBlockEntity(pos) instanceof PlatformBlockEntity be ? be.apparence() : null;
		for (BlockPos p : groupe(monde, pos)) {
			if (monde.getBlockEntity(p) instanceof PlatformBlockEntity be) {
				be.imiter(apparence);
			}
		}
	}

	public static Plateforme depuisBlocs(Level monde, BlockPos pos) {
		List<BlockPos> blocs = groupe(monde, pos);
		UUID groupe = groupeDe(monde, pos);
		int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (BlockPos p : blocs) {
			x0 = Math.min(x0, p.getX());
			z0 = Math.min(z0, p.getZ());
			x1 = Math.max(x1, p.getX());
			z1 = Math.max(z1, p.getZ());
		}
		return new Plateforme(groupe != null ? groupe : pos.immutable(), blocs, null, pos.getY(), x0, z0, x1, z1);
	}

	public static Plateforme depuisEntité(PlatformEntity entité) {
		AABB e = entité.getBoundingBox();
		return new Plateforme(entité.clé(), List.of(), entité, entité.destination(),
				(int) Math.round(e.minX), (int) Math.round(e.minZ),
				(int) Math.round(e.maxX) - 1, (int) Math.round(e.maxZ) - 1);
	}

	// --- Recherche ----------------------------------------------------------

	/** Les entités de bloc des chunks chargés autour de ce point, à un chunk près. */
	private static List<BlockEntity> entitésAutour(Level monde, BlockPos centre, int rayonChunks) {
		List<BlockEntity> trouvées = new ArrayList<>();
		ChunkPos c = ChunkPos.containing(centre);
		for (int dx = -rayonChunks; dx <= rayonChunks; dx++) {
			for (int dz = -rayonChunks; dz <= rayonChunks; dz++) {
				if (monde.getChunkSource().hasChunk(c.x() + dx, c.z() + dz)) {
					LevelChunk chunk = monde.getChunk(c.x() + dx, c.z() + dz);
					trouvées.addAll(chunk.getBlockEntities().values());
				}
			}
		}
		return trouvées;
	}

	/** La plateforme que dessert ce sélecteur : la plus proche de ceux dont il est dans la colonne. */
	@Nullable
	public static Plateforme desservie(Level monde, BlockPos sélecteur) {
		Map<Object, Plateforme> candidates = new LinkedHashMap<>();
		Set<BlockPos> vus = new HashSet<>();
		for (BlockEntity be : entitésAutour(monde, sélecteur, 1)) {
			if (!(be instanceof PlatformBlockEntity) || vus.contains(be.getBlockPos())
					|| Math.abs(be.getBlockPos().getY() - sélecteur.getY()) > HAUTEUR) {
				continue;
			}
			Plateforme p = depuisBlocs(monde, be.getBlockPos());
			vus.addAll(p.blocs());
			if (p.dessert(sélecteur)) {
				candidates.putIfAbsent(p.clé(), p);
			}
		}
		AABB colonne = new AABB(sélecteur).inflate(24, HAUTEUR, 24);
		for (PlatformEntity entité : monde.getEntities(ModEntities.LIFT_PLATFORM, colonne,
				e -> e.isAlive() && !e.posée())) {
			Plateforme p = depuisEntité(entité);
			if (p.dessert(sélecteur)) {
				candidates.put(p.clé(), p);
			}
		}
		return candidates.values().stream()
				.min(Comparator.comparingDouble(p -> distance(p, sélecteur)))
				.orElse(null);
	}

	private static double distance(Plateforme p, BlockPos s) {
		double dx = Math.max(0, Math.max(p.x0() - s.getX(), s.getX() - p.x1()));
		double dz = Math.max(0, Math.max(p.z0() - s.getZ(), s.getZ() - p.z1()));
		return (dx + dz) * 1000 + Math.abs(p.niveau() - s.getY());
	}

	/** Les étages d'une plateforme : les sélecteurs de sa colonne, du plus bas au plus haut, un par niveau. */
	public static List<Étage> étages(Level monde, Plateforme p) {
		BlockPos centre = new BlockPos((p.x0() + p.x1()) / 2, p.niveau(), (p.z0() + p.z1()) / 2);
		int rayon = 1 + Math.max(p.x1() - p.x0(), p.z1() - p.z0()) / 16;
		Map<Integer, Étage> parNiveau = new java.util.TreeMap<>();
		for (BlockEntity be : entitésAutour(monde, centre, rayon)) {
			if (be instanceof SelectorBlockEntity s && p.dessert(be.getBlockPos())) {
				Plateforme sienne = desservie(monde, be.getBlockPos());
				if (sienne == null || !sienne.clé().equals(p.clé())) {
					continue;
				}
				int niveau = s.niveau();
				Étage déjà = parNiveau.get(niveau);
				if (déjà == null || (déjà.nom().isEmpty() && s.nom().isPresent())) {
					parNiveau.put(niveau, new Étage(niveau, s.nom(), be.getBlockPos()));
				}
			}
		}
		return new ArrayList<>(parNiveau.values());
	}

	// --- Le voyage ----------------------------------------------------------

	/**
	 * Le chemin est-il libre ? Chaque case que traversent ses blocs, entre leur
	 * couche et celle d'arrivée, doit être vide (rien de solide), et la couche
	 * d'arrivée remplaçable : de l'air, de l'eau, de l'herbe.
	 */
	public static boolean cheminLibre(Level monde, Plateforme p, int niveau) {
		return obstacle(monde, p, niveau) == null;
	}

	/** La première case qui barre le chemin ({@link #cheminLibre}), ou {@code null} s'il est libre. */
	@Nullable
	public static BlockPos obstacle(Level monde, Plateforme p, int niveau) {
		int pas = Integer.signum(niveau - p.niveau());
		for (BlockPos b : p.blocs()) {
			for (int y = p.niveau() + pas; pas != 0 && y != niveau + pas; y += pas) {
				BlockPos c = new BlockPos(b.getX(), y, b.getZ());
				BlockState état = monde.getBlockState(c);
				boolean arrivée = y == niveau;
				if (arrivée ? !état.canBeReplaced() : !état.getCollisionShape(monde, c).isEmpty()) {
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Pourquoi un envoi à ce niveau serait refusé, en clair ; {@code null} s'il
	 * partirait. Le joueur l'apprend par un message ; un appel sans joueur
	 * (redstone, commande) ne disait rien — {@code /jn1 lift call} le rend.
	 */
	@Nullable
	public static String refus(Level monde, Plateforme p, int niveau) {
		if (p.enRoute()) {
			return "déjà en route";
		}
		if (p.niveau() == niveau) {
			return "déjà à cet étage";
		}
		BlockPos o = obstacle(monde, p, niveau);
		if (o != null) {
			return "chemin encombré en " + o.toShortString() + " ("
					+ net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(monde.getBlockState(o).getBlock()) + ")";
		}
		return null;
	}

	/** Envoie la plateforme à ce niveau. Rend vrai si elle part. */
	public static boolean envoyer(ServerLevel monde, Plateforme p, int niveau, @Nullable ServerPlayer joueur) {
		if (p.enRoute()) {
			dire(joueur, "message.jn1.lift.moving");
			return false;
		}
		if (p.niveau() == niveau) {
			dire(joueur, "message.jn1.lift.here");
			return false;
		}
		if (!cheminLibre(monde, p, niveau)) {
			dire(joueur, "message.jn1.lift.blocked");
			return false;
		}
		PlatformEntity entité = PlatformEntity.lever(monde, p, niveau);
		monde.playSound(null, BlockPos.containing(entité.position()), ModSounds.ELEVATOR_START, SoundSource.BLOCKS, 0.8F, 1.0F);
		alimenter(monde, depuisEntité(entité), false);
		dire(joueur, "message.jn1.lift.coming");
		return true;
	}

	/** Allume les sélecteurs de l'étage où la plateforme se tient, éteint les autres. */
	public static void alimenter(Level monde, Plateforme p, boolean arrêtée) {
		for (Étage étage : étages(monde, p)) {
			// Plusieurs sélecteurs peuvent servir le même étage : on les reprend tous.
			for (BlockEntity be : entitésAutour(monde, étage.sélecteur(), 0)) {
				if (be instanceof SelectorBlockEntity s && s.niveau() == étage.niveau() && p.dessert(be.getBlockPos())) {
					SelectorBlock.alimenter(monde, be.getBlockPos(), arrêtée && étage.niveau() == p.niveau());
				}
			}
		}
	}

	static void dire(@Nullable ServerPlayer joueur, String clé) {
		if (joueur != null) {
			joueur.sendOverlayMessage(Component.translatable(clé));
		}
	}
}
