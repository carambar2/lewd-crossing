package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenVendingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

/**
 * Le distributeur : la boutique sans le terminal.
 *
 * <p>Il vend moins — ni terminal ni capsule, qui restent l'affaire du
 * laboratoire —, mais on le pose où l'on veut. Ce qu'il vend et à quel prix est
 * tenu par {@link dev.pyrofetch.jn1.shop.ShopEntry}, à moins qu'on l'ait garni
 * à la main, et l'achat passe par le même guichet serveur que celui du
 * terminal : mêmes contrôles de portée, de prix et de monnaie.
 *
 * <p>Deux blocs de haut, comme une porte. La moitié basse porte tout : c'est
 * elle qu'on désigne au serveur, et elle seule rend l'objet quand on la casse.
 * Casser le haut fait tomber le bas par la mise à jour de voisinage, qui passe
 * alors par la table de butin — comme pour une porte.
 *
 * <p>En créatif, accroupi, un clic droit ouvre ses rayons pour en changer les
 * articles et les prix ({@link VendingMachineBlockEntity}). La moitié basse
 * porte l'entité qui les retient.
 */
public class VendingMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	public VendingMachineBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACING, Direction.NORTH)
				.setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	/** Face au joueur, et seulement s'il y a la place pour la moitié haute. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level world = context.getLevel();
		if (pos.getY() >= world.getMaxY() || !world.getBlockState(pos.above()).canBeReplaced(context)) {
			return null;
		}
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			return true;
		}
		BlockState dessous = world.getBlockState(pos.below());
		return dessous.is(this) && dessous.getValue(HALF) == DoubleBlockHalf.LOWER;
	}

	/** Une moitié sans l'autre disparaît. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
	                                 Direction direction, BlockPos posVoisin, BlockState voisin, RandomSource random) {
		DoubleBlockHalf moitié = state.getValue(HALF);
		boolean versLAutre = direction == (moitié == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
		if (versLAutre && (!voisin.is(this) || voisin.getValue(HALF) == moitié)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, world, ticks, pos, direction, posVoisin, voisin, random);
	}

	/**
	 * En créatif, casser le haut ne doit rien lâcher : on retire le bas sans
	 * passer par la table de butin, comme le jeu le fait pour ses portes.
	 */
	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		// À main nue non plus : le bas, retiré par voisinage, aurait rendu l'objet sans l'outil.
		if (!world.isClientSide() && (player.isCreative() || !player.hasCorrectToolForDrops(state))
				&& state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos bas = pos.below();
			BlockState dessous = world.getBlockState(bas);
			if (dessous.is(this) && dessous.getValue(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlock(bas, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				world.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, bas, Block.getId(dessous));
			}
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
	                             Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serveur) {
			ServerPlayNetworking.send(serveur, new OpenVendingPayload(bas(pos, state)));
		}
		return InteractionResult.CONSUME;
	}

	/** La moitié basse seulement : le haut ne fait que renvoyer vers elle. */
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new VendingMachineBlockEntity(pos, state) : null;
	}

	/**
	 * Créatif et accroupi : les rayons au lieu de la vitrine.
	 *
	 * <p>Pas dans {@link #useWithoutItem} : accroupi, un objet en main, le jeu ne consulte
	 * même pas le bloc et fait servir l'objet — on poserait le bloc qu'on tient
	 * au lieu d'éditer. L'événement de Fabric passe avant ce tri, des deux côtés.
	 */
	public static void registerEditor() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || !player.isCreative() || !player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (!(state.getBlock() instanceof VendingMachineBlock)) {
				return InteractionResult.PASS;
			}
			if (!world.isClientSide()
					&& world.getBlockEntity(bas(hit.getBlockPos(), state)) instanceof VendingMachineBlockEntity machine) {
				machine.préparerÉdition();
				player.openMenu(machine);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** La moitié basse, celle qu'on désigne au serveur. */
	public static BlockPos bas(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}
}
