package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenTerminalPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Terminal encastré dans un mur.
 *
 * <p>Le bloc n'occupe que les quatre pixels avant de son emplacement : posé
 * contre une paroi, il affleure. La propriété {@code in_use} bascule la texture
 * vers celle où des mains reposent sur le clavier — c'est ce qui fait croire que
 * le joueur est en train de taper, sans avoir à rendre le moindre bras.
 *
 * <p>La station que joue Radio.exe est tenue par l'état de bloc, comme
 * {@code in_use} : le jeu la sauvegarde et la transmet aux clients sans rien
 * écrire de plus. L'entité de bloc n'a pas de données ; elle n'existe que pour
 * donner au client un battement par terminal, où il allume et éteint le son
 * (voir {@link dev.pyrofetch.jn1.terminal.Radio}).
 */
public class ComputerBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final BooleanProperty IN_USE = BooleanProperty.create("in_use");
	/** 0 : radio éteinte ; sinon, la station de {@link dev.pyrofetch.jn1.terminal.Radio#STATIONS}. */
	public static final IntegerProperty STATION = IntegerProperty.create("station", 0,
			dev.pyrofetch.jn1.terminal.Radio.STATIONS.size());

	/** Profondeur du panneau, en pixels. */
	private static final double DEPTH = 4.0;

	public ComputerBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACING, Direction.NORTH)
				.setValue(IN_USE, false)
				.setValue(STATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, IN_USE, STATION);
	}

	/**
	 * Il se plaque contre la paroi qu'on vise, comme une trappe posée de champ.
	 *
	 * <p>{@code FACING} désigne la direction que regarde l'écran : viser la face
	 * est d'un mur donne un terminal tourné vers l'est, adossé à ce mur. Un clic
	 * sur le sol ou le plafond n'indique aucune paroi, on retombe alors sur
	 * l'orientation du joueur.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction side = context.getClickedFace();
		Direction facing = side.getAxis().isHorizontal()
				? side
				: context.getHorizontalDirection().getOpposite();
		return this.defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
	                                     CollisionContext context) {
		// Le panneau occupe la tranche opposée à son regard : le mur est derrière.
		return switch (state.getValue(FACING)) {
			case NORTH -> Shapes.box(0, 0, 1 - DEPTH / 16.0, 1, 1, 1);
			case SOUTH -> Shapes.box(0, 0, 0, 1, 1, DEPTH / 16.0);
			case WEST -> Shapes.box(1 - DEPTH / 16.0, 0, 0, 1, 1, 1);
			default -> Shapes.box(0, 0, 0, DEPTH / 16.0, 1, 1);
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
	                                       CollisionContext context) {
		return this.getShape(state, world, pos, context);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
	                             Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		// Plus de liaison exigée (0.84.0) : le terminal répond à tous.

		world.setBlock(pos, state.setValue(IN_USE, true), Block.UPDATE_CLIENTS);
		world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.6F);

		// Le client mène la séquence : approche de la caméra, puis écran.
		ServerPlayNetworking.send(serverPlayer,
				new OpenTerminalPayload(pos, state.getValue(FACING).get3DDataValue()));
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ComputerBlockEntity(pos, state);
	}

	/** Un battement côté client seulement : le serveur n'a rien à y faire. */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
	                                                              BlockEntityType<T> type) {
		if (!world.isClientSide() || type != dev.pyrofetch.jn1.registry.ModBlockEntities.COMPUTER) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<ComputerBlockEntity>)
				(monde, pos, état, entité) -> dev.pyrofetch.jn1.terminal.Radio.clientTick(monde, pos, état);
	}

	/** Rendu à l'écran de veille quand le joueur s'en va. */
	public static void release(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof ComputerBlock && state.getValue(IN_USE)) {
			world.setBlock(pos, state.setValue(IN_USE, false), Block.UPDATE_CLIENTS);
		}
	}
}
