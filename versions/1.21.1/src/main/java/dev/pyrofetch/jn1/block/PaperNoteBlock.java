package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.network.OpenNotePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
public class PaperNoteBlock extends WallMountedBlock implements BlockEntityProvider {
	public static final MapCodec<PaperNoteBlock> CODEC = createCodec(PaperNoteBlock::new);

	/** Une feuille : à peine plus qu'un plan, mais assez pour être visée. */
	private static final double ÉPAISSEUR = 1.0;
	private static final double MARGE = 3.0;

	private static final VoxelShape SOL =
			Block.createCuboidShape(MARGE, 0, MARGE, 16 - MARGE, ÉPAISSEUR, 16 - MARGE);
	private static final VoxelShape PLAFOND =
			Block.createCuboidShape(MARGE, 16 - ÉPAISSEUR, MARGE, 16 - MARGE, 16, 16 - MARGE);
	private static final VoxelShape MUR_NORD =
			Block.createCuboidShape(MARGE, MARGE, 0, 16 - MARGE, 16 - MARGE, ÉPAISSEUR);
	private static final VoxelShape MUR_SUD =
			Block.createCuboidShape(MARGE, MARGE, 16 - ÉPAISSEUR, 16 - MARGE, 16 - MARGE, 16);
	private static final VoxelShape MUR_OUEST =
			Block.createCuboidShape(0, MARGE, MARGE, ÉPAISSEUR, 16 - MARGE, 16 - MARGE);
	private static final VoxelShape MUR_EST =
			Block.createCuboidShape(16 - ÉPAISSEUR, MARGE, MARGE, 16, 16 - MARGE, 16 - MARGE);

	public PaperNoteBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACE, BlockFace.WALL)
				.with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends WallMountedBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PaperNoteBlockEntity(pos, state);
	}

	@Override
	protected ActionResult onUse(BlockState état, World monde, BlockPos pos,
			PlayerEntity joueur, BlockHitResult coup) {
		if (monde.isClient) {
			return ActionResult.SUCCESS;
		}
		if (!(joueur instanceof ServerPlayerEntity serveur)) {
			return ActionResult.PASS;
		}
		ServerPlayNetworking.send(serveur, new OpenNotePayload(pos,
				état.get(FACE).ordinal(), état.get(FACING).getId()));
		return ActionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState état, BlockView monde, BlockPos pos,
			ShapeContext contexte) {
		return switch (état.get(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> switch (état.get(FACING)) {
				case NORTH -> MUR_SUD;
				case SOUTH -> MUR_NORD;
				case WEST -> MUR_EST;
				default -> MUR_OUEST;
			};
		};
	}

	/** On marche dessus sans la froisser. */
	@Override
	protected VoxelShape getCollisionShape(BlockState état, BlockView monde, BlockPos pos,
			ShapeContext contexte) {
		return VoxelShapes.empty();
	}
}
