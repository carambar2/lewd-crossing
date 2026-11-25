package dev.pyrofetch.jn1.cum;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LichenGrower;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
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
public class CumTraceBlock extends MultifaceGrowthBlock {
	public static final MapCodec<CumTraceBlock> CODEC = createCodec(CumTraceBlock::new);
	public static final BooleanProperty SEC = BooleanProperty.of("sec");
	/** Cinq minutes. */
	public static final int SÉCHAGE = 6000;

	private final LichenGrower grower = new LichenGrower(this);

	public CumTraceBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(SEC, false));
	}

	@Override
	protected MapCodec<? extends MultifaceGrowthBlock> getCodec() {
		return CODEC;
	}

	@Override
	public LichenGrower getGrower() {
		return this.grower;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(SEC);
	}

	/** Fraîche à la pose (ou ravivée) : elle sèchera dans cinq minutes. */
	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onBlockAdded(state, world, pos, ancien, notify);
		if (!state.get(SEC) && (!ancien.isOf(this) || ancien.get(SEC))) {
			world.scheduleBlockTick(pos, this, SÉCHAGE);
		}
	}

	/**
	 * Les traces posées avant qu'elles ne sèchent (0.45.4 et avant) n'ont pas de
	 * séchage prévu : le battement au hasard leur en donne un. Les sèches n'en
	 * ont plus besoin.
	 */
	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return !state.get(SEC);
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!world.getBlockTickScheduler().isQueued(pos, this)) {
			world.scheduleBlockTick(pos, this, SÉCHAGE);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.get(SEC)) {
			world.setBlockState(pos, state.with(SEC, true), Block.NOTIFY_ALL);
		}
	}

	/** Fraîche : un flacon vide la recueille. */
	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
	                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(Items.GLASS_BOTTLE) || state.get(SEC)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!world.isClient()) {
			ôter(world, pos, state, hit.getSide().getOpposite());
			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
			}
			donner(player, hand, new ItemStack(ModItems.CUM), stack.isEmpty());
			world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 0.85F);
		}
		return ItemActionResult.success(world.isClient());
	}

	/** Sèche : à main nue, on en détache une écaille. */
	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!state.get(SEC)) {
			return ActionResult.PASS;
		}
		if (!world.isClient()) {
			ôter(world, pos, state, hit.getSide().getOpposite());
			donner(player, Hand.MAIN_HAND, new ItemStack(ModItems.DRIED_CUM), player.getMainHandStack().isEmpty());
			world.playSound(null, pos, SoundEvents.BLOCK_SUSPICIOUS_SAND_BREAK, SoundCategory.BLOCKS, 0.6F, 1.4F);
		}
		return ActionResult.success(world.isClient());
	}

	/** Sèche et cassée : une écaille par face. Fraîche, il ne reste rien. */
	@Override
	public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
	                       @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.afterBreak(world, player, pos, state, blockEntity, tool);
		if (state.get(SEC) && !world.isClient()) {
			int faces = (int) Direction.stream().filter(d -> hasDirection(state, d)).count();
			Block.dropStack(world, pos, new ItemStack(ModItems.DRIED_CUM, Math.max(1, faces)));
		}
	}

	/** Retire la face visée (ou une autre s'il n'y en a pas là) ; la case se vide avec la dernière. */
	private static void ôter(World world, BlockPos pos, BlockState state, Direction face) {
		if (!hasDirection(state, face)) {
			face = Direction.stream().filter(d -> hasDirection(state, d)).findFirst().orElse(face);
		}
		BlockState reste = state.with(getProperty(face), false);
		world.setBlockState(pos, hasAnyDirection(reste) ? reste : Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
	}

	private static void donner(PlayerEntity player, Hand hand, ItemStack objet, boolean mainVide) {
		if (mainVide) {
			player.setStackInHand(hand, objet);
		} else if (!player.getInventory().insertStack(objet)) {
			player.dropItem(objet, false);
		}
	}
}
