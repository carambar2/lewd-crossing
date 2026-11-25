package dev.pyrofetch.jn1.lift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
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
public class SelectorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

	public SelectorBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(TRIGGERED, false));
	}

	@Override
	protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
		return net.minecraft.world.level.block.RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, TRIGGERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SelectorBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (world.getBlockEntity(pos) instanceof SelectorBlockEntity be && stack.has(DataComponents.CUSTOM_NAME)) {
			be.nommer(stack.getHoverName());
		}
		if (!world.isClientSide()) {
			Platforms.Plateforme p = Platforms.desservie(world, pos);
			if (p != null && world.getBlockEntity(pos) instanceof SelectorBlockEntity be) {
				alimenter(world, pos, !p.enRoute() && p.niveau() == be.niveau());
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide() && player instanceof ServerPlayer joueur) {
			world.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.4F);
			LiftHandler.ouvrir(joueur, pos);
		}
		return InteractionResult.SUCCESS;
	}

	// --- Redstone -----------------------------------------------------------

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock,
	                              @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean notify) {
		boolean reçu = world.hasNeighborSignal(pos);
		if (reçu && !state.getValue(TRIGGERED)) {
			world.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_CLIENTS);
			if (world instanceof ServerLevel monde) {
				appeler(monde, pos, null);
			}
		} else if (!reçu && state.getValue(TRIGGERED)) {
			world.setBlock(pos, state.setValue(TRIGGERED, false), Block.UPDATE_CLIENTS);
		}
	}

	/** Allume ou éteint le sélecteur : la plateforme est-elle à son étage ? */
	public static void alimenter(Level monde, BlockPos pos, boolean allumé) {
		BlockState état = monde.getBlockState(pos);
		if (état.getBlock() instanceof SelectorBlock && état.getValue(POWERED) != allumé) {
			monde.setBlock(pos, état.setValue(POWERED, allumé), Block.UPDATE_ALL);
		}
	}

	/** Appelle la plateforme à l'étage de ce sélecteur. */
	public static void appeler(ServerLevel monde, BlockPos pos, @Nullable ServerPlayer joueur) {
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
