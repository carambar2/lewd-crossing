package dev.pyrofetch.jn1.lift;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Un bloc de plateforme d'ascenseur (voir {@link Platforms}). Accroupi, un
 * clic droit ouvre son interface ({@link PlatformScreenHandler}) : on y
 * dépose un bloc dont il prendra l'apparence, et on l'assemble à ses voisins.
 * Le clic est pris par {@link LiftHandler} : accroupi un objet en main, le jeu
 * ne consulte pas le bloc.
 */
public class PlatformBlock extends BaseEntityBlock implements EntityBlock {

	public PlatformBlock(Properties settings) {
		super(settings);
	}

	/** Un modèle ordinaire : le client le remplace par un modèle camouflé ({@code CamoModel}). */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlatformBlockEntity(pos, state);
	}
}
