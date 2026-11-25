package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenNotePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Une feuille posée à plat, qu'on lit en s'en approchant.
 *
 * <p>Elle se pose sur les six faces d'un bloc, comme un levier : au sol, contre
 * un mur, ou au plafond. C'est cette liberté qui oblige à traiter la caméra
 * autrement que pour le terminal, lequel est toujours mural — une note posée à
 * terre se lit en baissant les yeux, pas en la regardant de face.
 *
 * <p>Le clic droit n'ouvre rien lui-même : il demande au client d'entrer en
 * session, exactement comme le terminal. C'est le client qui fait glisser la vue
 * puis affiche la page, le serveur n'ayant pas à connaître de caméra.
 */
public class PaperNoteBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

	/** Une feuille : à peine plus qu'un plan, mais assez pour être visée. */
	private static final double ÉPAISSEUR = 1.0;
	private static final double MARGE = 3.0;

	private static final VoxelShape SOL =
			Block.box(MARGE, 0, MARGE, 16 - MARGE, ÉPAISSEUR, 16 - MARGE);
	private static final VoxelShape PLAFOND =
			Block.box(MARGE, 16 - ÉPAISSEUR, MARGE, 16 - MARGE, 16, 16 - MARGE);
	private static final VoxelShape MUR_NORD =
			Block.box(MARGE, MARGE, 0, 16 - MARGE, 16 - MARGE, ÉPAISSEUR);
	private static final VoxelShape MUR_SUD =
			Block.box(MARGE, MARGE, 16 - ÉPAISSEUR, 16 - MARGE, 16 - MARGE, 16);
	private static final VoxelShape MUR_OUEST =
			Block.box(0, MARGE, MARGE, ÉPAISSEUR, 16 - MARGE, 16 - MARGE);
	private static final VoxelShape MUR_EST =
			Block.box(16 - ÉPAISSEUR, MARGE, MARGE, 16, 16 - MARGE, 16 - MARGE);

	public PaperNoteBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACE, AttachFace.WALL)
				.setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PaperNoteBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState état, Level monde, BlockPos pos,
			Player joueur, BlockHitResult coup) {
		if (monde.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(joueur instanceof ServerPlayer serveur)) {
			return InteractionResult.PASS;
		}
		ServerPlayNetworking.send(serveur, new OpenNotePayload(pos,
				état.getValue(FACE).ordinal(), état.getValue(FACING).get3DDataValue()));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState état, BlockGetter monde, BlockPos pos,
			CollisionContext contexte) {
		return switch (état.getValue(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> switch (état.getValue(FACING)) {
				case NORTH -> MUR_SUD;
				case SOUTH -> MUR_NORD;
				case WEST -> MUR_EST;
				default -> MUR_OUEST;
			};
		};
	}

	/** On marche dessus sans la froisser. */
	@Override
	protected VoxelShape getCollisionShape(BlockState état, BlockGetter monde, BlockPos pos,
			CollisionContext contexte) {
		return Shapes.empty();
	}
}
