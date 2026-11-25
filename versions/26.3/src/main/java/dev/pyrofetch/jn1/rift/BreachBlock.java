package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
public class BreachBlock extends Block implements EntityBlock {

	public BreachBlock(Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BreachBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
	                                                              BlockEntityType<T> type) {
		if (world.isClientSide() || type != ModBlockEntities.BREACH) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<BreachBlockEntity>) BreachBlockEntity::tick;
	}

	/**
	 * En créatif, un clic droit fait défiler la taille de sa faille : 2,6, 6, 11
	 * puis 16 blocs de haut (0.83.8).
	 */
	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
	                                                net.minecraft.world.entity.player.Player player,
	                                                net.minecraft.world.phys.BlockHitResult hit) {
		if (!player.isCreative()) {
			return net.minecraft.world.InteractionResult.PASS;
		}
		if (!world.isClientSide() && world.getBlockEntity(pos) instanceof BreachBlockEntity brèche) {
			float taille = brèche.tailleSuivante();
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jn1.breach.size",
					String.format(java.util.Locale.ROOT, "%.1f", taille)));
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	/** Des poussières de glyphes qui montent de la fente. */
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
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
