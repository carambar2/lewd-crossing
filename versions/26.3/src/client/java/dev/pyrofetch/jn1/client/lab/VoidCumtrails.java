package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.pyrofetch.jn1.JN1;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Les « cumtrails » du ciel du vide.
 *
 * <p>Des avions traversent le ciel d'un bord à l'autre, sur un grand cercle qui
 * passe haut au-dessus de l'œil, et laissent une traînée crème : deux filets
 * derrière les réacteurs, qui se rejoignent en une corde grumeleuse, s'élargit,
 * se voile d'un halo et s'efface au bout de trois minutes, de la plus vieille
 * partie à la plus jeune. Ce n'est pas de la vapeur : des coulures en pendent,
 * qui s'allongent à mesure qu'elle vieillit, une goutte au bout. Tout dérive
 * lentement vers l'est avec le courant du tunnel ; plusieurs traînées se
 * croisent et font la grille des photos de chemtrails.
 *
 * <p>Rien n'est tiré au hasard d'une image à l'autre : chaque passage vient d'un
 * créneau de l'horloge du monde (un départ possible toutes les {@link #PÉRIODE}
 * ticks), dont il tire sa route et ses coulures. Deux joueurs voient donc le
 * même ciel, et rien n'est sauvegardé. Posé à {@link #DISTANCE} blocs, sous la
 * limite des cent blocs du ciel ({@link VoidSky}). Planches :
 * {@code tools/gen_void_sky.py cumtrails}.
 */
final class VoidCumtrails {
	private static final Identifier RUBAN = JN1.id("textures/environment/void_cumtrail.png");
	private static final Identifier HALO = JN1.id("textures/environment/void_cumtrail_halo.png");
	private static final Identifier GOUTTE = JN1.id("textures/environment/void_cumtrail_drop.png");
	private static final Identifier AVION = JN1.id("textures/environment/void_plane.png");
	private static final Identifier LUEUR = JN1.id("textures/environment/void_glow.png");
	/** Les planches des traînées et des avions, à charger avant la passe du ciel. */
	static final List<Identifier> PLANCHES = List.of(RUBAN, HALO, GOUTTE, AVION, LUEUR);

	private static final float DISTANCE = 84.0F;
	/** Un départ possible tous les tant de ticks, et sa chance. */
	private static final int PÉRIODE = 900;
	private static final float CHANCE = 0.8F;
	/** La durée d'une traversée, en ticks. */
	private static final int VOL_MIN = 1000;
	private static final int VOL_MAX = 1700;
	/** Ce que vit un point de la traînée après le passage de l'avion. */
	private static final int VIE = 3600;
	private static final int ÉCHANTILLONS = 80;
	/** Les blocs que couvre une répétition de la planche du ruban. */
	private static final float MOTIF = 20.0F;
	/** La dérive vers l'est, avec le courant du tunnel, en blocs par tick. */
	private static final float DÉRIVE = 0.004F;
	/** La demi-envergure de l'avion, en blocs. */
	private static final float AVION_DEMI = 1.7F;

	private VoidCumtrails() {
	}

	/** Dessine les traînées du moment, puis les avions qui volent encore, puis leurs feux. */
	static void dessiner(Matrix4f m, float t) {
		long créneau = (long) Math.floor(t / PÉRIODE);
		int recul = (VOL_MAX + VIE) / PÉRIODE + 2;
		List<Passage> enVol = new ArrayList<>();
		DessinCiel.translucide();
		for (long k = créneau - recul; k <= créneau; k++) {
			Passage p = Passage.tirer(k);
			if (p != null && p.dessiner(m, t)) {
				enVol.add(p);
			}
		}
		for (Passage p : enVol) {
			p.avion(m, t);
		}
		DessinCiel.additif();
		for (Passage p : enVol) {
			p.feux(m, t);
		}
		DessinCiel.translucide();
	}

	private static float lisse(float a, float b, float x) {
		float u = Mth.clamp((x - a) / (b - a), 0.0F, 1.0F);
		return u * u * (3 - 2 * u);
	}

	/** La corde : fine et dense derrière l'avion, plus large en vieillissant, puis effacée. */
	private static float largeur(float âge) {
		return 0.35F + 2.4F * (1.0F - (float) Math.exp(-âge / 600.0F));
	}

	private static float opacité(float âge) {
		return 0.85F * lisse(0, 10, âge) * (1.0F - lisse(VIE * 0.3F, VIE * 0.8F, âge));
	}

	/** Les deux filets des réacteurs, écartés derrière l'avion, qui se rejoignent en trois secondes. */
	private static float écart(float âge) {
		return 0.5F * (1.0F - lisse(0, 70, âge));
	}

	private static float largeurHalo(float âge) {
		return 1.5F + 11.0F * (1.0F - (float) Math.exp(-âge / 1000.0F));
	}

	private static float opacitéHalo(float âge) {
		return 0.45F * lisse(20, 400, âge) * (1.0F - lisse(VIE * 0.45F, VIE, âge));
	}

	private static int couleur(float r, float g, float b, float a) {
		return Mth.clamp((int) (a * 255), 0, 255) << 24
				| Mth.clamp((int) (r * 255), 0, 255) << 16
				| Mth.clamp((int) (g * 255), 0, 255) << 8
				| Mth.clamp((int) (b * 255), 0, 255);
	}

	/** Un sommet à {@code p + côté × d}. */
	private static void sommet(BufferBuilder tampon, Matrix4f m, Vector3f p, Vector3f côté, float d, float u, float v,
	                           int c) {
		tampon.addVertex(m, p.x + côté.x * d, p.y + côté.y * d, p.z + côté.z * d).setUv(u, v).setColor(c);
	}

	/** Un passage d'avion : sa route sur la sphère du ciel, son heure, ses coulures. */
	private static final class Passage {
		private final float départ;
		private final float vol;
		/** Le grand cercle : son point de départ {@code a}, la direction {@code c} vers l'arrivée, l'angle. */
		private final Vector3f a;
		private final Vector3f c;
		private final float angle;
		/** Les coulures : où elles pendent (fraction de la route) et leur longueur au plus. */
		private final float[] coulures;
		private final float[] longueurs;
		/** Les phases de l'ondulation qui gagne la traînée en vieillissant. */
		private final float[] phases;

		private Passage(float départ, float vol, Vector3f a, Vector3f c, float angle, float[] coulures,
		                float[] longueurs, float[] phases) {
			this.départ = départ;
			this.vol = vol;
			this.a = a;
			this.c = c;
			this.angle = angle;
			this.coulures = coulures;
			this.longueurs = longueurs;
			this.phases = phases;
		}

		/** Le passage de ce créneau, ou rien : chaque créneau tire toujours le même. */
		static Passage tirer(long créneau) {
			Random r = new Random(créneau * 0x9E3779B97F4A7C15L ^ 0x2545F4914F6CDD1DL);
			if (r.nextFloat() > CHANCE) {
				return null;
			}
			float départ = créneau * PÉRIODE + r.nextFloat() * PÉRIODE * 0.6F;
			float vol = VOL_MIN + r.nextFloat() * (VOL_MAX - VOL_MIN);
			// D'un bord du ciel à l'autre : l'arrivée à l'opposé du départ, à ±63°,
			// les deux entre 12° et 42° au-dessus de l'horizon — la route passe haut.
			float az0 = r.nextFloat() * Mth.TWO_PI;
			float az1 = az0 + Mth.PI + (r.nextFloat() - 0.5F) * 2.2F;
			Vector3f a = direction(az0, (12 + r.nextFloat() * 30) * Mth.DEG_TO_RAD);
			Vector3f b = direction(az1, (12 + r.nextFloat() * 30) * Mth.DEG_TO_RAD);
			float cos = Mth.clamp(a.dot(b), -0.999F, 0.999F);
			float angle = (float) Math.acos(cos);
			Vector3f c = new Vector3f(b).sub(new Vector3f(a).mul(cos)).normalize();
			int n = Math.max(1, (int) (angle * DISTANCE / 7.0F));
			float[] où = new float[n];
			float[] long_ = new float[n];
			int k = 0;
			for (int j = 0; j < n; j++) {
				float f = (j + r.nextFloat()) / n;
				float l = 2.0F + r.nextFloat() * 4.0F;
				if (r.nextFloat() < 0.5F) {
					où[k] = f;
					long_[k] = l;
					k++;
				}
			}
			float[] phases = {r.nextFloat() * Mth.TWO_PI, r.nextFloat() * Mth.TWO_PI,
					r.nextFloat() * Mth.TWO_PI};
			return new Passage(départ, vol, a, c, angle, java.util.Arrays.copyOf(où, k),
					java.util.Arrays.copyOf(long_, k), phases);
		}

		private static Vector3f direction(float azimut, float élévation) {
			return new Vector3f(Mth.cos(élévation) * Mth.cos(azimut), Mth.sin(élévation),
					Mth.cos(élévation) * Mth.sin(azimut));
		}

		/** Le point de la route à cette fraction, déporté vers l'est d'après son âge, sur la sphère du ciel. */
		private Vector3f point(float f, float âge, float distance) {
			float x = f * this.angle;
			Vector3f p = new Vector3f(this.a).mul(Mth.cos(x)).add(new Vector3f(this.c).mul(Mth.sin(x)))
					.mul(DISTANCE);
			p.x += DÉRIVE * Math.max(0, âge);
			// Droite derrière l'avion, elle ondule en vieillissant, comme une
			// traînée que le vent travaille.
			float o = 1.4F * lisse(200, 3000, âge);
			if (o > 0) {
				p.add(o * Mth.sin(f * 17 + this.phases[0]), 0.6F * o * Mth.sin(f * 11 + this.phases[1]),
						o * Mth.sin(f * 13 + this.phases[2]));
			}
			return p.normalize().mul(distance);
		}

		/** La traînée ; vrai si l'avion vole encore. */
		boolean dessiner(Matrix4f m, float t) {
			float écoulé = t - this.départ;
			if (écoulé <= 0 || écoulé > this.vol + VIE) {
				return false;
			}
			float tête = Math.min(1.0F, écoulé / this.vol);
			int n = ÉCHANTILLONS;
			Vector3f[] p = new Vector3f[n + 1];
			Vector3f[] côté = new Vector3f[n + 1];
			float[] âge = new float[n + 1];
			float[] v = new float[n + 1];
			// Là où l'avion est apparu, la traînée naît en fondu au lieu d'être coupée net.
			float[] naissance = new float[n + 1];
			for (int i = 0; i <= n; i++) {
				// Plus serrés près de la tête, où les deux filets se rejoignent.
				float s = (float) i / n;
				float f = tête * (1.0F - (1.0F - s) * (1.0F - s));
				âge[i] = écoulé - f * this.vol;
				naissance[i] = lisse(0, 0.04F, f);
				p[i] = this.point(f, âge[i], DISTANCE);
				v[i] = f * this.angle * DISTANCE / MOTIF;
			}
			for (int i = 0; i <= n; i++) {
				Vector3f le_long = new Vector3f(p[Math.min(i + 1, n)]).sub(p[Math.max(i - 1, 0)]);
				Vector3f s = le_long.cross(p[i], new Vector3f());
				côté[i] = s.lengthSquared() < 1.0E-8F ? new Vector3f(0, 1, 0) : s.normalize();
			}

			// Le halo d'abord, la corde par-dessus.
			BufferBuilder tampon = DessinCiel.commencer(HALO);
			boolean rien = true;
			for (int i = 0; i < n; i++) {
				float a1 = opacitéHalo(âge[i]) * naissance[i];
				float a2 = opacitéHalo(âge[i + 1]) * naissance[i + 1];
				if (a1 < 0.004F && a2 < 0.004F) {
					continue;
				}
				float l1 = largeurHalo(âge[i]) / 2;
				float l2 = largeurHalo(âge[i + 1]) / 2;
				int c1 = couleur(1, 1, 1, a1);
				int c2 = couleur(1, 1, 1, a2);
				sommet(tampon, m, p[i], côté[i], -l1, 0, v[i] * 0.5F, c1);
				sommet(tampon, m, p[i], côté[i], l1, 1, v[i] * 0.5F, c1);
				sommet(tampon, m, p[i + 1], côté[i + 1], l2, 1, v[i + 1] * 0.5F, c2);
				sommet(tampon, m, p[i + 1], côté[i + 1], -l2, 0, v[i + 1] * 0.5F, c2);
				rien = false;
			}
			tracer(tampon, rien);

			tampon = DessinCiel.commencer(RUBAN);
			rien = true;
			for (int filet = -1; filet <= 1; filet += 2) {
				for (int i = 0; i < n; i++) {
					float a1 = opacité(âge[i]) * 0.7F * naissance[i];
					float a2 = opacité(âge[i + 1]) * 0.7F * naissance[i + 1];
					if (a1 < 0.004F && a2 < 0.004F) {
						continue;
					}
					// Deux filets fins derrière l'avion ; rejoints, une seule corde.
					float d1 = filet * écart(âge[i]);
					float d2 = filet * écart(âge[i + 1]);
					float l1 = largeur(âge[i]) * (0.55F + 0.45F * lisse(0, 70, âge[i])) / 2;
					float l2 = largeur(âge[i + 1]) * (0.55F + 0.45F * lisse(0, 70, âge[i + 1])) / 2;
					int c1 = couleur(1, 1, 1, a1);
					int c2 = couleur(1, 1, 1, a2);
					// Le second filet lit la planche décalée : ses grumeaux ne tombent pas en face.
					float dv = filet > 0 ? 0.37F : 0.0F;
					sommet(tampon, m, p[i], côté[i], d1 - l1, 0, v[i] + dv, c1);
					sommet(tampon, m, p[i], côté[i], d1 + l1, 1, v[i] + dv, c1);
					sommet(tampon, m, p[i + 1], côté[i + 1], d2 + l2, 1, v[i + 1] + dv, c2);
					sommet(tampon, m, p[i + 1], côté[i + 1], d2 - l2, 0, v[i + 1] + dv, c2);
					rien = false;
				}
			}
			tracer(tampon, rien);
			this.coulures(m, écoulé, tête);
			return écoulé < this.vol;
		}

		/** Les coulures : un filet qui pend de la corde vers le bas du ciel, et sa goutte. */
		private void coulures(Matrix4f m, float écoulé, float tête) {
			List<float[]> gouttes = new ArrayList<>();
			BufferBuilder tampon = DessinCiel.commencer(RUBAN);
			boolean rien = true;
			for (int j = 0; j < this.coulures.length; j++) {
				float f = this.coulures[j];
				if (f > tête) {
					continue;
				}
				float âge = écoulé - f * this.vol;
				float alpha = opacité(âge);
				if (âge < 120 || alpha < 0.004F) {
					continue;
				}
				Vector3f p = this.point(f, âge, DISTANCE);
				Vector3f dir = new Vector3f(p).normalize();
				// Le bas, couché sur la sphère du ciel.
				Vector3f bas = new Vector3f(0, -1, 0).add(new Vector3f(dir).mul(dir.y));
				if (bas.length() < 0.25F) {
					continue;
				}
				bas.normalize();
				Vector3f côté = new Vector3f(bas).cross(dir).normalize();
				float longueur = this.longueurs[j] * lisse(120, 1600, âge);
				int c = couleur(1, 1, 1, alpha);
				Vector3f haut = new Vector3f(p).sub(new Vector3f(bas).mul(0.2F));
				Vector3f fin = new Vector3f(p).add(new Vector3f(bas).mul(longueur));
				// Le filet suit la corde : plus épais en haut, où il s'en détache.
				float l = 0.1F + 0.08F * largeur(âge);
				sommet(tampon, m, haut, côté, -l * 1.3F, 0.2F, j * 0.31F, c);
				sommet(tampon, m, haut, côté, l * 1.3F, 0.8F, j * 0.31F, c);
				sommet(tampon, m, fin, côté, l * 0.7F, 0.8F, j * 0.31F + longueur / MOTIF, c);
				sommet(tampon, m, fin, côté, -l * 0.7F, 0.2F, j * 0.31F + longueur / MOTIF, c);
				rien = false;
				float taille = 0.5F + 0.5F * lisse(120, 1600, âge);
				gouttes.add(new float[] {fin.x, fin.y, fin.z, bas.x, bas.y, bas.z, côté.x, côté.y, côté.z, taille,
						alpha});
			}
			tracer(tampon, rien);
			if (gouttes.isEmpty()) {
				return;
			}
			// La goutte au bout, la pointe vers le haut : elle s'ouvre où le filet s'arrête.
			tampon = DessinCiel.commencer(GOUTTE);
			for (float[] g : gouttes) {
				float s = g[9];
				int c = couleur(1, 1, 1, g[10]);
				Vector3f bas = new Vector3f(g[3], g[4], g[5]);
				Vector3f côté = new Vector3f(g[6], g[7], g[8]);
				Vector3f h = new Vector3f(g[0], g[1], g[2]).sub(new Vector3f(bas).mul(s * 0.5F));
				Vector3f b = new Vector3f(g[0], g[1], g[2]).add(new Vector3f(bas).mul(s * 1.5F));
				sommet(tampon, m, h, côté, -s, 0, 0, c);
				sommet(tampon, m, h, côté, s, 1, 0, c);
				sommet(tampon, m, b, côté, s, 1, 1, c);
				sommet(tampon, m, b, côté, -s, 0, 1, c);
			}
			DessinCiel.dessiner(tampon);
		}

		private static void tracer(BufferBuilder tampon, boolean rien) {
			// Un tampon vide ne dessine rien : DessinCiel le referme simplement.
			DessinCiel.dessiner(tampon);
		}

		/** Le repère de l'avion : sa place, son avant, son côté. */
		private Vector3f[] repère(float t) {
			float écoulé = t - this.départ;
			float f = Mth.clamp(écoulé / this.vol, 0, 1);
			Vector3f p = this.point(f, 0, DISTANCE - 0.6F);
			Vector3f avant = this.point(Math.min(1, f + 0.002F), 0, DISTANCE)
					.sub(this.point(Math.max(0, f - 0.002F), 0, DISTANCE)).normalize();
			Vector3f côté = new Vector3f(avant).cross(p).normalize();
			return new Vector3f[] {p, avant, côté};
		}

		/** Il paraît et disparaît en deux secondes, aux bords de sa route. */
		private float présence(float t) {
			float écoulé = t - this.départ;
			return lisse(0, 40, écoulé) * (1.0F - lisse(this.vol - 40, this.vol, écoulé));
		}

		void avion(Matrix4f m, float t) {
			Vector3f[] r = this.repère(t);
			Vector3f p = r[0];
			Vector3f avant = r[1];
			Vector3f côté = r[2];
			int c = couleur(1, 1, 1, this.présence(t));
			float d = AVION_DEMI;
			Vector3f nez = new Vector3f(p).add(new Vector3f(avant).mul(d));
			Vector3f queue = new Vector3f(p).sub(new Vector3f(avant).mul(d));
			BufferBuilder tampon = DessinCiel.commencer(AVION);
			sommet(tampon, m, nez, côté, -d, 0, 0, c);
			sommet(tampon, m, nez, côté, d, 1, 0, c);
			sommet(tampon, m, queue, côté, d, 1, 1, c);
			sommet(tampon, m, queue, côté, -d, 0, 1, c);
			DessinCiel.dessiner(tampon);
		}

		/** Les feux de position aux bouts des ailes, rouge et vert, et le flash blanc de la queue. */
		void feux(Matrix4f m, float t) {
			Vector3f[] r = this.repère(t);
			Vector3f p = r[0];
			Vector3f avant = r[1];
			Vector3f côté = r[2];
			float présence = this.présence(t);
			float d = AVION_DEMI;
			BufferBuilder tampon = DessinCiel.commencer(LUEUR);
			Vector3f aile = new Vector3f(p).sub(new Vector3f(avant).mul(0.2F * d));
			feu(tampon, m, new Vector3f(aile).sub(new Vector3f(côté).mul(0.86F * d)), avant, côté, 0.45F,
					couleur(1.0F, 0.15F, 0.1F, présence));
			feu(tampon, m, new Vector3f(aile).add(new Vector3f(côté).mul(0.86F * d)), avant, côté, 0.45F,
					couleur(0.2F, 1.0F, 0.3F, présence));
			if ((int) t % 24 < 2) {
				feu(tampon, m, new Vector3f(p).sub(new Vector3f(avant).mul(0.9F * d)), avant, côté, 0.9F,
						couleur(1, 1, 1, présence));
			}
			DessinCiel.dessiner(tampon);
		}

		private static void feu(BufferBuilder tampon, Matrix4f m, Vector3f centre, Vector3f avant, Vector3f côté,
		                        float s, int c) {
			Vector3f h = new Vector3f(centre).add(new Vector3f(avant).mul(s));
			Vector3f b = new Vector3f(centre).sub(new Vector3f(avant).mul(s));
			sommet(tampon, m, h, côté, -s, 0, 0, c);
			sommet(tampon, m, h, côté, s, 1, 0, c);
			sommet(tampon, m, b, côté, s, 1, 1, c);
			sommet(tampon, m, b, côté, -s, 0, 1, c);
		}
	}
}
