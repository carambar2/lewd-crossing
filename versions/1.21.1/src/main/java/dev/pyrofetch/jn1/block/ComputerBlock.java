package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenTerminalPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
public class ComputerBlock extends HorizontalFacingBlock implements BlockEntityProvider {
	public static final BooleanProperty IN_USE = BooleanProperty.of("in_use");
	/** 0 : radio éteinte ; sinon, la station de {@link dev.pyrofetch.jn1.terminal.Radio#STATIONS}. */
	public static final IntProperty STATION = IntProperty.of("station", 0,
			dev.pyrofetch.jn1.terminal.Radio.STATIONS.size());

	public static final com.mojang.serialization.MapCodec<ComputerBlock> CODEC =
			createCodec(ComputerBlock::new);

	/** Profondeur du panneau, en pixels. */
	private static final double DEPTH = 4.0;

	public ComputerBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(IN_USE, false)
				.with(STATION, 0));
	}

	@Override
	public com.mojang.serialization.MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
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
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction side = context.getSide();
		Direction facing = side.getAxis().isHorizontal()
				? side
				: context.getHorizontalPlayerFacing().getOpposite();
		return this.getDefaultState().with(FACING, facing);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
	                                     ShapeContext context) {
		// Le panneau occupe la tranche opposée à son regard : le mur est derrière.
		return switch (state.get(FACING)) {
			case NORTH -> VoxelShapes.cuboid(0, 0, 1 - DEPTH / 16.0, 1, 1, 1);
			case SOUTH -> VoxelShapes.cuboid(0, 0, 0, 1, 1, DEPTH / 16.0);
			case WEST -> VoxelShapes.cuboid(1 - DEPTH / 16.0, 0, 0, 1, 1, 1);
			default -> VoxelShapes.cuboid(0, 0, 0, DEPTH / 16.0, 1, 1);
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
	                                       ShapeContext context) {
		return this.getOutlineShape(state, world, pos, context);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos,
	                             PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		// Plus de liaison exigée (0.84.0) : le terminal répond à tous.

		world.setBlockState(pos, state.with(IN_USE, true), Block.NOTIFY_LISTENERS);
		world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F, 1.6F);

		// Le client mène la séquence : approche de la caméra, puis écran.
		ServerPlayNetworking.send(serverPlayer,
				new OpenTerminalPayload(pos, state.get(FACING).getId()));
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ComputerBlockEntity(pos, state);
	}

	/** Un battement côté client seulement : le serveur n'a rien à y faire. */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
	                                                              BlockEntityType<T> type) {
		if (!world.isClient() || type != dev.pyrofetch.jn1.registry.ModBlockEntities.COMPUTER) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<ComputerBlockEntity>)
				(monde, pos, état, entité) -> dev.pyrofetch.jn1.terminal.Radio.clientTick(monde, pos, état);
	}

	/** Rendu à l'écran de veille quand le joueur s'en va. */
	public static void release(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof ComputerBlock && state.get(IN_USE)) {
			world.setBlockState(pos, state.with(IN_USE, false), Block.NOTIFY_LISTENERS);
		}
	}
}
