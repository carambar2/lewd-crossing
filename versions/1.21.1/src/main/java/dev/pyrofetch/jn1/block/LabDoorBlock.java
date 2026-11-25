package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModParticles;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Une porte ordinaire, à ceci près qu'elle ne mène pas où elle devrait.
 *
 * <p>Elle s'ouvre, se ferme et se pose comme une porte ordinaire : le joueur
 * sait donc quoi en faire sans qu'on le lui explique. Le jeu de blocs est celui
 * du cuivre et non du fer — celui du fer interdit l'ouverture à la main, ce qui
 * rendait la porte inutilisable et, du même coup, le passage inatteignable. Le passage
 * lui-même est surveillé par {@link dev.pyrofetch.jn1.lab.LabDoorWatcher}, et
 * non ici : {@code onEntityCollision} n'est pas appelée pour ce bloc, même
 * quand une entité le traverse de part en part.
 */
public class LabDoorBlock extends DoorBlock {
	public static final MapCodec<LabDoorBlock> CODEC =
			createCodec(settings -> new LabDoorBlock(BlockSetType.COPPER, settings));

	public LabDoorBlock(BlockSetType type, Settings settings) {
		super(type, settings);
	}

	@Override
	public MapCodec<? extends DoorBlock> getCodec() {
		return CODEC;
	}

	/**
	 * Le passage se voit quand la porte est ouverte.
	 *
	 * <p>Rien n'annonçait qu'une porte du laboratoire mène ailleurs : ouverte,
	 * elle montrait la même chose que n'importe quelle porte, et il fallait la
	 * traverser pour l'apprendre. Les particules occupent maintenant le plan de
	 * l'embrasure — celui qu'on franchit, perpendiculaire au sens de la porte —
	 * de sorte qu'on voie une surface tendue là où il n'y a que du vide.
	 *
	 * <p>Les deux moitiés du battant émettent chacune les siennes, ce qui
	 * remplit la hauteur d'homme sans avoir à traiter le cas du haut et du bas
	 * séparément.
	 */
	@Override
	public void randomDisplayTick(BlockState état, World monde, BlockPos pos, Random alea) {
		if (!état.get(OPEN)) {
			return;
		}

		// L'axe de la porte, dans son plan : c'est là-dessus que les particules
		// s'étalent en largeur.
		Direction sens = état.get(FACING);
		Direction travers = sens.rotateYClockwise();

		for (int n = 0; n < PARTICULES_PAR_PASSE; n++) {
			double t = alea.nextDouble() - 0.5;
			double x = pos.getX() + 0.5 + travers.getOffsetX() * t;
			double y = pos.getY() + alea.nextDouble();
			double z = pos.getZ() + 0.5 + travers.getOffsetZ() * t;

			// Une dérive faible, perpendiculaire au plan : les particules en
			// sortent d'un souffle puis y reviennent, ce qui donne l'épaisseur
			// d'une surface plutôt qu'un rideau plat.
			double souffle = (alea.nextDouble() - 0.5) * 0.09;
			monde.addParticle(ParticleTypes.PORTAL, x, y, z,
					sens.getOffsetX() * souffle,
					(alea.nextDouble() - 0.5) * 0.05,
					sens.getOffsetZ() * souffle);
		}

		// Une escarbille du laboratoire de loin en loin : elle rattache le
		// passage au reste de l'installation, que ces poussières signalent déjà.
		if (alea.nextInt(6) == 0) {
			monde.addParticle(ModParticles.GLYPH_MOTE,
					pos.getX() + 0.5 + travers.getOffsetX() * (alea.nextDouble() - 0.5),
					pos.getY() + alea.nextDouble(),
					pos.getZ() + 0.5 + travers.getOffsetZ() * (alea.nextDouble() - 0.5),
					0.0, 0.015, 0.0);
		}
	}

	/**
	 * Assez pour que la surface se lise, pas au point de masquer ce qu'il y a
	 * derrière : la porte reste une porte, on doit voir au travers.
	 */
	private static final int PARTICULES_PAR_PASSE = 4;
}
