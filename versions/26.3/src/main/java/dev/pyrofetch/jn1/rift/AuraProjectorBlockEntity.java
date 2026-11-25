package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.AuraEntity;
import dev.pyrofetch.jn1.registry.ModBlockEntities;
import dev.pyrofetch.jn1.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ce que retient un projecteur holographique : où il a fait paraître Aura.
 *
 * <p>Il la fait paraître une fois, en veille, au-dessus de lui. Retenir la
 * <em>position</em> plutôt qu'un simple « c'est fait » compte : le projecteur
 * vit dans un bâtiment conçu en jeu puis sauvé au structure block, et la
 * génération le recopie ailleurs avec ses données. À une autre place, il n'est
 * plus celui qui l'a fait paraître — il en fait paraître une nouvelle. À sa
 * place d'origine, il n'en refait pas.
 */
public class AuraProjectorBlockEntity extends BlockEntity {
	@Nullable
	private BlockPos paruEn;

	public AuraProjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AURA_PROJECTOR, pos, state);
	}

	public static void tick(Level monde, BlockPos pos, BlockState état, AuraProjectorBlockEntity projecteur) {
		if (monde.getGameTime() % 20 != 0 || pos.equals(projecteur.paruEn) || !(monde instanceof ServerLevel serveur)) {
			return;
		}
		AuraEntity aura = ModEntities.AURA.create(serveur, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (aura == null) {
			return;
		}
		// Posée sur le disque, qui n'a que trois pixels de haut.
		aura.snapTo(pos.getX() + 0.5, pos.getY() + AuraProjectorBlock.HAUTEUR, pos.getZ() + 0.5,
				monde.getRandom().nextFloat() * 360.0F, 0.0F);
		if (!serveur.noCollision(aura)) {
			return;  // quelque chose l'occupe : on réessaiera à la seconde suivante
		}
		aura.setDormant(true);
		aura.setAnchorPos(pos.above());
		serveur.addFreshEntity(aura);
		projecteur.paruEn = pos.immutable();
		projecteur.setChanged();
		JN1.LOGGER.info("Aura projetée en veille, en {}.", pos.toShortString());
	}

	@Override
	protected void loadAdditional(ValueInput nbt) {
		super.loadAdditional(nbt);
		this.paruEn = nbt.getLong("Paru").map(BlockPos::of).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput nbt) {
		super.saveAdditional(nbt);
		if (this.paruEn != null) {
			nbt.putLong("Paru", this.paruEn.asLong());
		}
	}
}
