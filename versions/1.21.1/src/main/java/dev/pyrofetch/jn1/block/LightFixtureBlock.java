package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModSounds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
public class LightFixtureBlock extends WallMountedBlock {
	public static final BooleanProperty LIT = Properties.LIT;

	public static final MapCodec<LightFixtureBlock> CODEC = createCodec(settings ->
			new LightFixtureBlock(settings, 0, 0, 0, 16, 1, 16));

	private final Map<BlockFace, Map<Direction, VoxelShape>> formes = new EnumMap<>(BlockFace.class);

	/**
	 * @param settings réglages du bloc
	 * @param x1 à z2 l'emprise du luminaire posé au sol, regard au nord, en
	 *                pixels : c'est la pose de référence du modèle
	 */
	public LightFixtureBlock(AbstractBlock.Settings settings,
	                         double x1, double y1, double z1, double x2, double y2, double z2) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACE, BlockFace.WALL)
				.with(FACING, Direction.NORTH)
				.with(LIT, true));
		for (BlockFace face : BlockFace.values()) {
			Map<Direction, VoxelShape> parRegard = new EnumMap<>(Direction.class);
			for (Direction regard : Direction.Type.HORIZONTAL) {
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
		return state.get(LIT) ? allumé : 0;
	}

	@Override
	protected MapCodec<? extends WallMountedBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, LIT);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		return state == null ? null : state.with(LIT, true);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
	                                     ShapeContext context) {
		return this.formes.get(state.get(FACE)).get(state.get(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
	                                       ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos,
	                             PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		BlockState basculé = state.cycle(LIT);
		world.setBlockState(pos, basculé, Block.NOTIFY_ALL);
		world.playSound(null, pos, basculé.get(LIT) ? ModSounds.NEON_ON : ModSounds.NEON_OFF,
				SoundCategory.BLOCKS, 0.6F, 0.9F + world.random.nextFloat() * 0.2F);
		return ActionResult.CONSUME;
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
	                                  BlockFace face, Direction regard) {
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
		return Block.createCuboidShape(
				Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
				Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]));
	}

	/** Les rotations Y de l'état de bloc du levier, en quarts de tour. */
	static int quartsDeTour(BlockFace face, Direction regard) {
		int base = switch (regard) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		// Au plafond, le demi-tour autour de X a retourné le nord : on le rattrape.
		return face == BlockFace.CEILING ? (base + 2) % 4 : base;
	}
}
