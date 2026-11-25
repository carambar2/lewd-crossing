package dev.pyrofetch.jn1.rift;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * La brèche : le socle fendu d'un site de confinement, d'où sort sa faille.
 *
 * <p>On ne referme pas une brèche. Le socle est indestructible, et tant qu'il
 * est là une faille se tient au-dessus de lui : si l'entité manque — tuée par
 * commande, perdue —, la brèche en rouvre une. Elle contamine aussi qui
 * l'approche sans la tenue (voir {@link Contamination}).
 *
 * <p>C'est un bloc plutôt qu'une entité enregistrée dans le gabarit : il se
 * voit quand on retouche le site au structure block, et il survit à tout ce
 * qui peut arriver à une entité.
 */
public class BreachBlock extends Block implements BlockEntityProvider {
	public static final MapCodec<BreachBlock> CODEC = createCodec(BreachBlock::new);

	public BreachBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BreachBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
	                                                              BlockEntityType<T> type) {
		if (world.isClient() || type != ModBlockEntities.BREACH) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<BreachBlockEntity>) BreachBlockEntity::tick;
	}

	/**
	 * En créatif, un clic droit fait défiler la taille de sa faille : 2,6, 6, 11
	 * puis 16 blocs de haut (0.83.8).
	 */
	@Override
	protected net.minecraft.util.ActionResult onUse(BlockState state, World world, BlockPos pos,
	                                                net.minecraft.entity.player.PlayerEntity player,
	                                                net.minecraft.util.hit.BlockHitResult hit) {
		if (!player.isCreative()) {
			return net.minecraft.util.ActionResult.PASS;
		}
		if (!world.isClient() && world.getBlockEntity(pos) instanceof BreachBlockEntity brèche) {
			float taille = brèche.tailleSuivante();
			player.sendMessage(net.minecraft.text.Text.translatable("message.jn1.breach.size",
					String.format(java.util.Locale.ROOT, "%.1f", taille)), true);
		}
		return net.minecraft.util.ActionResult.success(world.isClient());
	}

	/** Des poussières de glyphes qui montent de la fente. */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(2) != 0) {
			return;
		}
		world.addParticle(ModParticles.GLYPH_MOTE,
				pos.getX() + 0.3 + random.nextDouble() * 0.4,
				pos.getY() + 1.02,
				pos.getZ() + 0.3 + random.nextDouble() * 0.4,
				0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
	}
}
