package dev.pyrofetch.jn1.lift;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Un bloc de plateforme d'ascenseur (voir {@link Platforms}). Accroupi, un
 * clic droit ouvre son interface ({@link PlatformScreenHandler}) : on y
 * dépose un bloc dont il prendra l'apparence, et on l'assemble à ses voisins.
 * Le clic est pris par {@link LiftHandler} : accroupi un objet en main, le jeu
 * ne consulte pas le bloc.
 */
public class PlatformBlock extends BlockWithEntity implements BlockEntityProvider {
	public static final MapCodec<PlatformBlock> CODEC = createCodec(PlatformBlock::new);

	public PlatformBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	/** Un modèle ordinaire : le client le remplace par un modèle camouflé ({@code CamoModel}). */
	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PlatformBlockEntity(pos, state);
	}
}
