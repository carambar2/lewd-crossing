package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

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
public class CompressorBlock extends HorizontalDirectionalBlock {
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).
	/** 0 : au repos, vide ; 1 : le bocal posé ; 2 à 7 : le plateau descend. */
	public static final IntegerProperty ÉTAPE = IntegerProperty.create("etape", 0, 7);
	private static final int DERNIÈRE = 7;
	private static final int PAS = 8;
	/** Une fois et demie la hauteur d'un bloc, comme son modèle (chapiteau compris). */
	private static final net.minecraft.world.phys.shapes.VoxelShape FORME = Block.box(0, 0, 0, 16, 24, 16);

	public CompressorBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ÉTAPE, 0));
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ÉTAPE);
	}

	@Override
	protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world,
			BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return FORME;
	}

	/** Les runes et la platine luisent ; plus fort quand il travaille. */
	public static int lumière(BlockState state) {
		return state.getValue(ÉTAPE) == 0 ? 7 : 11;
	}

	/** En marche : de la vapeur sort des deux tuyaux (au repos du modèle, façade au nord). */
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		if (state.getValue(ÉTAPE) == 0) {
			return;
		}
		net.minecraft.core.Direction face = state.getValue(FACING);
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
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ModItems.CUM_JAR)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (state.getValue(ÉTAPE) != 0) {
			player.sendOverlayMessage(Component.translatable("message.jn1.compressor.occupe"));
			return InteractionResult.SUCCESS;
		}
		if (!CumJarItem.prêt(stack)) {
			player.sendOverlayMessage(Component.translatable("message.jn1.compressor.pas_pret", CumJarItem.cum(stack),
					CumJarBlock.PLEIN, Component.translatable(CumJarItem.figurine(stack)
							? "tooltip.jn1.cum_jar.avec_figurine" : "tooltip.jn1.cum_jar.sans_figurine")));
			return InteractionResult.SUCCESS;
		}
		if (!world.isClientSide()) {
			// Versé dans le cylindre : la figurine avec, le bocal revient vide.
			if (!player.isCreative()) {
				stack.shrink(1);
				player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CUM_JAR), net.minecraft.util.Prediction.PREDICTED);
			}
			world.setBlockAndUpdate(pos, state.setValue(ÉTAPE, 1));
			world.scheduleTick(pos, this, PAS * 2);
			world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide()) {
			player.sendOverlayMessage(Component.translatable(state.getValue(ÉTAPE) != 0 ? "message.jn1.compressor.occupe"
					: "message.jn1.compressor.vide"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		int étape = state.getValue(ÉTAPE);
		if (étape == 0) {
			return;
		}
		if (étape < DERNIÈRE) {
			world.setBlockAndUpdate(pos, state.setValue(ÉTAPE, étape + 1));
			world.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F,
					0.5F + random.nextFloat() * 0.15F);
			if (étape + 1 < 6) {
				world.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.9F, 0.7F);
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.6F);
			} else {
				world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.4F);
			}
			world.scheduleTick(pos, this, PAS);
			return;
		}
		world.setBlockAndUpdate(pos, state.setValue(ÉTAPE, 0));
		ItemEntity cristal = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				new ItemStack(ModItems.CUM_CRYSTAL));
		cristal.setDeltaMovement(random.nextGaussian() * 0.04, 0.3, random.nextGaussian() * 0.04);
		world.addFreshEntity(cristal);
		world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 1.2F);
		world.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6F, 0.6F);
		world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
				16, 0.2, 0.15, 0.2, 0.05);
	}

	/** Cassé en marche : le bocal revient, plein. */
	// 26.3 : `onRemove` est devenu `affectNeighborsAfterRemoval`, appelé quand le
	// bloc a changé (côté serveur).
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
		if (state.getValue(ÉTAPE) != 0) {
			Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, bocalPlein());
		}
		super.affectNeighborsAfterRemoval(state, world, pos, moved);
	}

	public static ItemStack bocalPlein() {
		ItemStack bocal = new ItemStack(ModItems.CUM_JAR);
		bocal.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
				.with(CumJarBlock.CUM, CumJarBlock.PLEIN).with(CumJarBlock.FIGURINE, true));
		return bocal;
	}
}
