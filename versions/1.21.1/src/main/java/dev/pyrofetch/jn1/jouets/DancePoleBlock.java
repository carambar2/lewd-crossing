package dev.pyrofetch.jn1.jouets;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.JennyMode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Comparator;

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
	public static final MapCodec<DancePoleBlock> CODEC = createCodec(DancePoleBlock::new);
	public static final String SCÈNE = "jn1_pole";
	/** Où passe la barre, en pixels du modèle : sous la main droite levée. */
	public static final double MAIN_X = -8.07;
	public static final double MAIN_Z = -1.95;
	/**
	 * Les filles dont la main levée n'est pas celle de Jenny (bras d'une autre
	 * longueur) : x, z en pixels du modèle. Aucune pour l'instant.
	 */
	private static final java.util.Map<String, double[]> MAINS = java.util.Map.of();
	private static final VoxelShape FORME = Block.createCuboidShape(7, 0, 7, 9, 16, 9);
	private static final double PORTÉE = 16.0;

	public DancePoleBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return FORME;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity joueur, BlockHitResult hit) {
		if (!(world instanceof ServerWorld monde)) {
			return ActionResult.SUCCESS;
		}
		// Le pied de la barre : c'est là qu'on danse.
		BlockPos pied = pos;
		while (monde.getBlockState(pied.down()).isOf(this)) {
			pied = pied.down();
		}
		CompanionEntity dansante = monde.getEntitiesByClass(CompanionEntity.class, new Box(pied).expand(1.5, 0.5, 1.5),
				f -> SCÈNE.equals(f.getScene())).stream().findFirst().orElse(null);
		if (dansante != null) {
			dansante.stopScene();
			return ActionResult.SUCCESS;
		}
		Vec3d centre = Vec3d.ofBottomCenter(pied);
		CompanionEntity fille = monde.getEntitiesByClass(CompanionEntity.class, new Box(pied).expand(PORTÉE),
						f -> !f.hasScene() && !f.isCuffed() && !f.estEnVeille() && f.peutQuitterSaPlace() && f.joueLesScènes())
				.stream()
				.min(Comparator.comparingDouble(f -> joueur.getUuid().equals(f.getCompanionUuid())
						? f.squaredDistanceTo(centre) - 10000 : f.squaredDistanceTo(centre)))
				.orElse(null);
		if (fille == null) {
			joueur.sendMessage(Text.translatable("block.jn1.dance_pole.nobody"), true);
			return ActionResult.SUCCESS;
		}
		danser(fille, pied, joueur.getPos());
		dev.pyrofetch.jn1.progress.Succes.accorder(joueur, "pole");
		return ActionResult.SUCCESS;
	}

	/** La barre cassée à son pied : la danseuse s'arrête (la scène n'a pas de fin). */
	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState nouvel, boolean bougé) {
		if (!state.isOf(nouvel.getBlock()) && world instanceof ServerWorld monde
				&& !monde.getBlockState(pos.down()).isOf(this)) {
			for (CompanionEntity dansante : monde.getEntitiesByClass(CompanionEntity.class,
					new Box(pos).expand(1.5, 0.5, 1.5), f -> SCÈNE.equals(f.getScene()))) {
				dansante.stopScene();
			}
		}
		super.onStateReplaced(state, world, pos, nouvel, bougé);
	}

	/** Pose la fille contre la barre, tournée vers {@code regard}, et la fait danser. */
	public static void danser(CompanionEntity fille, BlockPos pied, Vec3d regard) {
		Vec3d centre = Vec3d.ofBottomCenter(pied);
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
		fille.refreshPositionAndAngles(centre.x - dx, pied.getY(), centre.z - dz, lacet, 0.0F);
		fille.setBodyYaw(lacet);
		fille.setHeadYaw(lacet);
		fille.setAnchorPos(fille.getBlockPos());
		fille.playScene(SCÈNE);
	}
}
