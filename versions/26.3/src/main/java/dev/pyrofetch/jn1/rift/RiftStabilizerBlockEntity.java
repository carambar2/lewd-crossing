package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vide : elle n'existe que pour que le client dessine le faisceau du stabilisateur. */
public class RiftStabilizerBlockEntity extends BlockEntity {
	public RiftStabilizerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RIFT_STABILIZER, pos, state);
	}
}
