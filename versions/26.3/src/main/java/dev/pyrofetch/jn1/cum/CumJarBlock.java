package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le bocal à cum. Plein et garni
 * de sa figurine, il part au {@link CompressorBlock}, qui en tire un Cum Crystal.
 *
 * <p>Le contenu est dans l'état du bloc ({@link #CUM}, {@link #FIGURINE}) : le
 * butin le recopie sur l'objet ({@code copy_state}), et l'objet le rend au bloc
 * en le posant — le jeu le fait seul pour {@code minecraft:block_state}. Pas
 * d'entité de bloc.
 *
 * <p>Fiole de cum en main : il prend toute la pile, jusqu'au plein, et rend
 * autant de flacons vides. Figurine en main : elle y tombe. Main vide : il dit
 * où il en est.
 */
public class CumJarBlock extends Block {
	public static final int PLEIN = 69;
	public static final IntegerProperty CUM = IntegerProperty.create("cum", 0, PLEIN);
	public static final BooleanProperty FIGURINE = BooleanProperty.create("figurine");

	private static final VoxelShape FORME = Block.box(5.0, 0.0, 5.0, 11.0, 11.5, 11.0);

	public CumJarBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(CUM, 0).setValue(FIGURINE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CUM, FIGURINE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORME;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.is(ModItems.CUM)) {
			int place = PLEIN - state.getValue(CUM);
			if (place <= 0) {
				player.sendOverlayMessage(Component.translatable("message.jn1.cum_jar.plein"));
				return InteractionResult.SUCCESS;
			}
			if (!world.isClientSide()) {
				int n = Math.min(place, stack.getCount());
				BlockState rempli = state.setValue(CUM, state.getValue(CUM) + n);
				if (!player.isCreative()) {
					stack.shrink(n);
					player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE, n), net.minecraft.util.Prediction.PREDICTED);
				}
				world.setBlockAndUpdate(pos, rempli);
				world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
				world.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.2F);
				player.sendOverlayMessage(état(rempli));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.FIGURINE)) {
			if (state.getValue(FIGURINE)) {
				player.sendOverlayMessage(Component.translatable("message.jn1.cum_jar.deja_figurine"));
				return InteractionResult.SUCCESS;
			}
			if (!world.isClientSide()) {
				BlockState garni = state.setValue(FIGURINE, true);
				stack.consume(1, player);
				world.setBlockAndUpdate(pos, garni);
				world.playSound(null, pos, state.getValue(CUM) > 0 ? SoundEvents.GENERIC_SPLASH
						: SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.5F, 1.4F);
				player.sendOverlayMessage(état(garni));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide()) {
			player.sendOverlayMessage(état(state));
		}
		return InteractionResult.SUCCESS;
	}

	/** « Bocal : 34 / 69 — sans figurine », dans la barre d'action. */
	public static Component état(BlockState state) {
		return Component.translatable("message.jn1.cum_jar.etat", state.getValue(CUM), PLEIN,
				Component.translatable(state.getValue(FIGURINE) ? "tooltip.jn1.cum_jar.avec_figurine"
						: "tooltip.jn1.cum_jar.sans_figurine"));
	}
}
