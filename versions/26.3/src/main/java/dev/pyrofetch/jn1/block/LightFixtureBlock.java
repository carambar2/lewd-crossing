package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModSounds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un luminaire qu'on fixe au sol, au mur ou au plafond, et qu'on allume d'un
 * clic : néons, plafonnier, applique.
 *
 * <p>Tous partagent la même mécanique, seule la forme change. Elle est décrite
 * une fois, posée au sol, puis tournée comme le jeu tourne le modèle : les
 * rotations reprennent exactement celles de l'état de bloc du levier, que
 * {@code tools/gen_furniture.py} écrit pour chaque luminaire. Si les deux
 * divergeaient, on viserait un tube là où il n'est pas dessiné.
 *
 * <p>Aucune collision : un tube au plafond ne doit pas cogner la tête, ni un
 * néon au sol faire trébucher.
 */
public class LightFixtureBlock extends FaceAttachedHorizontalDirectionalBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private final Map<AttachFace, Map<Direction, VoxelShape>> formes = new EnumMap<>(AttachFace.class);

	/**
	 * @param settings réglages du bloc
	 * @param x1 à z2 l'emprise du luminaire posé au sol, regard au nord, en
	 *                pixels : c'est la pose de référence du modèle
	 */
	public LightFixtureBlock(BlockBehaviour.Properties settings,
	                         double x1, double y1, double z1, double x2, double y2, double z2) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACE, AttachFace.WALL)
				.setValue(FACING, Direction.NORTH)
				.setValue(LIT, true));
		for (AttachFace face : AttachFace.values()) {
			Map<Direction, VoxelShape> parRegard = new EnumMap<>(Direction.class);
			for (Direction regard : Direction.Plane.HORIZONTAL) {
				parRegard.put(regard, tourner(x1, y1, z1, x2, y2, z2, face, regard));
			}
			this.formes.put(face, parRegard);
		}
	}

	/**
	 * La luminosité suit l'interrupteur.
	 *
	 * <p>À passer aux réglages du bloc : un luminaire éteint ne doit pas
	 * continuer d'éclairer la pièce.
	 */
	public static int luminosité(BlockState state, int allumé) {
		return state.getValue(LIT) ? allumé : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(LIT, true);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
	                                     CollisionContext context) {
		return this.formes.get(state.getValue(FACE)).get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
	                                       CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
	                             Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockState basculé = state.cycle(LIT);
		world.setBlock(pos, basculé, Block.UPDATE_ALL);
		world.playSound(null, pos, basculé.getValue(LIT) ? ModSounds.NEON_ON : ModSounds.NEON_OFF,
				SoundSource.BLOCKS, 0.6F, 0.9F + world.getRandom().nextFloat() * 0.2F);
		return InteractionResult.CONSUME;
	}

	/**
	 * Tourne l'emprise de référence comme l'état de bloc tourne le modèle.
	 *
	 * <p>D'abord autour de X — un quart de tour pour le mur, un demi pour le
	 * plafond —, puis autour de Y selon le regard. Les angles sont ceux du
	 * levier du jeu ; un quart de tour autour de X envoie le haut vers le nord,
	 * un quart de tour autour de Y envoie le nord vers l'est.
	 */
	private static VoxelShape tourner(double x1, double y1, double z1, double x2, double y2, double z2,
	                                  AttachFace face, Direction regard) {
		double[] a = {x1, y1, z1};
		double[] b = {x2, y2, z2};
		switch (face) {
			case WALL -> {
				a = new double[] {a[0], a[2], 16 - a[1]};
				b = new double[] {b[0], b[2], 16 - b[1]};
			}
			case CEILING -> {
				a = new double[] {a[0], 16 - a[1], 16 - a[2]};
				b = new double[] {b[0], 16 - b[1], 16 - b[2]};
			}
			case FLOOR -> { }
		}
		int quarts = quartsDeTour(face, regard);
		for (int i = 0; i < quarts; i++) {
			a = new double[] {16 - a[2], a[1], a[0]};
			b = new double[] {16 - b[2], b[1], b[0]};
		}
		return Block.box(
				Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
				Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]));
	}

	/** Les rotations Y de l'état de bloc du levier, en quarts de tour. */
	static int quartsDeTour(AttachFace face, Direction regard) {
		int base = switch (regard) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		// Au plafond, le demi-tour autour de X a retourné le nord : on le rattrape.
		return face == AttachFace.CEILING ? (base + 2) % 4 : base;
	}
}
