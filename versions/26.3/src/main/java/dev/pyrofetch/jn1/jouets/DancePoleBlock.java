package dev.pyrofetch.jn1.jouets;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La barre de pole dance : un tube d'acier, un bloc par étage, qui s'empile.
 *
 * <p>Clic droit : la fille la plus proche (la sienne d'abord, à seize blocs)
 * vient y danser — l'animation {@code jn1_pole}, jouée comme une scène : elle
 * tourne lentement autour de la barre, la main droite accrochée au-dessus
 * d'elle. Encore un clic : elle s'arrête. La main tombe en {@link #MAIN_X},
 * {@link #MAIN_Z} pixels du modèle ({@code tools/gen_jouets.py} l'écrit) :
 * la fille est posée pour que ce point soit sur la barre.
 */
public class DancePoleBlock extends Block {
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).
	public static final String SCÈNE = "jn1_pole";
	/** Où passe la barre, en pixels du modèle : sous la main droite levée. */
	public static final double MAIN_X = -8.07;
	public static final double MAIN_Z = -1.95;
	/**
	 * Les filles dont la main levée n'est pas celle de Jenny (bras d'une autre
	 * longueur) : x, z en pixels du modèle. Aucune pour l'instant.
	 */
	private static final java.util.Map<String, double[]> MAINS = java.util.Map.of();
	private static final VoxelShape FORME = Block.box(7, 0, 7, 9, 16, 9);
	private static final double PORTÉE = 16.0;

	public DancePoleBlock(Properties settings) {
		super(settings);
	}


	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return FORME;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player joueur, BlockHitResult hit) {
		if (!(world instanceof ServerLevel monde)) {
			return InteractionResult.SUCCESS;
		}
		// Le pied de la barre : c'est là qu'on danse.
		BlockPos pied = pos;
		while (monde.getBlockState(pied.below()).is(this)) {
			pied = pied.below();
		}
		CompanionEntity dansante = monde.getEntitiesOfClass(CompanionEntity.class, new AABB(pied).inflate(1.5, 0.5, 1.5),
				f -> SCÈNE.equals(f.getScene())).stream().findFirst().orElse(null);
		if (dansante != null) {
			dansante.stopScene();
			return InteractionResult.SUCCESS;
		}
		Vec3 centre = Vec3.atBottomCenterOf(pied);
		CompanionEntity fille = monde.getEntitiesOfClass(CompanionEntity.class, new AABB(pied).inflate(PORTÉE),
						f -> !f.hasScene() && !f.isCuffed() && !f.estEnVeille() && f.peutQuitterSaPlace() && f.joueLesScènes())
				.stream()
				.min(Comparator.comparingDouble(f -> joueur.getUUID().equals(f.getCompanionUuid())
						? f.distanceToSqr(centre) - 10000 : f.distanceToSqr(centre)))
				.orElse(null);
		if (fille == null) {
			joueur.sendOverlayMessage(Component.translatable("block.jn1.dance_pole.nobody"));
			return InteractionResult.SUCCESS;
		}
		danser(fille, pied, joueur.position());
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "pole");
		return InteractionResult.SUCCESS;
	}

	/** La barre cassée à son pied : la danseuse s'arrête (la scène n'a pas de fin). */
	// 26.3 : `onRemove` est devenu `affectNeighborsAfterRemoval`, appelé quand le
	// bloc a changé (côté serveur seulement).
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel monde, BlockPos pos, boolean bougé) {
		if (!monde.getBlockState(pos.below()).is(this)) {
			for (CompanionEntity dansante : monde.getEntitiesOfClass(CompanionEntity.class,
					new AABB(pos).inflate(1.5, 0.5, 1.5), f -> SCÈNE.equals(f.getScene()))) {
				dansante.stopScene();
			}
		}
		super.affectNeighborsAfterRemoval(state, monde, pos, bougé);
	}

	/** Pose la fille contre la barre, tournée vers {@code regard}, et la fait danser. */
	public static void danser(CompanionEntity fille, BlockPos pied, Vec3 regard) {
		Vec3 centre = Vec3.atBottomCenterOf(pied);
		float lacet = (float) Math.toDegrees(Math.atan2(regard.z - centre.z, regard.x - centre.x)) - 90.0F;
		// Du modèle au monde, comme CompanionEntity.calculateBoundingBox : x
		// inversé, puis un tour de (180 - lacet) autour de y.
		double angle = Math.toRadians(180.0F - lacet);
		double[] main = MAINS.getOrDefault(fille.kind().id(), new double[] {MAIN_X, MAIN_Z});
		double bx = -main[0], bz = main[1];
		double échelle = fille.getScale() / 16.0;
		double dx = (bx * Math.cos(angle) + bz * Math.sin(angle)) * échelle;
		double dz = (-bx * Math.sin(angle) + bz * Math.cos(angle)) * échelle;
		fille.stopScene();
		fille.getNavigation().stop();
		fille.setMode(JennyMode.IDLE);
		fille.snapTo(centre.x - dx, pied.getY(), centre.z - dz, lacet, 0.0F);
		fille.setYBodyRot(lacet);
		fille.setYHeadRot(lacet);
		fille.setAnchorPos(fille.blockPosition());
		fille.playScene(SCÈNE);
	}
}
