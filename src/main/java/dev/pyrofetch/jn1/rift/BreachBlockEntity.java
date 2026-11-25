package dev.pyrofetch.jn1.rift;

import dev.pyrofetch.jn1.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ce que fait la brèche, une fois par seconde : tenir sa faille ouverte, et
 * contaminer alentour.
 *
 * <p>Elle n'a rien à enregistrer. La faille est une entité qui se sauvegarde
 * seule, et qui retient elle-même à quelle brèche elle appartient.
 */
public class BreachBlockEntity extends BlockEntity {
	/** Une fois par seconde. */
	public static final int PÉRIODE = 20;
	/**
	 * Le temps laissé aux entités du chunk pour se charger.
	 *
	 * <p>Elles arrivent après les blocs. Sans ce délai, une brèche qui vient de
	 * se charger ne voit pas encore sa faille, en rouvre une, et l'on se retrouve
	 * avec deux failles l'une dans l'autre.
	 */
	private static final int PATIENCE = 100;
	/** Jusqu'où l'on est contaminé, en blocs, en tous sens. */
	public static final double RAYON = 11.0;

	private int âge;
	/** La hauteur de sa faille, en blocs : grande par défaut (0.83.8), réglable en créatif. */
	private float taille = TAILLES[2];
	/** Les tailles qu'on fait défiler d'un clic, en créatif. */
	public static final float[] TAILLES = {RiftEntity.TAILLE_DÉFAUT, 6.0F, 11.0F, 16.0F};

	public float taille() {
		return this.taille;
	}

	/** Passe à la taille suivante ; rend la nouvelle. */
	public float tailleSuivante() {
		int i = 0;
		while (i < TAILLES.length && TAILLES[i] <= this.taille + 0.01F) {
			i++;
		}
		this.taille = TAILLES[i % TAILLES.length];
		this.markDirty();
		return this.taille;
	}

	@Override
	protected void writeNbt(net.minecraft.nbt.NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putFloat("RiftSize", this.taille);
	}

	@Override
	protected void readNbt(net.minecraft.nbt.NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		if (nbt.contains("RiftSize")) {
			this.taille = nbt.getFloat("RiftSize");
		}
	}

	public BreachBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BREACH, pos, state);
	}

	public static void tick(World monde, BlockPos pos, BlockState état, BreachBlockEntity brèche) {
		brèche.âge++;
		if (brèche.âge % PÉRIODE != 0 || !(monde instanceof ServerWorld serveur)) {
			return;
		}
		if (brèche.âge >= PATIENCE && serveur.shouldTickEntity(pos)) {
			entretenir(serveur, pos, brèche.taille);
		}
		Contamination.exposer(serveur, Vec3d.ofCenter(pos.up()), RAYON, PÉRIODE);
	}

	/** Une faille, et une seule, au-dessus du socle. */
	private static void entretenir(ServerWorld monde, BlockPos pos, float taille) {
		BlockPos pied = pos.up();
		// On cherche par le pied : une grande faille déborde largement de la case.
		List<RiftEntity> failles = monde.getEntitiesByClass(RiftEntity.class,
				new Box(pied).expand(0.5, 1.0, 0.5), faille -> !faille.seReferme()
						&& faille.getBlockPos().getX() == pied.getX() && faille.getBlockPos().getZ() == pied.getZ()
						&& Math.abs(faille.getY() - pied.getY()) < 1.0);
		if (failles.isEmpty()) {
			RiftEntity faille = RiftSpawner.ouvrir(monde, pied);
			if (faille != null) {
				faille.ancrer(pos);
				faille.setTaille(taille);
			}
			return;
		}
		failles.getFirst().ancrer(pos);
		if (Math.abs(failles.getFirst().taille() - taille) > 0.01F) {
			failles.getFirst().setTaille(taille);
		}
		// Un doublon, né d'un chargement trop pressé : il se referme.
		for (int i = 1; i < failles.size(); i++) {
			failles.get(i).refermer();
		}
	}
}
