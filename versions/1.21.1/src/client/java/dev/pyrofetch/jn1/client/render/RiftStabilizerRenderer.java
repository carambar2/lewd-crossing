package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.rift.RiftEntity;
import dev.pyrofetch.jn1.rift.RiftStabilizerBlock;
import dev.pyrofetch.jn1.rift.RiftStabilizerBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Comparator;

/**
 * Le faisceau du stabilisateur : de sa pointe vers la faille la plus proche (24
 * blocs au plus), un trait de lumière qui vacille — un cœur blanc, un halo
 * cyan, deux rubans croisés pour qu'on le voie sous tous les angles. Rien ne
 * passe par le serveur : c'est un décor.
 */
public class RiftStabilizerRenderer implements BlockEntityRenderer<RiftStabilizerBlockEntity> {
	private static final double PORTÉE = 24.0;

	public RiftStabilizerRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(RiftStabilizerBlockEntity stabilisateur, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière, int recouvrement) {
		var monde = stabilisateur.getWorld();
		if (monde == null) {
			return;
		}
		var pos = stabilisateur.getPos();
		Direction sens = stabilisateur.getCachedState().get(RiftStabilizerBlock.FACING);
		Vec3d centre = Vec3d.ofCenter(pos);
		Vec3d pointe = centre.add(Vec3d.of(sens.getVector()).multiply(0.47));
		RiftEntity faille = monde.getEntitiesByClass(RiftEntity.class, new Box(pos).expand(PORTÉE), f -> f.ouverture(tickDelta) > 0.3F)
				.stream().min(Comparator.comparingDouble(f -> f.squaredDistanceTo(pointe))).orElse(null);
		if (faille == null) {
			return;
		}
		Vec3d lieu = faille.getLerpedPos(tickDelta);
		double y = MathHelper.clamp(pointe.y, lieu.y + 0.5, lieu.y + faille.getHeight() - 0.5);
		float temps = monde.getTime() + tickDelta;
		long graine = pos.asLong();
		// La cible tremble un peu sur la faille : le faisceau « cherche » sa prise.
		double tremble = 0.15 * Math.sin(temps * 0.31 + (graine & 7));
		Vec3d cible = new Vec3d(lieu.x + tremble, y + 0.2 * Math.sin(temps * 0.23 + (graine & 15)), lieu.z - tremble);

		Vec3d a = pointe.subtract(Vec3d.of(pos));
		Vec3d b = cible.subtract(Vec3d.of(pos));
		Vec3d axe = b.subtract(a);
		if (axe.lengthSquared() < 1.0E-4) {
			return;
		}
		Vec3d d = axe.normalize();
		Vec3d p1 = Math.abs(d.y) > 0.95 ? d.crossProduct(new Vec3d(1, 0, 0)).normalize() : d.crossProduct(new Vec3d(0, 1, 0)).normalize();
		Vec3d p2 = d.crossProduct(p1).normalize();

		float vacille = 0.55F + 0.3F * MathHelper.sin(temps * 0.9F + (graine & 31)) + 0.15F * MathHelper.sin(temps * 2.7F);
		VertexConsumer tampon = tampons.getBuffer(RenderLayer.getLightning());
		Matrix4f m = matrices.peek().getPositionMatrix();
		for (Vec3d p : new Vec3d[] {p1, p2}) {
			ruban(tampon, m, a, b, p, 0.16, 0.30F, 0.90F, 1.0F, 0.22F * vacille);   // le halo cyan
			ruban(tampon, m, a, b, p, 0.05, 0.92F, 1.0F, 1.0F, 0.75F * vacille);   // le cœur blanc
		}
	}

	private static void ruban(VertexConsumer t, Matrix4f m, Vec3d a, Vec3d b, Vec3d p, double demi,
	                          float r, float g, float bl, float alpha) {
		Vec3d o = p.multiply(demi);
		Vec3d[] coins = {a.add(o), b.add(o), b.subtract(o), a.subtract(o)};
		for (int k = 0; k < 4; k++) {
			t.vertex(m, (float) coins[k].x, (float) coins[k].y, (float) coins[k].z).color(r, g, bl, alpha);
		}
		for (int k = 3; k >= 0; k--) {
			t.vertex(m, (float) coins[k].x, (float) coins[k].y, (float) coins[k].z).color(r, g, bl, alpha);
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(RiftStabilizerBlockEntity stabilisateur) {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return 96;
	}
}
