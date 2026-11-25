package dev.pyrofetch.jn1.cum;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;

/**
 * Un chaudron de cum. On y verse les fioles (trois niveaux, comme l'eau) ; on
 * en reprend au flacon tant qu'il est frais. Au bout de {@link #CAILLAGE}
 * ticks après la première fiole, il caille ({@link #CAILLÉ}) : à main nue, on
 * en sort autant de fromages que de niveaux, et il redevient un chaudron
 * ordinaire.
 *
 * <p>C'est un chaudron du jeu à part entière : ce qu'on y fait passe par sa
 * table de comportements, celle du chaudron vide reçoit la fiole. Ni pluie,
 * ni stalactite, ni feu éteint — il ne contient pas d'eau.
 */
public class CumCauldronBlock extends LeveledCauldronBlock {
	public static final MapCodec<CumCauldronBlock> CODEC = createCodec(CumCauldronBlock::new);
	public static final BooleanProperty CAILLÉ = BooleanProperty.of("caille");
	/** Cinq minutes. */
	public static final int CAILLAGE = 6000;

	public static final CauldronBehavior.CauldronBehaviorMap COMPORTEMENTS = CauldronBehavior.createMap("jn1_cum");

	public CumCauldronBlock(Settings settings) {
		super(Biome.Precipitation.NONE, COMPORTEMENTS, settings);
		this.setDefaultState(this.getDefaultState().with(CAILLÉ, false));
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public MapCodec<LeveledCauldronBlock> getCodec() {
		return (MapCodec) CODEC;
	}

	/** Branche la fiole et le flacon dans les tables du jeu. Appelé une fois, au démarrage. */
	public static void comportements() {
		// Chaudron vide + fiole : un premier niveau.
		CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map().put(ModItems.CUM, (state, world, pos, player, hand, stack) -> {
			if (!world.isClient()) {
				player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				world.setBlockState(pos, ModBlocks.CUM_CAULDRON.getDefaultState());
				verser(world, pos, player);
			}
			return ItemActionResult.success(world.isClient());
		});
		COMPORTEMENTS.map().put(ModItems.CUM, (state, world, pos, player, hand, stack) -> {
			if (state.get(CAILLÉ) || state.get(LEVEL) >= MAX_LEVEL) {
				return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			if (!world.isClient()) {
				player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				world.setBlockState(pos, state.with(LEVEL, state.get(LEVEL) + 1));
				verser(world, pos, player);
			}
			return ItemActionResult.success(world.isClient());
		});
		COMPORTEMENTS.map().put(Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
			if (state.get(CAILLÉ)) {
				return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			if (!world.isClient()) {
				player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(ModItems.CUM)));
				decrementFluidLevel(state, world, pos);
				world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 0.85F);
				world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
			}
			return ItemActionResult.success(world.isClient());
		});
	}

	private static void verser(World world, BlockPos pos, PlayerEntity player) {
		world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 0.85F);
		Cum.bruit(world, pos);
		world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(CAILLÉ);
	}

	/** Le caillage commence à la première fiole ; les suivantes ne le retardent pas. */
	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onBlockAdded(state, world, pos, ancien, notify);
		if (!state.get(CAILLÉ) && !world.getBlockTickScheduler().isQueued(pos, this)) {
			world.scheduleBlockTick(pos, this, CAILLAGE);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.get(CAILLÉ)) {
			world.setBlockState(pos, state.with(CAILLÉ, true));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 0.6F, 0.7F);
		}
	}

	/** Caillé : à main nue, les fromages ; le chaudron se vide. */
	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!state.get(CAILLÉ) || !player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
			return ActionResult.PASS;
		}
		if (!world.isClient()) {
			ItemStack fromages = new ItemStack(ModItems.CUM_CHEESE, state.get(LEVEL));
			if (!player.getInventory().insertStack(fromages)) {
				player.dropItem(fromages, false);
			}
			world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.BLOCKS, 0.8F, 0.9F);
			world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return ActionResult.success(world.isClient());
	}

	@Override
	public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
		return new ItemStack(Items.CAULDRON);
	}

	// Pas d'eau là-dedans : ni feu éteint, ni pluie, ni stalactite.

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
	}

	@Override
	public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
	}

	@Override
	protected boolean canBeFilledByDripstone(Fluid fluid) {
		return false;
	}

	@Override
	protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
	}
}
