package dev.pyrofetch.jn1.client.camera;

import com.mojang.blaze3d.platform.NativeImage;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.network.PhotoUploadPayload;
import dev.pyrofetch.jn1.photo.PhotoFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;
import org.jetbrains.annotations.Nullable;

/**
 * La capture : à la fin du rendu d'une image, l'écran est lu, recadré, réduit
 * et ramené à la palette des photos, puis envoyé au serveur par tranches.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class PhotoCapture {
	private static boolean demandé;
	private static PhotoFormat format = PhotoFormat.PAYSAGE;
	private static int[] palette;
	private static int[] indicesValides;
	private static int[][] teintes;
	private static final int ACCENTUATION = 60;

	private PhotoCapture() {
	}

	public static void demander(PhotoFormat formatVoulu, @Nullable BlockPos depuis, float lacet, float tangage) {
		demandé = true;
		format = formatVoulu;
		if (depuis != null) {
			JN1.LOGGER.debug("Prise de vue depuis {} ({}° / {}°).", new Object[]{depuis, lacet, tangage});
		}
	}

	/** Une photo attend la fin de l'image en cours. */
	public static boolean demandée() {
		return demandé;
	}

	/**
	 * À la fin du rendu du monde (avant la main et l'interface) : l'image est
	 * copiée de la carte graphique. Depuis 1.21.5, cette lecture est différée :
	 * l'image arrive un peu plus tard, quand la copie est faite ; la pose de la
	 * caméra, elle, est relevée tout de suite, celle de l'image qu'on copie.
	 */
	public static void auRenduTerminé() {
		if (demandé) {
			demandé = false;
			Minecraft client = Minecraft.getInstance();
			com.mojang.blaze3d.pipeline.RenderTarget cible = client.gameRenderer.mainRenderTarget();
			PhotoFormat voulu = format;
			try {
				dev.pyrofetch.jn1.photo.PhotoPose prise = pose(client, cible.width, cible.height, voulu);
				Screenshot.takeScreenshot(cible, écran -> {
					try (écran) {
						envoyer(réduire(écran, voulu), voulu, prise);
					} catch (Exception échec) {
						JN1.LOGGER.error("Prise de vue impossible", échec);
					}
				});
			} catch (Exception échec) {
				JN1.LOGGER.error("Prise de vue impossible", échec);
			}
		}
	}

	private static byte[] réduire(NativeImage écran, PhotoFormat format) {
		préparerLaPalette();
		int largeurÉcran = écran.getWidth();
		int hauteurÉcran = écran.getHeight();
		int[] cadre = cadre(Minecraft.getInstance(), largeurÉcran, hauteurÉcran, format);
		int largeurCadre = cadre[0];
		int hauteurCadre = cadre[1];
		int gauche = (largeurÉcran - largeurCadre) / 2;
		int haut = (hauteurÉcran - hauteurCadre) / 2;
		int largeur = format.largeurEnPixels();
		int hauteur = format.hauteurEnPixels();
		byte[] octets = new byte[largeur * hauteur];
		int[] rouge = new int[largeur * hauteur];
		int[] vert = new int[largeur * hauteur];
		int[] bleu = new int[largeur * hauteur];

		for (int y = 0; y < hauteur; y++) {
			int y0 = haut + (int)((long)y * hauteurCadre / hauteur);
			int y1 = Math.max(y0 + 1, haut + (int)((long)(y + 1) * hauteurCadre / hauteur));
			y1 = Math.min(y1, hauteurÉcran);

			for (int x = 0; x < largeur; x++) {
				int x0 = gauche + (int)((long)x * largeurCadre / largeur);
				int x1 = Math.max(x0 + 1, gauche + (int)((long)(x + 1) * largeurCadre / largeur));
				x1 = Math.min(x1, largeurÉcran);
				long r = 0L;
				long v = 0L;
				long b = 0L;
				int compte = 0;

				for (int sy = y0; sy < y1; sy++) {
					for (int sx = x0; sx < x1; sx++) {
						// En ARGB depuis 1.21.2 (l'ancienne lecture rendait de l'ABGR).
						int pixel = écran.getPixel(sx, sy);
						r += pixel >> 16 & 0xFF;
						v += pixel >> 8 & 0xFF;
						b += pixel & 0xFF;
						compte++;
					}
				}

				if (compte != 0) {
					int rang = y * largeur + x;
					rouge[rang] = (int)(r / compte);
					vert[rang] = (int)(v / compte);
					bleu[rang] = (int)(b / compte);
				}
			}
		}

		accentuer(rouge, largeur, hauteur);
		accentuer(vert, largeur, hauteur);
		accentuer(bleu, largeur, hauteur);

		for (int rang = 0; rang < octets.length; rang++) {
			octets[rang] = plusProche(rouge[rang] | vert[rang] << 8 | bleu[rang] << 16);
		}

		return octets;
	}

	private static void accentuer(int[] canal, int largeur, int hauteur) {
		int[] flou = new int[canal.length];

		for (int y = 0; y < hauteur; y++) {
			for (int x = 0; x < largeur; x++) {
				int somme = 0;
				int compte = 0;

				for (int dy = -1; dy <= 1; dy++) {
					int sy = y + dy;
					if (sy >= 0 && sy < hauteur) {
						for (int dx = -1; dx <= 1; dx++) {
							int sx = x + dx;
							if (sx >= 0 && sx < largeur) {
								somme += canal[sy * largeur + sx];
								compte++;
							}
						}
					}
				}

				flou[y * largeur + x] = somme / compte;
			}
		}

		for (int rang = 0; rang < canal.length; rang++) {
			int rehaussé = canal[rang] + (canal[rang] - flou[rang]) * 60 / 100;
			canal[rang] = Math.clamp(rehaussé, 0, 255);
		}
	}

	private static byte plusProche(int couleur) {
		int r = couleur & 0xFF;
		int v = couleur >> 8 & 0xFF;
		int b = couleur >> 16 & 0xFF;
		int meilleur = indicesValides[0];
		int écartMin = Integer.MAX_VALUE;

		for (int rang = 0; rang < indicesValides.length; rang++) {
			int[] teinte = teintes[rang];
			int dr = r - teinte[0];
			int dv = v - teinte[1];
			int db = b - teinte[2];
			int écart = dr * dr + dv * dv + db * db;
			if (écart < écartMin) {
				écartMin = écart;
				meilleur = indicesValides[rang];
			}
		}

		return (byte)meilleur;
	}

	private static void préparerLaPalette() {
		if (indicesValides == null) {
			List<Integer> retenus = new ArrayList<>();
			List<int[]> couleurs = new ArrayList<>();

			for (int identifiant = 0; identifiant < 64; identifiant++) {
				MapColor couleur;
				try {
					couleur = MapColor.byId(identifiant);
				} catch (RuntimeException horsTable) {
					break;
				}

				if (couleur != null && couleur != MapColor.NONE) {
					for (Brightness ombrage : Brightness.values()) {
						int facteur = ombrage.modifier;
						retenus.add(identifiant * 4 + ombrage.id);
						couleurs.add(new int[]{(couleur.col >> 16 & 0xFF) * facteur / 255, (couleur.col >> 8 & 0xFF) * facteur / 255, (couleur.col & 0xFF) * facteur / 255});
					}
				}
			}

			indicesValides = retenus.stream().mapToInt(Integer::intValue).toArray();
			teintes = couleurs.toArray(new int[0][]);
			JN1.LOGGER.debug("Palette de photo : {} teintes utilisables sur 256.", indicesValides.length);
		}
	}

	public static int[] palette() {
		if (palette == null) {
			palette = new int[256];

			for (int indice = 0; indice < 256; indice++) {
				palette[indice] = MapColor.getColorFromPackedId(indice) | 0xFF000000;
			}
		}

		return palette;
	}

	/**
	 * Le cadre de la photo, en pixels de l'écran, centré : la fenêtre du viseur
	 * ({@link Viewfinder#cadrer}), et non tout l'écran recadré au format — la
	 * photo prenait sinon un bon sixième de plus de chaque côté que ce qu'on
	 * avait cadré.
	 */
	private static int[] cadre(Minecraft client, int largeurÉcran, int hauteurÉcran, PhotoFormat format) {
		double échelle = client.getWindow().getGuiScale();
		var fenêtre = Viewfinder.cadrer((int) Math.ceil(largeurÉcran / échelle), (int) Math.ceil(hauteurÉcran / échelle),
				format.rapport());
		int largeur = Math.min(largeurÉcran, (int) Math.round(fenêtre.largeur() * échelle));
		int hauteur = Math.min(hauteurÉcran, Math.round(largeur / format.rapport()));
		return new int[]{Math.max(1, largeur), Math.max(1, hauteur)};
	}

	/**
	 * La caméra telle qu'elle vient d'être rendue — à la main, à la troisième
	 * personne ou au trépied, c'est elle qui fait foi —, et l'ouverture du cadre
	 * recadré au format, comme {@link #réduire} le découpe.
	 */
	static dev.pyrofetch.jn1.photo.PhotoPose pose(Minecraft client, int largeurÉcran, int hauteurÉcran,
	                                             PhotoFormat format) {
		var caméra = client.gameRenderer.mainCamera();
		// Le champ de la caméra tel qu'elle vient de le rendre (réglage et effets
		// compris) ; en 1.21.1, le réglage multiplié par le modificateur du joueur.
		double champ = caméra.getFov();
		double tanV = Math.tan(Math.toRadians(champ) / 2);
		double tanH = tanV * largeurÉcran / (double) hauteurÉcran;
		int[] cadre = cadre(client, largeurÉcran, hauteurÉcran, format);
		int largeurCadre = cadre[0];
		int hauteurCadre = cadre[1];
		var œil = caméra.position();
		return new dev.pyrofetch.jn1.photo.PhotoPose(new org.joml.Vector3f((float) œil.x, (float) œil.y, (float) œil.z),
				caméra.yRot(), caméra.xRot(),
				(float) (tanH * largeurCadre / largeurÉcran), (float) (tanV * hauteurCadre / hauteurÉcran));
	}

	private static void envoyer(byte[] pixels, PhotoFormat format, dev.pyrofetch.jn1.photo.PhotoPose pose) {
		int session = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);

		for (int décalage = 0; décalage < pixels.length; décalage += 24000) {
			int longueur = Math.min(24000, pixels.length - décalage);
			byte[] tranche = new byte[longueur];
			System.arraycopy(pixels, décalage, tranche, 0, longueur);
			boolean dernière = décalage + longueur >= pixels.length;
			ClientPlayNetworking.send(new PhotoUploadPayload(session, format, décalage, tranche, dernière, pose));
		}

		JN1.LOGGER.debug("Photo envoyée : {} octets en {} tranche(s).", pixels.length, (pixels.length + 24000 - 1) / 24000);
	}
}
