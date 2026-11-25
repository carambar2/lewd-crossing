package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.lab.LidarCloud;
import dev.pyrofetch.jn1.client.lab.LidarTracker;
import dev.pyrofetch.jn1.lab.VoidSwim;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Le lidar en main : un traqueur de mouvement de cinéma, et son écran
 * cathodique dessiné en direct.
 *
 * <p>Le boîtier est un modèle ordinaire ({@code item/lidar_body}). L'écran est
 * tracé dans le creux de son cadre, en vert phosphore sur fond noir : un
 * éventail de cent degrés dont la pointe, en bas, est le joueur et le milieu la
 * direction du regard, quatre arcs de distance, des rayons tous les douze
 * degrés et demi, le front de l'impulsion qui le parcourt, les échos qui
 * s'allument à son passage. Dans les coins du bas, le nombre d'échos et la
 * distance au sol du plus proche ; en haut à droite, sa hauteur. Des lignes de
 * balayage, par-dessus, font le tube.
 *
 * <p><b>La vue 3D</b> remplace l'éventail sur demande (voir
 * {@link LidarTracker}) : le relief relevé autour du joueur, en nuage de points
 * ({@link LidarCloud}), vu de derrière et d'au-dessus, tourné avec le regard ;
 * au milieu le joueur, debout sur trois anneaux de distance tracés à la
 * hauteur de ses pieds ; les échos reliés à ce plan par une tige, qui dit
 * d'un coup d'œil s'ils sont au-dessus ou en dessous. Le front de l'impulsion
 * s'y élargit en anneau et allume les points à son passage. Un écho plus loin
 * que le relevé se tient sur son bord, dans sa vraie direction, en creux.
 *
 * <p><b>La couche compte.</b> Le boîtier passe par une couche « fixe », que le
 * jeu ne dessine qu'à la toute fin. Un écran sur une couche translucide qui
 * n'écrit pas la profondeur était donc peint d'abord, puis recouvert par le
 * boîtier : il n'en restait que le texte, qui l'écrit. L'écran passe par la
 * couche des fonds de texte, translucide mais qui écrit la profondeur.
 *
 * <p>L'écran n'est vivant que dans la main du joueur, à la première personne :
 * vu d'ailleurs — inventaire, main d'un autre, sol —, il ne montre que sa
 * grille et son balayage, puisque les échos sont ceux du joueur.
 */
public class LidarItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	public static final Identifier CORPS = JN1.id("item/lidar_body");

	// L'écran, en pixels du modèle, dans le creux du cadre de sa face avant (+z).
	private static final float X1 = 3.2F;
	private static final float X2 = 11.8F;
	private static final float Y1 = 6.2F;
	private static final float Y2 = 12.8F;
	private static final float Z = 11.03F;
	/** La pointe de l'éventail : en bas de l'écran, au milieu. C'est le joueur. */
	private static final float CX = (X1 + X2) / 2;
	private static final float CY = Y1 + 0.55F;
	private static final float R = 5.4F;
	/** La demi-ouverture de l'éventail, en degrés. */
	private static final float OUVERTURE = 50.0F;

	/** Le joueur, dans la vue 3D : un peu sous le milieu, l'anneau du bas grossissant par la perspective. */
	private static final float V0 = Y1 + 3.4F;
	/** Pixels du modèle par bloc, dans la vue 3D : le relevé entier tient dans l'écran. */
	private static final float ÉCHELLE = 3.5F / LidarCloud.RAYON;
	/**
	 * De combien la vue 3D plonge sur le joueur. À trente degrés, un écho à six
	 * blocs devant et cinq en dessous tombait sur le joueur lui-même.
	 */
	private static final float PLONGÉE = 50.0F * MathHelper.RADIANS_PER_DEGREE;
	/**
	 * Le recul de l'œil, en blocs, pour une perspective légère. Une vue
	 * orthogonale empilait les colonnes de blocs en rayures pleines dès qu'on
	 * regardait le long d'un axe.
	 */
	private static final float RECUL = 80.0F;
	private static final float SIN_P = MathHelper.sin(PLONGÉE);
	private static final float COS_P = MathHelper.cos(PLONGÉE);

	private static final int FOND = 0xFF03120A;
	private static final int PHOSPHORE = 0x3CF090;
	private static final int ÉCHO = 0xDCFFE8;
	private static final int TEXTE = 0xFF78F0A8;
	private static final int PLEIN = LightmapTextureManager.MAX_LIGHT_COORDINATE;

	@Override
	public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
	                   VertexConsumerProvider tampons, int lumière, int superposition) {
		MinecraftClient client = MinecraftClient.getInstance();
		BakedModel corps = client.getBakedModelManager().getModel(CORPS);
		client.getBlockRenderManager().getModelRenderer().render(matrices.peek(),
				tampons.getBuffer(TexturedRenderLayers.getEntityCutout()), null, corps,
				1.0F, 1.0F, 1.0F, lumière, superposition);

		ClientPlayerEntity joueur = client.player;
		boolean vivant = mode.isFirstPerson() && joueur != null && LidarTracker.tenu(joueur);
		float tickDelta = client.getRenderTickCounter().getTickDelta(false);
		matrices.push();
		matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		this.écran(client, matrices, tampons, vivant, tickDelta);
		matrices.pop();
	}

	private void écran(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider tampons,
	                   boolean vivant, float tickDelta) {
		VertexConsumer tampon = tampons.getBuffer(RenderLayer.getTextBackground());
		MatrixStack.Entry repère = matrices.peek();
		Geometrie.aplatUni(repère, tampon, X1, Y1, X2, Y2, Z, FOND, PLEIN);

		ClientPlayerEntity joueur = client.player;
		if (vivant && !VoidSwim.dansLeVide(joueur)) {
			this.sansSignal(client, matrices, tampons, tampon, "item.jn1.lidar.no_signal_short");
			return;
		}

		// Le front de l'impulsion. Hors de la main, il bat sur l'horloge du monde.
		float âge = vivant ? LidarTracker.âgeImpulsion(tickDelta)
				: client.world == null ? 0.0F : (client.world.getTime() % LidarTracker.PÉRIODE) + tickDelta;
		boolean relief = vivant && LidarTracker.vue3D();
		List<BlockPos> échos = vivant ? LidarTracker.échos() : List.of();
		BlockPos proche = relief
				? this.vue3D(joueur, échos, repère, tampon, âge, tickDelta)
				: this.éventail(joueur, échos, repère, tampon, âge, tickDelta);

		String nombre = "";
		String mètres = "";
		String hauteur = "";
		if (vivant) {
			nombre = "◆" + échos.size();
			if (proche == null) {
				mètres = "--m";
				hauteur = "--m";
			} else {
				Vec3d moi = joueur.getLerpedPos(tickDelta);
				mètres = Math.round(horizontale(proche, moi)) + "m";
				hauteur = hauteur(proche.getY() - moi.y);
			}
		}

		// Les lignes de balayage du tube, par-dessus tout le reste.
		for (float y = Y1 + 0.2F; y < Y2; y += 0.5F) {
			Geometrie.aplatUni(repère, tampon, X1, y, X2, y + 0.15F, Z + 0.035F, 0x16000000, PLEIN);
		}

		// Le texte vient en dernier : il passe par une autre couche, et en
		// changer referme celle de l'écran.
		if (vivant) {
			this.texte(client, matrices, tampons, nombre, X1 + 0.3F, Y1 + 1.05F, false);
			this.texte(client, matrices, tampons, mètres, X2 - 0.3F, Y1 + 1.05F, true);
			this.texte(client, matrices, tampons, hauteur, X2 - 0.3F, Y2 - 0.25F, true);
			if (relief) {
				this.texte(client, matrices, tampons, "3D", X1 + 0.3F, Y2 - 0.25F, false);
			}
		}
	}

	/** L'éventail : la grille, le front, le joueur à la pointe, les échos. Rend le plus proche au sol. */
	private BlockPos éventail(ClientPlayerEntity joueur, List<BlockPos> échos, MatrixStack.Entry repère,
	                         VertexConsumer tampon, float âge, float tickDelta) {
		// La grille : quatre arcs, des rayons tous les douze degrés et demi, les
		// deux bords plus marqués.
		for (int i = 1; i <= 4; i++) {
			this.arc(repère, tampon, R * i / 4.0F, i == 4 ? 0.12F : 0.08F, Z + 0.01F,
					couleur(PHOSPHORE, i == 4 ? 0.75F : 0.3F));
		}
		for (int i = -4; i <= 4; i++) {
			boolean bord = Math.abs(i) == 4;
			float a = OUVERTURE * i / 4 * MathHelper.RADIANS_PER_DEGREE;
			Geometrie.traitUni(repère, tampon, CX, CY, CX + R * MathHelper.sin(a), CY + R * MathHelper.cos(a),
					bord ? 0.12F : 0.06F, Z + 0.01F, couleur(PHOSPHORE, bord ? 0.75F : 0.2F), PLEIN);
		}
		float front = âge / LidarTracker.COURSE;
		if (front <= 1.0F) {
			this.arc(repère, tampon, R * front, 0.22F, Z + 0.02F, couleur(PHOSPHORE, 0.85F * (1.0F - front) + 0.15F));
		}
		// Le joueur, à la pointe.
		Geometrie.aplatUni(repère, tampon, CX - 0.35F, CY - 0.2F, CX + 0.35F, CY + 0.2F, Z + 0.03F,
				couleur(ÉCHO, 0.9F), PLEIN);
		return échos.isEmpty() ? null : this.échos(joueur, échos, repère, tampon, âge, tickDelta);
	}

	/** Les échos, placés selon le regard. Rend le plus proche au sol. */
	private BlockPos échos(ClientPlayerEntity joueur, List<BlockPos> échos, MatrixStack.Entry repère,
	                       VertexConsumer tampon, float âge, float tickDelta) {
		BlockPos plusProche = null;
		double auPlusPrès = Double.MAX_VALUE;
		float lacet = joueur.getYaw(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
		Vec3d moi = joueur.getLerpedPos(tickDelta);
		double limite = OUVERTURE * MathHelper.RADIANS_PER_DEGREE;
		for (BlockPos écho : échos) {
			double dx = écho.getX() + 0.5 - moi.x;
			double dz = écho.getZ() + 0.5 - moi.z;
			double distance = horizontale(écho, moi);
			if (distance < auPlusPrès) {
				auPlusPrès = distance;
				plusProche = écho;
			}
			// Dans le repère du regard : devant, et à droite.
			double devant = -dx * MathHelper.sin(lacet) + dz * MathHelper.cos(lacet);
			double droite = -dx * MathHelper.cos(lacet) - dz * MathHelper.sin(lacet);
			double angle = Math.atan2(droite, devant);
			boolean horsChamp = Math.abs(angle) > limite;
			// Hors de l'éventail, l'écho se tient sur son bord, du côté où se tourner.
			angle = MathHelper.clamp(angle, -limite, limite);
			float r = LidarTracker.rayon(distance);
			float x = CX + R * r * (float) Math.sin(angle);
			float y = CY + R * r * (float) Math.cos(angle);

			float éclat = éclat(âge, LidarTracker.front(écho, moi));
			if (horsChamp) {
				éclat *= 0.5F;
			}
			this.écho(repère, tampon, x, y, éclat, false);
		}
		return plusProche;
	}

	/**
	 * La vue 3D : les anneaux, le front, le relief, le joueur, les échos, dans
	 * cet ordre — chacun un peu plus près de la vitre que le précédent. Rend le
	 * plus proche au sol, comme l'éventail.
	 */
	private BlockPos vue3D(ClientPlayerEntity joueur, List<BlockPos> échos, MatrixStack.Entry repère,
	                       VertexConsumer tampon, float âge, float tickDelta) {
		Vue vue = new Vue(joueur.getYaw(tickDelta) * MathHelper.RADIANS_PER_DEGREE, joueur.getLerpedPos(tickDelta));
		float bord = LidarCloud.RAYON;

		// Trois anneaux à hauteur des pieds, et la ligne du regard.
		for (int i = 1; i <= 3; i++) {
			this.anneau(vue, repère, tampon, bord * i / 3, i == 3 ? 0.1F : 0.06F, Z + 0.01F,
					couleur(PHOSPHORE, i == 3 ? 0.6F : 0.25F));
		}
		vue.poserVu(0, 0, 0);
		float ox = vue.x;
		float oy = vue.y;
		vue.poserVu(0, 0, bord);
		Geometrie.traitUni(repère, tampon, ox, oy, vue.x, vue.y, 0.05F, Z + 0.01F, couleur(PHOSPHORE, 0.2F), PLEIN);
		vue.poserVu(-bord, 0, 0);
		float gx = vue.x;
		float gy = vue.y;
		vue.poserVu(bord, 0, 0);
		Geometrie.traitUni(repère, tampon, gx, gy, vue.x, vue.y, 0.05F, Z + 0.01F, couleur(PHOSPHORE, 0.2F), PLEIN);

		float front = âge / LidarTracker.COURSE;
		if (front <= 1.0F) {
			this.anneau(vue, repère, tampon, bord * front, 0.16F, Z + 0.012F,
					couleur(PHOSPHORE, 0.85F * (1.0F - front) + 0.15F));
		}

		// Le relief : un point par bloc, plus sombre au fond, allumé par le front.
		for (long empaqueté : LidarCloud.points()) {
			int bx = BlockPos.unpackLongX(empaqueté);
			int by = BlockPos.unpackLongY(empaqueté);
			int bz = BlockPos.unpackLongZ(empaqueté);
			vue.poser(bx + 0.5, by + 0.5, bz + 0.5);
			if (!dansLÉcran(vue.x, vue.y)) {
				continue;
			}
			float fond = MathHelper.clamp(0.5F - vue.profondeur / (2 * bord), 0.0F, 1.0F);
			float lueur = éclat(âge, Math.min(1.0F, vue.distance / bord));
			float alpha = (0.15F + 0.35F * fond) * (0.4F + 0.6F * lueur);
			Geometrie.aplatUni(repère, tampon, vue.x - 0.04F, vue.y - 0.04F, vue.x + 0.04F, vue.y + 0.04F,
					Z + 0.015F, couleur(PHOSPHORE, alpha), PLEIN);
		}

		// Le joueur : debout au milieu, deux blocs de haut, la tête marquée.
		vue.poserVu(0, 0, 0);
		float px = vue.x;
		float py = vue.y;
		vue.poserVu(0, 1.8, 0);
		Geometrie.traitUni(repère, tampon, px, py, vue.x, vue.y, 0.14F, Z + 0.018F, couleur(ÉCHO, 0.9F), PLEIN);
		Geometrie.aplatUni(repère, tampon, vue.x - 0.13F, vue.y - 0.13F, vue.x + 0.13F, vue.y + 0.13F,
				Z + 0.018F, couleur(ÉCHO, 0.9F), PLEIN);

		// Les échos : à leur place s'ils sont dans le relevé, sur son bord sinon,
		// et reliés au plan des pieds par une tige.
		BlockPos plusProche = null;
		double auPlusPrès = Double.MAX_VALUE;
		for (BlockPos écho : échos) {
			double distance = horizontale(écho, vue.moi);
			if (distance < auPlusPrès) {
				auPlusPrès = distance;
				plusProche = écho;
			}
			double dx = écho.getX() + 0.5 - vue.moi.x;
			double dy = écho.getY() + 0.5 - vue.moi.y;
			double dz = écho.getZ() + 0.5 - vue.moi.z;
			double vraie = Math.sqrt(dx * dx + dy * dy + dz * dz);
			boolean auDelà = vraie > bord;
			if (auDelà) {
				double k = bord / vraie;
				dx *= k;
				dy *= k;
				dz *= k;
			}
			vue.poser(vue.moi.x + dx, vue.moi.y + dy, vue.moi.z + dz);
			float x = vue.x;
			float y = vue.y;
			vue.poser(vue.moi.x + dx, vue.moi.y, vue.moi.z + dz);
			float éclat = éclat(âge, LidarTracker.front(écho, vue.moi));
			if (Math.abs(y - vue.y) > 0.1F) {
				Geometrie.traitUni(repère, tampon, vue.x, vue.y, x, y, 0.07F, Z + 0.018F,
						couleur(PHOSPHORE, 0.6F * éclat), PLEIN);
				Geometrie.aplatUni(repère, tampon, vue.x - 0.22F, vue.y - 0.05F, vue.x + 0.22F, vue.y + 0.05F,
						Z + 0.018F, couleur(PHOSPHORE, 0.7F * éclat), PLEIN);
			}
			this.écho(repère, tampon, x, y, éclat, auDelà);
		}
		return plusProche;
	}

	/** Un écho : un halo et un point, ou seulement un cadre s'il est au-delà du relevé. */
	private void écho(MatrixStack.Entry repère, VertexConsumer tampon, float x, float y, float éclat, boolean enCreux) {
		Geometrie.aplatUni(repère, tampon, x - 0.42F, y - 0.42F, x + 0.42F, y + 0.42F, Z + 0.02F,
				couleur(PHOSPHORE, 0.35F * éclat), PLEIN);
		if (!enCreux) {
			Geometrie.aplatUni(repère, tampon, x - 0.2F, y - 0.2F, x + 0.2F, y + 0.2F, Z + 0.03F,
					couleur(ÉCHO, éclat), PLEIN);
			return;
		}
		int c = couleur(ÉCHO, éclat);
		float e = 0.07F;
		Geometrie.aplatUni(repère, tampon, x - 0.24F, y + 0.24F - e, x + 0.24F, y + 0.24F, Z + 0.03F, c, PLEIN);
		Geometrie.aplatUni(repère, tampon, x - 0.24F, y - 0.24F, x + 0.24F, y - 0.24F + e, Z + 0.03F, c, PLEIN);
		Geometrie.aplatUni(repère, tampon, x - 0.24F, y - 0.24F, x - 0.24F + e, y + 0.24F, Z + 0.03F, c, PLEIN);
		Geometrie.aplatUni(repère, tampon, x + 0.24F - e, y - 0.24F, x + 0.24F, y + 0.24F, Z + 0.03F, c, PLEIN);
	}

	/** L'éclat d'un point que le front atteint à cette fraction de sa course : vif au passage, puis il s'éteint. */
	private static float éclat(float âge, float fraction) {
		float depuis = âge - fraction * LidarTracker.COURSE;
		if (depuis < 0) {
			depuis += LidarTracker.PÉRIODE;
		}
		return Math.max(0.2F, (float) Math.exp(-depuis / LidarTracker.RÉMANENCE));
	}

	/** La distance au sol, du joueur au milieu du bloc de l'écho. */
	private static double horizontale(BlockPos écho, Vec3d moi) {
		double dx = écho.getX() + 0.5 - moi.x;
		double dz = écho.getZ() + 0.5 - moi.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** La hauteur lue au coin : au-dessus, en dessous, ou de plain-pied. */
	private static String hauteur(double dy) {
		long mètres = Math.round(dy);
		if (mètres == 0) {
			return "±0m";
		}
		return (mètres > 0 ? "▲" : "▼") + Math.abs(mètres) + "m";
	}

	private static boolean dansLÉcran(float x, float y) {
		return x > X1 + 0.1F && x < X2 - 0.1F && y > Y1 + 0.1F && y < Y2 - 0.1F;
	}

	/** Un arc de l'éventail, d'un bord à l'autre en passant par le haut. */
	private void arc(MatrixStack.Entry repère, VertexConsumer tampon, float rayon, float épaisseur, float z, int couleur) {
		if (rayon < 0.05F) {
			return;
		}
		int segments = 20;
		for (int i = 0; i < segments; i++) {
			float a = (-OUVERTURE + 2 * OUVERTURE * i / segments) * MathHelper.RADIANS_PER_DEGREE;
			float b = (-OUVERTURE + 2 * OUVERTURE * (i + 1) / segments) * MathHelper.RADIANS_PER_DEGREE;
			Geometrie.traitUni(repère, tampon,
					CX + rayon * MathHelper.sin(a), CY + rayon * MathHelper.cos(a),
					CX + rayon * MathHelper.sin(b), CY + rayon * MathHelper.cos(b),
					épaisseur, z, couleur, PLEIN);
		}
	}

	/** Un anneau de la vue 3D, à hauteur des pieds, de ce rayon en blocs. */
	private void anneau(Vue vue, MatrixStack.Entry repère, VertexConsumer tampon, float rayon, float épaisseur,
	                    float z, int couleur) {
		if (rayon < 0.3F) {
			return;
		}
		int segments = 32;
		vue.poserVu(0, 0, rayon);
		float ax = vue.x;
		float ay = vue.y;
		for (int i = 1; i <= segments; i++) {
			float a = MathHelper.TAU * i / segments;
			vue.poserVu(rayon * MathHelper.sin(a), 0, rayon * MathHelper.cos(a));
			if (dansLÉcran(ax, ay) && dansLÉcran(vue.x, vue.y)) {
				Geometrie.traitUni(repère, tampon, ax, ay, vue.x, vue.y, épaisseur, z, couleur, PLEIN);
			}
			ax = vue.x;
			ay = vue.y;
		}
	}

	/**
	 * La projection de la vue 3D : tournée avec le lacet du joueur, plongeant de
	 * {@link #PLONGÉE} sur lui, d'une perspective légère ({@link #RECUL}). Le devant monte vers le haut de
	 * l'écran, comme dans l'éventail ; la hauteur aussi, d'où la tige des échos
	 * pour lever l'ambiguïté.
	 */
	private static final class Vue {
		final Vec3d moi;
		private final float sin;
		private final float cos;
		/** Le dernier point posé, sur l'écran. */
		float x;
		float y;
		/** Sa profondeur, en blocs : positive au fond. */
		float profondeur;
		/** Sa distance vraie au joueur, en blocs. */
		float distance;

		Vue(float lacet, Vec3d moi) {
			this.moi = moi;
			this.sin = MathHelper.sin(lacet);
			this.cos = MathHelper.cos(lacet);
		}

		/** Pose un point du monde. */
		void poser(double wx, double wy, double wz) {
			double dx = wx - this.moi.x;
			double dz = wz - this.moi.z;
			// Dans le repère du regard : devant, et à droite — comme l'éventail.
			double devant = -dx * this.sin + dz * this.cos;
			double droite = -dx * this.cos - dz * this.sin;
			this.poserVu(droite, wy - this.moi.y, devant);
		}

		/** Pose un point déjà dans le repère du regard : à droite, en haut, devant, en blocs. */
		void poserVu(double droite, double haut, double devant) {
			this.profondeur = (float) (devant * COS_P - haut * SIN_P);
			float fuite = RECUL / (RECUL + this.profondeur);
			this.x = CX + ÉCHELLE * fuite * (float) droite;
			this.y = V0 + ÉCHELLE * fuite * (float) (haut * COS_P + devant * SIN_P);
			this.distance = (float) Math.sqrt(droite * droite + haut * haut + devant * devant);
		}
	}

	/** Une lecture dans un coin, calée à gauche ou à droite, en petits caractères, sous la hauteur y. */
	private void texte(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider tampons,
	                   String texte, float x, float y, boolean àDroite) {
		TextRenderer police = client.textRenderer;
		float échelle = 0.8F / 8.0F;
		float largeur = police.getWidth(texte) * échelle;
		matrices.push();
		matrices.translate(àDroite ? x - largeur : x, y, Z + 0.04F);
		matrices.scale(échelle, -échelle, échelle);
		police.draw(texte, 0.0F, 0.0F, TEXTE, false, matrices.peek().getPositionMatrix(), tampons,
				TextRenderer.TextLayerType.POLYGON_OFFSET, 0, PLEIN);
		matrices.pop();
	}

	/** Hors du vide, ou sans liaison : rien ne répond. Un message qui clignote, et de la neige. */
	private void sansSignal(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider tampons,
	                        VertexConsumer tampon, String message) {
		long temps = client.world == null ? 0 : client.world.getTime();
		MatrixStack.Entry repère = matrices.peek();
		java.util.Random neige = new java.util.Random(temps / 2);
		for (int i = 0; i < 6; i++) {
			float y = Y1 + 0.3F + neige.nextFloat() * (Y2 - Y1 - 0.6F);
			Geometrie.aplatUni(repère, tampon, X1 + 0.2F, y, X2 - 0.2F, y + 0.12F, Z + 0.01F,
					couleur(PHOSPHORE, 0.15F + neige.nextFloat() * 0.25F), PLEIN);
		}
		if ((temps / 10) % 2 == 0) {
			TextRenderer police = client.textRenderer;
			String texte = net.minecraft.text.Text.translatable(message).getString();
			float échelle = 0.8F / 8.0F;
			float largeur = police.getWidth(texte) * échelle;
			matrices.push();
			matrices.translate(CX - largeur / 2, (Y1 + Y2) / 2 + 0.4F, Z + 0.04F);
			matrices.scale(échelle, -échelle, échelle);
			police.draw(texte, 0.0F, 0.0F, TEXTE, false, matrices.peek().getPositionMatrix(), tampons,
					TextRenderer.TextLayerType.POLYGON_OFFSET, 0, PLEIN);
			matrices.pop();
		}
	}

	private static int couleur(int rgb, float alpha) {
		int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}
