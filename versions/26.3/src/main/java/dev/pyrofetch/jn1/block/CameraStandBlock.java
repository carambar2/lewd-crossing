package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenCameraPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le trépied : on y pose l'appareil pour cadrer depuis un point fixe, avec
 * retardateur si l'on veut être sur la photo.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class CameraStandBlock extends HorizontalDirectionalBlock implements EntityBlock {
	private static final VoxelShape FORME = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);

	public CameraStandBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CameraStandBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		} else if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		} else if (player instanceof ServerPlayer serveur) {
			ServerPlayNetworking.send(serveur, OpenCameraPayload.surPied(pos));
			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}

	/**
	 * Transparent pour la vue : l'œil de la prise de vue est dans le bloc, et
	 * le rayon qui cherche les filles dans le cadre (`Sujets`, de forme
	 * « visuelle ») s'arrêtait sur le trépied dès son départ — une photo au
	 * trépied n'avait jamais personne dessus.
	 */
	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORME;
	}
}
