package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Une trace de cum à plat, sur une ou plusieurs faces d'un bloc : sol, mur,
 * plafond.
 *
 * <p>Comme le lichen lumineux, sans pousser : la classe du jeu donne d'office
 * les six faces, leur forme, et le décrochage quand le bloc porteur
 * disparaît. L'eau l'emporte, comme le lichen.
 *
 * <p><b>Deux stades.</b> Fraîche, elle se recueille au flacon (la face visée,
 * une fiole) ; la casser ne laisse rien — c'est un liquide. Au bout de
 * {@link #SÉCHAGE} ticks, elle sèche ({@link #SEC}) : une croûte, qu'on
 * ramasse à la main — un clic droit, une face, ou en la cassant, toutes ses
 * faces — en cum séché. Du frais versé sur une trace sèche la ravive.
 */
public class CumTraceBlock extends MultifaceBlock {
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).
	public static final BooleanProperty SEC = BooleanProperty.create("sec");
	/** Cinq minutes. */
	public static final int SÉCHAGE = 6000;


	public CumTraceBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(SEC, false));
	}


	// 26.3 : un bloc à faces du jeu ne pousse plus de lui-même (la croissance est
	// passée à `MultifaceSpreadeableBlock`) : plus rien à déclarer, elle ne pousse pas.

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SEC);
	}

	/** Fraîche à la pose (ou ravivée) : elle sèchera dans cinq minutes. */
	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onPlace(state, world, pos, ancien, notify);
		if (!state.getValue(SEC) && (!ancien.is(this) || ancien.getValue(SEC))) {
			world.scheduleTick(pos, this, SÉCHAGE);
		}
	}

	/**
	 * Les traces posées avant qu'elles ne sèchent (0.45.4 et avant) n'ont pas de
	 * séchage prévu : le battement au hasard leur en donne un. Les sèches n'en
	 * ont plus besoin.
	 */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(SEC);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (!world.getBlockTicks().hasScheduledTick(pos, this)) {
			world.scheduleTick(pos, this, SÉCHAGE);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (!state.getValue(SEC)) {
			world.setBlock(pos, state.setValue(SEC, true), Block.UPDATE_ALL);
		}
	}

	/** Fraîche : un flacon vide la recueille. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
	                                         Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.GLASS_BOTTLE) || state.getValue(SEC)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!world.isClientSide()) {
			ôter(world, pos, state, hit.getDirection().getOpposite());
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			donner(player, hand, new ItemStack(ModItems.CUM), stack.isEmpty());
			world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.85F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Sèche : à main nue, on en détache une écaille. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(SEC)) {
			return InteractionResult.PASS;
		}
		if (!world.isClientSide()) {
			ôter(world, pos, state, hit.getDirection().getOpposite());
			donner(player, InteractionHand.MAIN_HAND, new ItemStack(ModItems.DRIED_CUM), player.getMainHandItem().isEmpty());
			world.playSound(null, pos, SoundEvents.SUSPICIOUS_SAND_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
		}
		return InteractionResult.SUCCESS;
	}

	/** Sèche et cassée : une écaille par face. Fraîche, il ne reste rien. */
	@Override
	public void playerDestroy(ServerLevel world, net.minecraft.server.level.ServerPlayer player, BlockPos pos, BlockState state,
	                       @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(world, player, pos, state, blockEntity, tool);
		if (state.getValue(SEC)) {
			int faces = (int) Direction.stream().filter(d -> hasFace(state, d)).count();
			Block.popResource(world, pos, new ItemStack(ModItems.DRIED_CUM, Math.max(1, faces)));
		}
	}

	/** Retire la face visée (ou une autre s'il n'y en a pas là) ; la case se vide avec la dernière. */
	private static void ôter(Level world, BlockPos pos, BlockState state, Direction face) {
		if (!hasFace(state, face)) {
			face = Direction.stream().filter(d -> hasFace(state, d)).findFirst().orElse(face);
		}
		BlockState reste = state.setValue(getFaceProperty(face), false);
		world.setBlock(pos, hasAnyFace(reste) ? reste : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static void donner(Player player, InteractionHand hand, ItemStack objet, boolean mainVide) {
		if (mainVide) {
			player.setItemInHand(hand, objet);
		} else if (!player.getInventory().add(objet)) {
			player.drop(objet, false, net.minecraft.util.Prediction.PREDICTED);
		}
	}
}
