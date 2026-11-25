package dev.pyrofetch.jn1.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pyrofetch.jn1.rift.RiftEntity;
import dev.pyrofetch.jn1.rift.RiftStabilizerBlock;
import dev.pyrofetch.jn1.rift.RiftStabilizerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Comparator;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Le faisceau du stabilisateur : de sa pointe vers la faille la plus proche (24
 * blocs au plus), un trait de lumière qui vacille — un cœur blanc, un halo
 * cyan, deux rubans croisés pour qu'on le voie sous tous les angles. Rien ne
 * passe par le serveur : c'est un décor.
 *
 * <p>En 26.3, le faisceau est calculé au relevé de l'image ({@link État}),
 * puis soumis.
 */
public class RiftStabilizerRenderer implements BlockEntityRenderer<RiftStabilizerBlockEntity, RiftStabilizerRenderer.État> {
	private static final double PORTÉE = 24.0;

	/** Le faisceau à dessiner : ses bouts, dans le repère du bloc, et son vacillement ; rien sans faille. */
	public static class État extends BlockEntityRenderState {
		@Nullable
		Vec3 a;
		@Nullable
		Vec3 b;
		float vacille;
	}

	public RiftStabilizerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public État createRenderState() {
		return new État();
	}

	@Override
	public void extractRenderState(RiftStabilizerBlockEntity stabilisateur, État état, float tickDelta, Vec3 caméra,
	                               ModelFeatureRenderer.@Nullable CrumblingOverlay cassure) {
		BlockEntityRenderer.super.extractRenderState(stabilisateur, état, tickDelta, caméra, cassure);
		état.a = null;
		état.b = null;
		var monde = stabilisateur.getLevel();
		if (monde == null) {
			return;
		}
		BlockPos pos = stabilisateur.getBlockPos();
		Direction sens = stabilisateur.getBlockState().getValue(RiftStabilizerBlock.FACING);
		Vec3 centre = Vec3.atCenterOf(pos);
		Vec3 pointe = centre.add(Vec3.atLowerCornerOf(sens.getUnitVec3i()).scale(0.47));
		RiftEntity faille = monde.getEntitiesOfClass(RiftEntity.class, new AABB(pos).inflate(PORTÉE), f -> f.ouverture(tickDelta) > 0.3F)
				.stream().min(Comparator.comparingDouble(f -> f.distanceToSqr(pointe))).orElse(null);
		if (faille == null) {
			return;
		}
		Vec3 lieu = faille.getPosition(tickDelta);
		double y = Mth.clamp(pointe.y, lieu.y + 0.5, lieu.y + faille.getBbHeight() - 0.5);
		float temps = monde.getGameTime() + tickDelta;
		long graine = pos.asLong();
		// La cible tremble un peu sur la faille : le faisceau « cherche » sa prise.
		double tremble = 0.15 * Math.sin(temps * 0.31 + (graine & 7));
		Vec3 cible = new Vec3(lieu.x + tremble, y + 0.2 * Math.sin(temps * 0.23 + (graine & 15)), lieu.z - tremble);

		Vec3 a = pointe.subtract(Vec3.atLowerCornerOf(pos));
		Vec3 b = cible.subtract(Vec3.atLowerCornerOf(pos));
		if (b.subtract(a).lengthSqr() < 1.0E-4) {
			return;
		}
		état.a = a;
		état.b = b;
		état.vacille = 0.55F + 0.3F * Mth.sin(temps * 0.9F + (graine & 31)) + 0.15F * Mth.sin(temps * 2.7F);
	}

	@Override
	public void submit(État état, PoseStack matrices, SubmitNodeCollector tâches, CameraRenderState caméra) {
		Vec3 a = état.a;
		Vec3 b = état.b;
		if (a == null || b == null) {
			return;
		}
		Vec3 d = b.subtract(a).normalize();
		Vec3 p1 = Math.abs(d.y) > 0.95 ? d.cross(new Vec3(1, 0, 0)).normalize() : d.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 p2 = d.cross(p1).normalize();
		float vacille = état.vacille;
		tâches.submitCustomGeometry(matrices, RenderTypes.lightning(), (entrée, tampon) -> {
			Matrix4f m = entrée.pose();
			for (Vec3 p : new Vec3[] {p1, p2}) {
				ruban(tampon, m, a, b, p, 0.16, 0.30F, 0.90F, 1.0F, 0.22F * vacille);   // le halo cyan
				ruban(tampon, m, a, b, p, 0.05, 0.92F, 1.0F, 1.0F, 0.75F * vacille);   // le cœur blanc
			}
		});
	}

	private static void ruban(VertexConsumer t, Matrix4f m, Vec3 a, Vec3 b, Vec3 p, double demi,
	                          float r, float g, float bl, float alpha) {
		Vec3 o = p.scale(demi);
		Vec3[] coins = {a.add(o), b.add(o), b.subtract(o), a.subtract(o)};
		for (int k = 0; k < 4; k++) {
			t.addVertex(m, (float) coins[k].x, (float) coins[k].y, (float) coins[k].z).setColor(r, g, bl, alpha);
		}
		for (int k = 3; k >= 0; k--) {
			t.addVertex(m, (float) coins[k].x, (float) coins[k].y, (float) coins[k].z).setColor(r, g, bl, alpha);
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
