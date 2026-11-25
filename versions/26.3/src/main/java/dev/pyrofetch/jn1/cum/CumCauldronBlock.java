package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

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
public class CumCauldronBlock extends LayeredCauldronBlock {
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).
	public static final BooleanProperty CAILLÉ = BooleanProperty.create("caille");
	/** Cinq minutes. */
	public static final int CAILLAGE = 6000;

	public CumCauldronBlock(Properties settings) {
		// 26.3 : les tables de comportements du jeu (`CauldronInteraction.Dispatcher`)
		// ne se remplissent plus de l'extérieur ; la nôtre reste vide, et ce bloc
		// répond lui-même à la fiole et au flacon ({@link #useItemOn}).
		super(Biome.Precipitation.NONE, new CauldronInteraction.Dispatcher(), settings);
		this.registerDefaultState(this.defaultBlockState().setValue(CAILLÉ, false));
	}


	/**
	 * Branche la fiole dans le chaudron vide du jeu. Appelé une fois, au démarrage.
	 *
	 * <p>26.3 : la table du chaudron vide n'accepte plus d'ajout ; on passe par
	 * l'événement d'usage d'un bloc de Fabric, avec les mêmes conditions que le
	 * jeu (pas en spectateur, pas accroupi un objet en main).
	 */
	public static void comportements() {
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, visée) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(ModItems.CUM) || player.isSpectator() || player.isSecondaryUseActive()) {
				return InteractionResult.PASS;
			}
			BlockPos pos = visée.getBlockPos();
			if (!world.getBlockState(pos).is(Blocks.CAULDRON)) {
				return InteractionResult.PASS;
			}
			// Chaudron vide + fiole : un premier niveau.
			if (!world.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				world.setBlockAndUpdate(pos, ModBlocks.CUM_CAULDRON.defaultBlockState());
				verser(world, pos, player);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** La fiole s'y verse, le flacon y puise : la table de comportements d'avant, tenue ici. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
	                                      InteractionHand hand, BlockHitResult hit) {
		if (stack.is(ModItems.CUM)) {
			if (state.getValue(CAILLÉ) || state.getValue(LEVEL) >= MAX_FILL_LEVEL) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!world.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				world.setBlockAndUpdate(pos, state.setValue(LEVEL, state.getValue(LEVEL) + 1));
				verser(world, pos, player);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.GLASS_BOTTLE)) {
			if (state.getValue(CAILLÉ)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!world.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.CUM)));
				lowerFillLevel(state, world, pos);
				world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.85F);
				world.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, world, pos, player, hand, hit);
	}

	private static void verser(Level world, BlockPos pos, Player player) {
		world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.85F);
		Cum.bruit(world, pos);
		world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CAILLÉ);
	}

	/** Le caillage commence à la première fiole ; les suivantes ne le retardent pas. */
	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onPlace(state, world, pos, ancien, notify);
		if (!state.getValue(CAILLÉ) && !world.getBlockTicks().hasScheduledTick(pos, this)) {
			world.scheduleTick(pos, this, CAILLAGE);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (!state.getValue(CAILLÉ)) {
			world.setBlockAndUpdate(pos, state.setValue(CAILLÉ, true));
			world.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.6F, 0.7F);
		}
	}

	/** Caillé : à main nue, les fromages ; le chaudron se vide. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(CAILLÉ) || !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!world.isClientSide()) {
			ItemStack fromages = new ItemStack(ModItems.CUM_CHEESE, state.getValue(LEVEL));
			if (!player.getInventory().add(fromages)) {
				player.drop(fromages, false, net.minecraft.util.Prediction.PREDICTED);
			}
			world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
			world.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
			world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean avecDonnées) {
		return new ItemStack(Items.CAULDRON);
	}

	// Pas d'eau là-dedans : ni feu éteint, ni pluie, ni stalactite.

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
	                            net.minecraft.world.entity.InsideBlockEffectApplier effets, boolean précis) {
	}

	@Override
	public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
	}

	@Override
	protected boolean canReceiveStalactiteDrip(Fluid fluid) {
		return false;
	}

	@Override
	protected void receiveStalactiteDrip(BlockState state, Level world, BlockPos pos, Fluid fluid) {
	}
}
