package dev.pyrofetch.jn1.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.DimensionPadding;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * La maison, posée seulement là où le terrain s'y prête.
 *
 * <p>Le jigsaw ordinaire pose où on lui dit, sans regarder ce qu'il y a : la
 * maison enjambait les rivières, plantée sur l'eau.
 *
 * <p>Cette structure sonde donc l'emprise avant de laisser faire, et pose trois
 * conditions. Elle doit être sèche. Elle doit être assez plate pour que le
 * sous-sol reste sous terre. Et elle ne doit pas être une cuvette — un creux
 * échappe à la mesure du dénivelé, puisqu'un fond de trou régulier est
 * parfaitement plat, et c'est pourtant là que la maison se retrouvait le plus
 * souvent : au fond, cave à nu sur ses quatre faces et sol à hauteur du toit.
 *
 * <p>Un emplacement refusé ne produit rien — la maison suivante sortira
 * ailleurs, ce qui vaut mieux qu'une maison dans un trou.
 *
 * <p>Le niveau du seuil est le zéro de la maison : la hauteur de pose se compte
 * depuis lui, et tout ce qui est en dessous dans le gabarit est de la cave.
 */
public class HouseStructure extends Structure {
	public static final MapCodec<HouseStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					configCodecBuilder(instance),
					StructurePool.REGISTRY_CODEC.fieldOf("start_pool")
							.forGetter(s -> s.startPool),
					com.mojang.serialization.Codec.INT.fieldOf("seuil")
							.forGetter(s -> s.seuil),
					com.mojang.serialization.Codec.INT.optionalFieldOf("denivele_tolere", 3)
							.forGetter(s -> s.dénivelléToléré),
					com.mojang.serialization.Codec.INT.optionalFieldOf("emprise", 8)
							.forGetter(s -> s.emprise),
					com.mojang.serialization.Codec.INT.optionalFieldOf("creux_tolere", 1)
							.forGetter(s -> s.creuxToléré),
					com.mojang.serialization.Codec.INT.listOf().listOf().optionalFieldOf("reperes_sol", List.of())
							.forGetter(s -> s.repèresSol)
			).apply(instance, HouseStructure::new));

	private final RegistryEntry<StructurePool> startPool;
	/** La couche du gabarit où se trouve le seuil : la maison descend d'autant. */
	private final int seuil;
	/** Écart de terrain admis sur l'emprise, en blocs. */
	private final int dénivelléToléré;
	/** Demi-largeur sondée autour du centre. */
	private final int emprise;
	/** De combien les bords peuvent dominer le centre avant que ce soit un trou. */
	private final int creuxToléré;
	/**
	 * Des points du gabarit ({@code [x, z]}) qui doivent tomber au niveau du sol
	 * (0.84.6) : le site de confinement, refait à la main, a
	 * un anneau d'herbe autour de son poste de garde, à la couche sous le seuil.
	 * Vides pour les maisons.
	 */
	private final List<List<Integer>> repèresSol;

	public HouseStructure(Config config, RegistryEntry<StructurePool> startPool,
			int seuil, int dénivelléToléré, int emprise, int creuxToléré, List<List<Integer>> repèresSol) {
		super(config);
		this.startPool = startPool;
		this.seuil = seuil;
		this.dénivelléToléré = dénivelléToléré;
		this.emprise = emprise;
		this.creuxToléré = creuxToléré;
		this.repèresSol = repèresSol;
	}

	@Override
	protected Optional<StructurePosition> getStructurePosition(Context context) {
		ChunkPos chunk = context.chunkPos();
		int centreX = chunk.getStartX();
		int centreZ = chunk.getStartZ();

		int centre = 0;
		List<Integer> bords = new ArrayList<>(8);
		for (int dx = -this.emprise; dx <= this.emprise; dx += this.emprise) {
			for (int dz = -this.emprise; dz <= this.emprise; dz += this.emprise) {
				int x = centreX + dx;
				int z = centreZ + dz;
				int sol = context.chunkGenerator().getHeightOnGround(
						x, z, Heightmap.Type.WORLD_SURFACE_WG,
						context.world(), context.noiseConfig());
				if (mouillé(context, x, z, sol)) {
					return Optional.empty();
				}
				if (dx == 0 && dz == 0) {
					centre = sol;
				} else {
					bords.add(sol);
				}
			}
		}

		int plusBas = Math.min(centre, bords.stream().mapToInt(Integer::intValue).min().orElse(centre));
		int plusHaut = Math.max(centre, bords.stream().mapToInt(Integer::intValue).max().orElse(centre));
		if (plusHaut - plusBas > this.dénivelléToléré) {
			return Optional.empty();
		}

		// Une cuvette : le terrain remonte tout autour. La maison y serait au
		// fond d'un trou, la cave à nu sur ses quatre faces et le sol à hauteur
		// du toit. C'est le défaut qu'on voyait le plus souvent, et il ne se
		// mesure pas au dénivelé — un creux régulier est parfaitement « plat »
		// au sens de l'écart entre le plus haut et le plus bas point.
		Collections.sort(bords);
		int bordMédian = bords.get(bords.size() / 2);
		if (bordMédian - centre > this.creuxToléré) {
			return Optional.empty();
		}

		// On se cale sur le terrain au centre, là où se tient la maison. Viser
		// le point le plus bas de l'emprise l'enfonçait d'autant, et c'est ce
		// qui la faisait sortir dans un trou dès que le terrain bougeait un peu.
		BlockPos départ = new BlockPos(centreX, centre - this.seuil, centreZ);
		Optional<StructurePosition> position = StructurePoolBasedGenerator.generate(context, this.startPool,
				Optional.empty(), 1, départ, false, Optional.empty(), 80,
				StructurePoolAliasLookup.EMPTY, DimensionPadding.NONE,
				// Pas d'eau dans les blocs posés (0.85.1). En APPLY, tout bloc gorgeable posé dans une nappe du
				// sous-sol en sortait gorgé : 495 des 1 307 blocs de lumière du bunker,
				// sur une graine signalée, chacun une source d'eau dans la chambre
				// de la brèche. Les salles d'essai du jeu, enterrées, font de même.
				StructureLiquidSettings.IGNORE_WATERLOGGING);
		final int solAuCentre = centre;
		return position.map(p -> new StructurePosition(p.position(), collecteur -> {
			p.generator().ifLeft(remplir -> remplir.accept(collecteur)).ifRight(tout -> {
				for (var pièce : tout.toList().pieces()) {
					collecteur.addPiece(pièce);
				}
			});
			if (!this.repèresSol.isEmpty()) {
				int calage = this.calage(context, collecteur);
				if (calage != 0) {
					collecteur.shift(calage);
				}
				return;
			}
			int hausse = this.hausse(context, collecteur.getBoundingBox(), solAuCentre);
			if (hausse > 0) {
				collecteur.shift(hausse);
			}
		}));
	}

	/** Le calage sur les repères ne déplace pas de plus que cela, d'un côté ou de l'autre. */
	private static final int CALAGE_MAX = 8;

	/**
	 * De combien monter ou descendre la structure pour que ses repères tombent au
	 * niveau du sol (0.84.6). Long de 57 blocs, le site part d'un coin à une
	 * quarantaine de blocs de son poste de garde : la hauteur prise au coin ne
	 * disait rien du terrain sous le poste. Une fois la pièce tournée, on mesure
	 * le terrain à chaque repère, on prend la médiane, et la couche du seuil
	 * vient se poser dessus (la première case d'air : la couche des repères est
	 * celle du dessous, le sol).
	 */
	private int calage(Context context, net.minecraft.structure.StructurePiecesCollector collecteur) {
		var pièces = collecteur.toList().pieces();
		if (pièces.isEmpty() || !(pièces.get(0) instanceof net.minecraft.structure.PoolStructurePiece pièce)) {
			return 0;
		}
		List<Integer> sols = new ArrayList<>();
		for (List<Integer> repère : this.repèresSol) {
			BlockPos monde = pièce.getPos().add(net.minecraft.structure.StructureTemplate.transformAround(
					new BlockPos(repère.get(0), 0, repère.get(1)), net.minecraft.util.BlockMirror.NONE,
					pièce.getRotation(), BlockPos.ORIGIN));
			sols.add(context.chunkGenerator().getHeightOnGround(monde.getX(), monde.getZ(),
					Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig()));
		}
		Collections.sort(sols);
		int sol = sols.get(sols.size() / 2);
		int seuilActuel = pièce.getBoundingBox().getMinY() + this.seuil;
		return Math.clamp(sol - seuilActuel, -CALAGE_MAX, CALAGE_MAX);
	}

	/** Au plus deux blocs de plus : au-delà, la cave sortirait de terre du côté bas. */
	private static final int HAUSSE_MAX = 2;

	/**
	 * De combien relever la maison pour que son sol ne soit pas sous le terrain
	 * qu'elle couvre (0.81.4). La hauteur se prenait en un seul point, le coin d'où part la pièce —
	 * qui n'est pas sous la maison mais à son angle, la maison étant posée de là
	 * et tournée au hasard : sur un terrain qui monte un peu, l'entrée se
	 * retrouvait enterrée d'un bloc. On relève maintenant le terrain sur toute
	 * l'emprise réelle de la pièce, une case sur deux, et l'on cale le sol sur
	 * le quartile haut (les trois quarts du terrain sous la maison sont à son
	 * niveau ou en dessous) ; jamais plus bas que le centre, au plus deux blocs.
	 */
	private int hausse(Context context, net.minecraft.util.math.BlockBox boîte, int centre) {
		List<Integer> sols = new ArrayList<>();
		for (int x = boîte.getMinX(); x <= boîte.getMaxX(); x += 2) {
			for (int z = boîte.getMinZ(); z <= boîte.getMaxZ(); z += 2) {
				sols.add(context.chunkGenerator().getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG,
						context.world(), context.noiseConfig()));
			}
		}
		if (sols.isEmpty()) {
			return 0;
		}
		Collections.sort(sols);
		int quartileHaut = sols.get(Math.min(sols.size() - 1, (sols.size() * 3) / 4));
		return Math.clamp(quartileHaut - centre, 0, HAUSSE_MAX);
	}

	/**
	 * Y a-t-il du liquide à hauteur du seuil, ou juste en dessous ?
	 *
	 * <p>Le relevé de surface compte l'eau comme du sol : une rivière rend donc
	 * une hauteur parfaitement plausible, et c'est ainsi qu'une maison finissait
	 * posée dessus. On regarde la colonne elle-même.
	 */
	private boolean mouillé(Context context, int x, int z, int sol) {
		VerticalBlockSample colonne = context.chunkGenerator().getColumnSample(
				x, z, context.world(), context.noiseConfig());
		for (int y = sol - 1; y <= sol; y++) {
			if (!colonne.getState(y).getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public StructureType<?> getType() {
		return ModStructures.HOUSE;
	}
}
