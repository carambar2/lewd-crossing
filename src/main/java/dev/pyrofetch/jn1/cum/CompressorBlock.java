package dev.pyrofetch.jn1.cum;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Le compresseur.
 *
 * <p>Dessiné d'après un croquis ({@code tools/gen_boutique_gaster.py}) :
 * un bocal plein ({@value CumJarBlock#PLEIN} fioles) et garni de sa figurine,
 * tendu au compresseur, est versé dans le cylindre de verre — le bocal revient
 * vide ; le piston y descend en {@link #ÉTAPE} étapes de {@link #PAS} ticks,
 * la vapeur sort des tuyaux, un cristal naît sur la platine et flotte, puis
 * il saute. Cassé en marche, il rend le bocal plein.
 */
public class CompressorBlock extends HorizontalFacingBlock {
	public static final MapCodec<CompressorBlock> CODEC = createCodec(CompressorBlock::new);
	/** 0 : au repos, vide ; 1 : le bocal posé ; 2 à 7 : le plateau descend. */
	public static final IntProperty ÉTAPE = IntProperty.of("etape", 0, 7);
	private static final int DERNIÈRE = 7;
	private static final int PAS = 8;
	/** Une fois et demie la hauteur d'un bloc, comme son modèle (chapiteau compris). */
	private static final net.minecraft.util.shape.VoxelShape FORME = Block.createCuboidShape(0, 0, 0, 16, 24, 16);

	public CompressorBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH).with(ÉTAPE, 0));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ÉTAPE);
	}

	@Override
	protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
			BlockPos pos, net.minecraft.block.ShapeContext context) {
		return FORME;
	}

	/** Les runes et la platine luisent ; plus fort quand il travaille. */
	public static int lumière(BlockState state) {
		return state.get(ÉTAPE) == 0 ? 7 : 11;
	}

	/** En marche : de la vapeur sort des deux tuyaux (au repos du modèle, façade au nord). */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(ÉTAPE) == 0) {
			return;
		}
		net.minecraft.util.math.Direction face = state.get(FACING);
		// Les bouts des coudes (gen_modeles_3d.BOUTS_TUYAUX), en pixels du modèle.
		for (double[] bout : new double[][] {{17.6, 11.0, 5.4}, {-1.8, 13.5, 10.1}}) {
			// Le modèle est tourné comme l'état : on tourne le point de même.
			double x = bout[0] / 16 - 0.5, z = bout[2] / 16 - 0.5, rx = x, rz = z;
			switch (face) {
				case EAST -> { rx = -z; rz = x; }
				case SOUTH -> { rx = -x; rz = -z; }
				case WEST -> { rx = z; rz = -x; }
				default -> { }
			}
			world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + rx, pos.getY() + bout[1] / 16,
					pos.getZ() + 0.5 + rz, (bout[0] > 8 ? 1 : -1) * 0.01, 0.03, 0.0);
		}
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
			PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(ModItems.CUM_JAR)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (state.get(ÉTAPE) != 0) {
			player.sendMessage(Text.translatable("message.jn1.compressor.occupe"), true);
			return ItemActionResult.success(world.isClient);
		}
		if (!CumJarItem.prêt(stack)) {
			player.sendMessage(Text.translatable("message.jn1.compressor.pas_pret", CumJarItem.cum(stack),
					CumJarBlock.PLEIN, Text.translatable(CumJarItem.figurine(stack)
							? "tooltip.jn1.cum_jar.avec_figurine" : "tooltip.jn1.cum_jar.sans_figurine")), true);
			return ItemActionResult.success(world.isClient);
		}
		if (!world.isClient) {
			// Versé dans le cylindre : la figurine avec, le bocal revient vide.
			if (!player.isCreative()) {
				stack.decrement(1);
				player.getInventory().offerOrDrop(new ItemStack(ModItems.CUM_JAR));
			}
			world.setBlockState(pos, state.with(ÉTAPE, 1));
			world.scheduleBlockTick(pos, this, PAS * 2);
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 0.8F);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient) {
			player.sendMessage(Text.translatable(state.get(ÉTAPE) != 0 ? "message.jn1.compressor.occupe"
					: "message.jn1.compressor.vide"), true);
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int étape = state.get(ÉTAPE);
		if (étape == 0) {
			return;
		}
		if (étape < DERNIÈRE) {
			world.setBlockState(pos, state.with(ÉTAPE, étape + 1));
			world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.6F,
					0.5F + random.nextFloat() * 0.15F);
			if (étape + 1 < 6) {
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.9F, 0.7F);
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.25F, 1.6F);
			} else {
				world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 1.0F, 1.4F);
			}
			world.scheduleBlockTick(pos, this, PAS);
			return;
		}
		world.setBlockState(pos, state.with(ÉTAPE, 0));
		ItemEntity cristal = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				new ItemStack(ModItems.CUM_CRYSTAL));
		cristal.setVelocity(random.nextGaussian() * 0.04, 0.3, random.nextGaussian() * 0.04);
		world.spawnEntity(cristal);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5F, 1.2F);
		world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.6F, 0.6F);
		world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
				16, 0.2, 0.15, 0.2, 0.05);
	}

	/** Cassé en marche : le bocal revient, plein. */
	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && state.get(ÉTAPE) != 0 && !world.isClient) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, bocalPlein());
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	public static ItemStack bocalPlein() {
		ItemStack bocal = new ItemStack(ModItems.CUM_JAR);
		bocal.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT
				.with(CumJarBlock.CUM, CumJarBlock.PLEIN).with(CumJarBlock.FIGURINE, true));
		return bocal;
	}
}
