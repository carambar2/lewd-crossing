package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.photo.PhotoFormat;
import dev.pyrofetch.jn1.photo.PhotoItem;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le cadre photo, au mur, au sol ou au plafond. Un tirage posé dessus s'étend
 * sur autant de cadres voisins que son format le demande ; un clic à main nue
 * le décroche et le rend.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoFrameBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
	private static final double ÉPAISSEUR = 1.0;
	private static final VoxelShape SOL = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
	private static final VoxelShape PLAFOND = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape MUR_NORD = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
	private static final VoxelShape MUR_SUD = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
	private static final VoxelShape MUR_OUEST = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
	private static final VoxelShape MUR_EST = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);

	public PhotoFrameBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACE, AttachFace.WALL).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PhotoFrameBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ModItems.PHOTO)) {
			// Décrocher se fait à main nue : une torche ou un bloc en main
			// décrochaient la photo au lieu de se poser.
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND
					: InteractionResult.PASS;
		}

		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		PhotoFrameBlockEntity cadre = PhotoFrameBlockEntity.at(world, pos);
		if (cadre == null) {
			return InteractionResult.FAIL;
		}

		if (!cadre.estLibre()) {
			player.sendOverlayMessage(Component.translatable("photo.jn1.frame.occupied"));
			return InteractionResult.FAIL;
		}

		if (!PhotoItem.estDéveloppée(stack)) {
			player.sendOverlayMessage(Component.translatable("photo.jn1.frame.blank"));
			return InteractionResult.FAIL;
		}

		PhotoFormat format = PhotoItem.format(stack);
		if (!cadre.accrocher(PhotoItem.numéro(stack), format)) {
			player.sendOverlayMessage(Component.translatable("photo.jn1.frame.no_room", format.casesEnLargeur(), format.casesEnHauteur()));
			return InteractionResult.FAIL;
		}

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		world.playSound((Player)null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		PhotoFrameBlockEntity cadre = PhotoFrameBlockEntity.at(world, pos);
		if (cadre == null) {
			return InteractionResult.PASS;
		}

		PhotoFrameBlockEntity.Décrochage décroché = cadre.décrocher();
		if (décroché == null) {
			return InteractionResult.PASS;
		}

		rendre(world, pos, player, décroché);
		world.playSound((Player)null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Rend le tirage décroché : au joueur, sinon au sol. Aussi appelé par
	 * l'entité de bloc quand le cadre disparaît (26.3 : {@code onRemove} n'existe
	 * plus, c'est {@link PhotoFrameBlockEntity#preRemoveSideEffects} qui s'en charge).
	 */
	static void rendre(Level monde, BlockPos pos, Player joueur, PhotoFrameBlockEntity.Décrochage décroché) {
		ItemStack tirage = PhotoItem.tirage(ModItems.PHOTO, décroché.numéro(), décroché.format());
		if (joueur == null || !joueur.getInventory().add(tirage)) {
			ItemEntity tombé = new ItemEntity(monde, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, tirage);
			tombé.setDefaultPickUpDelay();
			monde.addFreshEntity(tombé);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch ((AttachFace)state.getValue(FACE)) {
			case FLOOR -> SOL;
			case CEILING -> PLAFOND;
			case WALL -> {
				switch ((Direction)state.getValue(FACING)) {
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
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}
}
