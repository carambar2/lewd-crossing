package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleItem;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * La capsule, en vraie sphère (0.56.8).
 *
 * <p>Un modèle d'objet ne fait que des boîtes : la sphère de
 * {@code gen_capsule.py} empilait onze gradins, qu'on voyait en marches. Elle
 * est désormais tracée ici, comme le lidar : 40 méridiens, des parallèles tous
 * les 6°, plus ceux des bords du bandeau, la normale de chaque sommet celle de
 * la sphère (l'éclairage glisse au lieu de marquer des facettes). Mêmes
 * proportions que l'ancien modèle : rayon 7 pixels, centrée, le joint à mi-hauteur,
 * le bouton au nord. Vide, le capot bâille de {@link #LEVÉE} pixel et l'on voit
 * l'intérieur sombre ; pleine, elle est close.
 *
 * <p>Vide, les couleurs sont celles des planches retouchées dans {@code art/}
 * ({@code item/capsule_*}), en moyenne. Pleine, elle prend celles de l'œuf
 * d'apparition de sa captive (0.56.9) : le haut à la couleur
 * principale, le bas à la seconde ; sans œuf connu, elle garde les siennes.
 * La capsule en vol passe par le même rendu (son entité dessine son objet).
 */
public class CapsuleItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	/** Un carré blanc : les couleurs viennent des sommets. */
	private static final Identifier BLANC = JN1.id("textures/entity/gaster/lueur.png");

	private static final float CENTRE = 8.0F;
	private static final float RAYON = 7.0F;
	private static final float JOINT = 8.0F;
	/** Demi-hauteur du bandeau noir, de part et d'autre du joint. */
	private static final float BANDEAU = 0.6F;
	private static final float LEVÉE = 0.8F;
	private static final int MÉRIDIENS = 40;
	private static final float PAS_PARALLÈLES = 6.0F;

	private static final int ROUGE = 0xFFC5393F;
	private static final int BLANC_COQUE = 0xFFE1E1E7;
	private static final int NOIR = 0xFF211F27;
	private static final int INTÉRIEUR = 0xFF15141A;
	private static final int BOUTON = 0xFFEDEDF2;

	@Override
	public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière, int superposition) {
		boolean ouverte = !CapsuleItem.isFull(stack);
		int haut = ROUGE;
		int bas = BLANC_COQUE;
		dev.pyrofetch.jn1.capsule.CapsuleContents contenu = stack.get(dev.pyrofetch.jn1.registry.ModComponents.CAPSULE_CONTENTS);
		if (contenu != null) {
			net.minecraft.item.SpawnEggItem œuf = œuf(contenu);
			if (œuf != null) {
				haut = 0xFF000000 | œuf.getColor(0);
				bas = 0xFF000000 | œuf.getColor(1);
			}
		}
		VertexConsumer t = tampons.getBuffer(RenderLayer.getEntityCutoutNoCull(BLANC));
		matrices.push();
		matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		MatrixStack.Entry e = matrices.peek();

		// Les parallèles : un pas régulier, plus les bords du bandeau et le joint.
		List<Float> latitudes = new ArrayList<>();
		for (float a = -90.0F; a < 90.0F; a += PAS_PARALLÈLES) {
			latitudes.add(a);
		}
		latitudes.add(90.0F);
		for (float y : new float[] {JOINT - BANDEAU, JOINT, JOINT + BANDEAU}) {
			latitudes.add((float) Math.toDegrees(Math.asin((y - CENTRE) / RAYON)));
		}
		latitudes.sort(Float::compare);

		for (int i = 0; i + 1 < latitudes.size(); i++) {
			float a = latitudes.get(i);
			float b = latitudes.get(i + 1);
			if (b - a < 0.01F) {
				continue;
			}
			float milieu = CENTRE + RAYON * (float) Math.sin(Math.toRadians((a + b) / 2));
			int couleur = milieu > JOINT + BANDEAU ? haut : milieu < JOINT - BANDEAU ? bas : NOIR;
			float dy = ouverte && milieu > JOINT ? LEVÉE : 0.0F;
			for (int m = 0; m < MÉRIDIENS; m++) {
				double t0 = 2 * Math.PI * m / MÉRIDIENS;
				double t1 = 2 * Math.PI * (m + 1) / MÉRIDIENS;
				sommet(t, e, a, t0, dy, couleur, lumière, superposition);
				sommet(t, e, a, t1, dy, couleur, lumière, superposition);
				sommet(t, e, b, t1, dy, couleur, lumière, superposition);
				sommet(t, e, b, t0, dy, couleur, lumière, superposition);
			}
		}

		// Ouverte : les deux coques montrent leur tranche, sombre.
		if (ouverte) {
			disque(t, e, CENTRE, JOINT, CENTRE, RAYON, -1, INTÉRIEUR, lumière, superposition, 'y');
			disque(t, e, CENTRE, JOINT + LEVÉE, CENTRE, RAYON, 1, INTÉRIEUR, lumière, superposition, 'y');
		}

		// Le bouton, au nord, sur le joint : un anneau noir, un rond blanc devant.
		float surface = CENTRE - (float) Math.sqrt(RAYON * RAYON - 2.1F * 2.1F);
		disque(t, e, CENTRE, JOINT, surface - 0.45F, 2.1F, -1, NOIR, lumière, superposition, 'z');
		cylindre(t, e, CENTRE, JOINT, surface - 0.45F, surface + 0.2F, 2.1F, NOIR, lumière, superposition);
		disque(t, e, CENTRE, JOINT, surface - 0.55F, 1.25F, -1, BOUTON, lumière, superposition, 'z');
		cylindre(t, e, CENTRE, JOINT, surface - 0.55F, surface - 0.45F, 1.25F, BOUTON, lumière, superposition);

		matrices.pop();
	}

	/** L'œuf d'apparition du type de la captive, lu dans son état sauvegardé ({@code id}). */
	@org.jetbrains.annotations.Nullable
	private static net.minecraft.item.SpawnEggItem œuf(dev.pyrofetch.jn1.capsule.CapsuleContents contenu) {
		Identifier id = Identifier.tryParse(contenu.entityData().getString("id"));
		if (id == null) {
			return null;
		}
		return net.minecraft.registry.Registries.ENTITY_TYPE.getOrEmpty(id)
				.map(net.minecraft.item.SpawnEggItem::forEntity).orElse(null);
	}

	private static void sommet(VertexConsumer t, MatrixStack.Entry e, float latitude, double longitude, float dy,
	                           int couleur, int lumière, int superposition) {
		double φ = Math.toRadians(latitude);
		float nx = (float) (Math.cos(φ) * Math.cos(longitude));
		float ny = (float) Math.sin(φ);
		float nz = (float) (Math.cos(φ) * Math.sin(longitude));
		t.vertex(e, CENTRE + RAYON * nx, CENTRE + RAYON * ny + dy, CENTRE + RAYON * nz).color(couleur)
				.texture(0.5F, 0.5F).overlay(superposition).light(lumière).normal(e, nx, ny, nz);
	}

	/** Un disque plein, perpendiculaire à {@code axe} ('y' ou 'z'), tourné vers {@code sens}. */
	private static void disque(VertexConsumer t, MatrixStack.Entry e, float cx, float cy, float cz, float r, int sens,
	                           int couleur, int lumière, int superposition, char axe) {
		for (int m = 0; m < MÉRIDIENS; m++) {
			double t0 = 2 * Math.PI * m / MÉRIDIENS;
			double t1 = 2 * Math.PI * (m + 1) / MÉRIDIENS;
			float[][] pts = {{0, 0}, {(float) Math.cos(t0) * r, (float) Math.sin(t0) * r},
					{(float) Math.cos(t1) * r, (float) Math.sin(t1) * r}, {0, 0}};
			for (float[] p : pts) {
				float x = cx + p[0];
				float y = axe == 'y' ? cy : cy + p[1];
				float z = axe == 'y' ? cz + p[1] : cz;
				t.vertex(e, x, y, z).color(couleur).texture(0.5F, 0.5F).overlay(superposition).light(lumière)
						.normal(e, 0, axe == 'y' ? sens : 0, axe == 'z' ? sens : 0);
			}
		}
	}

	/** Le flanc d'un cylindre d'axe z, de {@code z0} à {@code z1}. */
	private static void cylindre(VertexConsumer t, MatrixStack.Entry e, float cx, float cy, float z0, float z1, float r,
	                             int couleur, int lumière, int superposition) {
		for (int m = 0; m < MÉRIDIENS; m++) {
			double t0 = 2 * Math.PI * m / MÉRIDIENS;
			double t1 = 2 * Math.PI * (m + 1) / MÉRIDIENS;
			double[] angles = {t0, t1, t1, t0};
			float[] zs = {z0, z0, z1, z1};
			for (int k = 0; k < 4; k++) {
				float nx = (float) Math.cos(angles[k]);
				float ny = (float) Math.sin(angles[k]);
				t.vertex(e, cx + nx * r, cy + ny * r, zs[k]).color(couleur).texture(0.5F, 0.5F)
						.overlay(superposition).light(lumière).normal(e, nx, ny, 0);
			}
		}
	}
}
