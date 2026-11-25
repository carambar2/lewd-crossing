package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
	public static final IntProperty CUM = IntProperty.of("cum", 0, PLEIN);
	public static final BooleanProperty FIGURINE = BooleanProperty.of("figurine");

	private static final VoxelShape FORME = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 11.5, 11.0);

	public CumJarBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(CUM, 0).with(FIGURINE, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(CUM, FIGURINE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return FORME;
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
			PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (stack.isOf(ModItems.CUM)) {
			int place = PLEIN - state.get(CUM);
			if (place <= 0) {
				player.sendMessage(Text.translatable("message.jn1.cum_jar.plein"), true);
				return ItemActionResult.success(world.isClient);
			}
			if (!world.isClient) {
				int n = Math.min(place, stack.getCount());
				BlockState rempli = state.with(CUM, state.get(CUM) + n);
				if (!player.isCreative()) {
					stack.decrement(n);
					player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE, n));
				}
				world.setBlockState(pos, rempli);
				world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 0.8F);
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.6F, 1.2F);
				player.sendMessage(état(rempli), true);
			}
			return ItemActionResult.success(world.isClient);
		}
		if (stack.isOf(ModItems.FIGURINE)) {
			if (state.get(FIGURINE)) {
				player.sendMessage(Text.translatable("message.jn1.cum_jar.deja_figurine"), true);
				return ItemActionResult.success(world.isClient);
			}
			if (!world.isClient) {
				BlockState garni = state.with(FIGURINE, true);
				stack.decrementUnlessCreative(1, player);
				world.setBlockState(pos, garni);
				world.playSound(null, pos, state.get(CUM) > 0 ? SoundEvents.ENTITY_GENERIC_SPLASH
						: SoundEvents.BLOCK_GLASS_HIT, SoundCategory.BLOCKS, 0.5F, 1.4F);
				player.sendMessage(état(garni), true);
			}
			return ItemActionResult.success(world.isClient);
		}
		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient) {
			player.sendMessage(état(state), true);
		}
		return ActionResult.success(world.isClient);
	}

	/** « Bocal : 34 / 69 — sans figurine », dans la barre d'action. */
	public static Text état(BlockState state) {
		return Text.translatable("message.jn1.cum_jar.etat", state.get(CUM), PLEIN,
				Text.translatable(state.get(FIGURINE) ? "tooltip.jn1.cum_jar.avec_figurine"
						: "tooltip.jn1.cum_jar.sans_figurine"));
	}
}
