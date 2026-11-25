package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.photo.PhotoItem;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Le cadre photo, au mur, au sol ou au plafond. Un tirage posé dessus s'étend
 * sur autant de cadres voisins que son format le demande ; un clic à main nue
 * le décroche et le rend.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoFrameBlock extends WallMountedBlock implements BlockEntityProvider {
	public static final MapCodec<PhotoFrameBlock> CODEC = createCodec(PhotoFrameBlock::new);
	private static final double ÉPAISSEUR = 1.0;
	private static final VoxelShape SOL = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
	private static final VoxelShape PLAFOND = Block.createCuboidShape(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape MUR_NORD = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
	private static final VoxelShape MUR_SUD = Block.createCuboidShape(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
	private static final VoxelShape MUR_OUEST = Block.createCuboidShape(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
	private static final VoxelShape MUR_EST = Block.createCuboidShape(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);

	public PhotoFrameBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACE, BlockFace.WALL).with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends WallMountedBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PhotoFrameBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(ModItems.PHOTO)) {
			// Décrocher se fait à main nue : une torche ou un bloc en main
			// décrochaient la photo au lieu de se poser.
			return stack.isEmpty() ? ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
					: ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}

		if (world.isClient) {
			return ItemActionResult.SUCCESS;
		}

		PhotoFrameBlockEntity cadre = PhotoFrameBlockEntity.at(world, pos);
		if (cadre == null) {
			return ItemActionResult.FAIL;
		}

		if (!cadre.estLibre()) {
			player.sendMessage(Text.translatable("photo.jn1.frame.occupied"), true);
			return ItemActionResult.FAIL;
		}

		if (!PhotoItem.estDéveloppée(stack)) {
			player.sendMessage(Text.translatable("photo.jn1.frame.blank"), true);
			return ItemActionResult.FAIL;
		}

		PhotoFormat format = PhotoItem.format(stack);
		if (!cadre.accrocher(PhotoItem.numéro(stack), format)) {
			player.sendMessage(Text.translatable("photo.jn1.frame.no_room", format.casesEnLargeur(), format.casesEnHauteur()), true);
			return ItemActionResult.FAIL;
		}

		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}

		world.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.8F, 1.2F);
		return ItemActionResult.SUCCESS;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		PhotoFrameBlockEntity cadre = PhotoFrameBlockEntity.at(world, pos);
		if (cadre == null) {
			return ActionResult.PASS;
		}

		PhotoFrameBlockEntity.Décrochage décroché = cadre.décrocher();
		if (décroché == null) {
			return ActionResult.PASS;
		}

		rendre(world, pos, player, décroché);
		world.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 0.8F, 1.2F);
		return ActionResult.SUCCESS;
	}

	private static void rendre(World monde, BlockPos pos, PlayerEntity joueur, PhotoFrameBlockEntity.Décrochage décroché) {
		ItemStack tirage = PhotoItem.tirage(ModItems.PHOTO, décroché.numéro(), décroché.format());
		if (joueur == null || !joueur.getInventory().insertStack(tirage)) {
			ItemEntity tombé = new ItemEntity(monde, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, tirage);
			tombé.setToDefaultPickupDelay();
			monde.spawnEntity(tombé);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && !world.isClient) {
			PhotoFrameBlockEntity cadre = PhotoFrameBlockEntity.at(world, pos);
			if (cadre != null) {
				PhotoFrameBlockEntity.Décrochage décroché = cadre.décrocher();
				if (décroché != null) {
					rendre(world, pos, null, décroché);
				}
			}
		}

		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch ((BlockFace)state.get(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> {
				switch ((Direction)state.get(FACING)) {
					case NORTH:
						yield MUR_SUD;
					case SOUTH:
						yield MUR_NORD;
					case WEST:
						yield MUR_EST;
					default:
						yield MUR_OUEST;
				}
			}
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}
}
