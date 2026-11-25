package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * L'entité du terminal : vide.
 *
 * <p>Elle ne porte aucune donnée — la station est dans l'état de bloc — et
 * n'existe que pour que le client reçoive un battement par terminal chargé.
 * C'est là qu'il vérifie si la radio doit jouer (voir
 * {@link dev.pyrofetch.jn1.terminal.Radio}).
 */
public class ComputerBlockEntity extends BlockEntity {
	public ComputerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COMPUTER, pos, state);
	}
}
