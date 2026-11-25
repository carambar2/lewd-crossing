package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La tête du Cummy piston : celle du piston collant, sa plaque nappée de cum.
 *
 * <p>Le jeu ne connaît qu'une tête, {@code Blocks.PISTON_HEAD}, et la nomme en
 * dur partout où elle naît ou se reconnaît. Les mixins du piston
 * ({@code PistonBlockMixin}, {@code PistonHeadBlockMixin},
 * {@code PistonBlockEntityMixin} et, côté client,
 * {@code PistonBlockEntityRendererMixin}) y substituent celle-ci quand le
 * socle est le nôtre. Elle est toujours collante : ses deux types ont le même
 * modèle. Un end rod posé sur elle devient un dildo, comme sur le socle rentré.
 */
public class CummyPistonHeadBlock extends PistonHeadBlock {
	public CummyPistonHeadBlock(Properties settings) {
		super(settings);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean avecDonnées) {
		return new ItemStack(ModBlocks.CUMMY_PISTON);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock,
	                              @org.jetbrains.annotations.Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean notify) {
		// 26.3 : le voisin n'est plus désigné par sa position, mais par une orientation.
		super.neighborChanged(state, world, pos, sourceBlock, orientation, notify);
		if (world.getBlockState(pos).is(this)) {
			DildoBlock.convertir(world, pos.relative(state.getValue(FACING)), state.getValue(FACING));
		}
	}
}
