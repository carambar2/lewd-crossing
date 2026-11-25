package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.network.OpenVendingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;

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
public class VendingMachineBlock extends HorizontalFacingBlock implements BlockEntityProvider {
	public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
	public static final MapCodec<VendingMachineBlock> CODEC = createCodec(VendingMachineBlock::new);

	public VendingMachineBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	/** Face au joueur, et seulement s'il y a la place pour la moitié haute. */
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(context)) {
			return null;
		}
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		if (state.get(HALF) == DoubleBlockHalf.LOWER) {
			return true;
		}
		BlockState dessous = world.getBlockState(pos.down());
		return dessous.isOf(this) && dessous.get(HALF) == DoubleBlockHalf.LOWER;
	}

	/** Une moitié sans l'autre disparaît. */
	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState voisin,
	                                               WorldAccess world, BlockPos pos, BlockPos posVoisin) {
		DoubleBlockHalf moitié = state.get(HALF);
		boolean versLAutre = direction == (moitié == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
		if (versLAutre && (!voisin.isOf(this) || voisin.get(HALF) == moitié)) {
			return Blocks.AIR.getDefaultState();
		}
		return super.getStateForNeighborUpdate(state, direction, voisin, world, pos, posVoisin);
	}

	/**
	 * En créatif, casser le haut ne doit rien lâcher : on retire le bas sans
	 * passer par la table de butin, comme le jeu le fait pour ses portes.
	 */
	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		// À main nue non plus : le bas, retiré par voisinage, aurait rendu l'objet sans l'outil.
		if (!world.isClient() && (player.isCreative() || !player.canHarvest(state))
				&& state.get(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos bas = pos.down();
			BlockState dessous = world.getBlockState(bas);
			if (dessous.isOf(this) && dessous.get(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlockState(bas, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
				world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, bas, Block.getRawIdFromState(dessous));
			}
		}
		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos,
	                             PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serveur) {
			ServerPlayNetworking.send(serveur, new OpenVendingPayload(bas(pos, state)));
		}
		return ActionResult.CONSUME;
	}

	/** La moitié basse seulement : le haut ne fait que renvoyer vers elle. */
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.LOWER ? new VendingMachineBlockEntity(pos, state) : null;
	}

	/**
	 * Créatif et accroupi : les rayons au lieu de la vitrine.
	 *
	 * <p>Pas dans {@link #onUse} : accroupi, un objet en main, le jeu ne consulte
	 * même pas le bloc et fait servir l'objet — on poserait le bloc qu'on tient
	 * au lieu d'éditer. L'événement de Fabric passe avant ce tri, des deux côtés.
	 */
	public static void registerEditor() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand != Hand.MAIN_HAND || !player.isCreative() || !player.isSneaking()) {
				return ActionResult.PASS;
			}
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (!(state.getBlock() instanceof VendingMachineBlock)) {
				return ActionResult.PASS;
			}
			if (!world.isClient()
					&& world.getBlockEntity(bas(hit.getBlockPos(), state)) instanceof VendingMachineBlockEntity machine) {
				machine.préparerÉdition();
				player.openHandledScreen(machine);
			}
			return ActionResult.SUCCESS;
		});
	}

	/** La moitié basse, celle qu'on désigne au serveur. */
	public static BlockPos bas(BlockPos pos, BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
	}
}
