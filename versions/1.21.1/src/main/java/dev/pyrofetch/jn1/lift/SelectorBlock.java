package dev.pyrofetch.jn1.lift;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Le sélecteur d'étage, posé au mur de chaque palier (voir {@link Platforms}).
 *
 * <p>Clic droit : le menu des étages de la plateforme qu'il dessert, et son
 * réglage (où s'arrête la plateforme par rapport à lui, le nom de l'étage).
 * Redstone : allumé ({@link #POWERED}) tant que la plateforme est à son étage
 * — il alimente ce qui le touche, une porte de fer s'ouvre ; un signal reçu
 * l'appelle, au front montant ({@link #TRIGGERED}, comme le distributeur).
 */
public class SelectorBlock extends BlockWithEntity {
	public static final MapCodec<SelectorBlock> CODEC = createCodec(SelectorBlock::new);
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty POWERED = Properties.POWERED;
	public static final BooleanProperty TRIGGERED = Properties.TRIGGERED;

	public SelectorBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH).with(POWERED, false).with(TRIGGERED, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected net.minecraft.block.BlockRenderType getRenderType(BlockState state) {
		return net.minecraft.block.BlockRenderType.MODEL;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, TRIGGERED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SelectorBlockEntity(pos, state);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (world.getBlockEntity(pos) instanceof SelectorBlockEntity be && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
			be.nommer(stack.getName());
		}
		if (!world.isClient()) {
			Platforms.Plateforme p = Platforms.desservie(world, pos);
			if (p != null && world.getBlockEntity(pos) instanceof SelectorBlockEntity be) {
				alimenter(world, pos, !p.enRoute() && p.niveau() == be.niveau());
			}
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient() && player instanceof ServerPlayerEntity joueur) {
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.4F, 1.4F);
			LiftHandler.ouvrir(joueur, pos);
		}
		return ActionResult.success(world.isClient());
	}

	// --- Redstone -----------------------------------------------------------

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) ? 15 : 0;
	}

	@Override
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
	                              boolean notify) {
		boolean reçu = world.isReceivingRedstonePower(pos);
		if (reçu && !state.get(TRIGGERED)) {
			world.setBlockState(pos, state.with(TRIGGERED, true), Block.NOTIFY_LISTENERS);
			if (world instanceof ServerWorld monde) {
				appeler(monde, pos, null);
			}
		} else if (!reçu && state.get(TRIGGERED)) {
			world.setBlockState(pos, state.with(TRIGGERED, false), Block.NOTIFY_LISTENERS);
		}
	}

	/** Allume ou éteint le sélecteur : la plateforme est-elle à son étage ? */
	public static void alimenter(World monde, BlockPos pos, boolean allumé) {
		BlockState état = monde.getBlockState(pos);
		if (état.getBlock() instanceof SelectorBlock && état.get(POWERED) != allumé) {
			monde.setBlockState(pos, état.with(POWERED, allumé), Block.NOTIFY_ALL);
		}
	}

	/** Appelle la plateforme à l'étage de ce sélecteur. */
	public static void appeler(ServerWorld monde, BlockPos pos, @Nullable ServerPlayerEntity joueur) {
		if (!(monde.getBlockEntity(pos) instanceof SelectorBlockEntity be)) {
			return;
		}
		Platforms.Plateforme p = Platforms.desservie(monde, pos);
		if (p == null) {
			Platforms.dire(joueur, "message.jn1.lift.no_platform");
			return;
		}
		Platforms.envoyer(monde, p, be.niveau(), joueur);
	}
}
