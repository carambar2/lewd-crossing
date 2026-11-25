package dev.pyrofetch.jn1.client.mesh;

import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Un maillage lisse exporté de Blender (`tools/blender_maillage.py`), tel que
 * le jeu le lit : des parties (une par texture), des sommets liés à des os
 * de GeckoLib par des poids.
 *
 * <p>Le fichier donne positions et normales dans le repère du fichier de
 * modèle (pixels, x non retourné) ; on les ramène ici dans celui où GeckoLib
 * dessine ses cubes : x retourné, en blocs. Les UV sont déjà en haut à gauche.
 *
 * <p>Les maillages sont lus une fois et gardés ; un rechargement des ressources
 * (F3+T) les oublie.
 */
public final class Maillage {
	private static final int MAGIQUE = 0x4A4D5348; // « JMSH »
	private static final Map<Identifier, Optional<Maillage>> LUS = new HashMap<>();

	/** Les noms des os, dans l'ordre où les sommets les désignent. */
	public final String[] os;
	public final List<Partie> parties;

	/**
	 * Une partie : les triangles d'un même objet et d'un même matériau de Blender.
	 *
	 * @param nom       « objet/matériau », ce qu'une tenue montre ou cache
	 * @param positions x, y, z par sommet, au repos, dans le repère de GeckoLib
	 * @param normales  idem
	 * @param uv        u, v par sommet
	 * @param osIndex   quatre os par sommet (−1 : aucun)
	 * @param poids     quatre poids par sommet, de somme 1
	 * @param triangles trois sommets par triangle
	 */
	public record Partie(String nom, Identifier texture, boolean lumineuse, float[] positions, float[] normales, float[] uv,
	                     short[] osIndex, float[] poids, int[] triangles, Map<String, Forme> formes) {
		public int sommets() {
			return this.positions.length / 3;
		}
	}

	/**
	 * Une forme (shape key de Blender, version 3) : un déplacement par sommet
	 * touché, au repos, dans le repère de GeckoLib (x retourné, en blocs). Le
	 * rendu la dose de 0 à 1 avant de déformer le maillage par ses os.
	 */
	public record Forme(int[] sommets, float[] déplacements) {
	}

	private Maillage(String[] os, List<Partie> parties) {
		this.os = os;
		this.parties = parties;
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return JN1.id("maillages");
					}

					@Override
					public void onResourceManagerReload(ResourceManager manager) {
						LUS.clear();
					}
				});
	}

	/** Le maillage à cet identifiant (`jn1:mesh/gaster.jmesh`), ou rien s'il est illisible. */
	@Nullable
	public static Maillage de(Identifier id) {
		return LUS.computeIfAbsent(id, Maillage::lire).orElse(null);
	}

	private static Optional<Maillage> lire(Identifier id) {
		Optional<Resource> ressource = Minecraft.getInstance().getResourceManager().getResource(id);
		if (ressource.isEmpty()) {
			JN1.LOGGER.error("Maillage introuvable : {}", id);
			return Optional.empty();
		}
		try (InputStream flux = ressource.get().open();
		     DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(flux))) {
			if (in.readInt() != MAGIQUE) {
				throw new IOException("pas un maillage");
			}
			int version = in.readInt();
			String[] os = new String[in.readInt()];
			for (int i = 0; i < os.length; i++) {
				os[i] = in.readUTF();
			}
			int n = in.readInt();
			java.util.ArrayList<Partie> parties = new java.util.ArrayList<>(n);
			for (int p = 0; p < n; p++) {
				String nom = version >= 2 ? in.readUTF() : "partie" + p;
				Identifier texture = Identifier.parse(in.readUTF());
				boolean lumineuse = (in.readByte() & 1) != 0;
				int sommets = in.readInt();
				float[] positions = new float[sommets * 3];
				float[] normales = new float[sommets * 3];
				float[] uv = new float[sommets * 2];
				short[] osIndex = new short[sommets * 4];
				float[] poids = new float[sommets * 4];
				for (int s = 0; s < sommets; s++) {
					// Fichier (pixels, x non retourné) → GeckoLib (blocs, x retourné).
					positions[s * 3] = -in.readFloat() / 16.0F;
					positions[s * 3 + 1] = in.readFloat() / 16.0F;
					positions[s * 3 + 2] = in.readFloat() / 16.0F;
					normales[s * 3] = -in.readFloat();
					normales[s * 3 + 1] = in.readFloat();
					normales[s * 3 + 2] = in.readFloat();
					uv[s * 2] = in.readFloat();
					uv[s * 2 + 1] = in.readFloat();
					for (int k = 0; k < 4; k++) {
						osIndex[s * 4 + k] = in.readShort();
					}
					for (int k = 0; k < 4; k++) {
						poids[s * 4 + k] = in.readFloat();
					}
				}
				int[] triangles = new int[in.readInt() * 3];
				for (int t = 0; t < triangles.length; t++) {
					triangles[t] = in.readInt();
				}
				Map<String, Forme> formes = new HashMap<>();
				if (version >= 3) {
					int nf = in.readInt();
					for (int f = 0; f < nf; f++) {
						String nomForme = in.readUTF();
						int k = in.readInt();
						int[] indices = new int[k];
						float[] d = new float[k * 3];
						for (int s = 0; s < k; s++) {
							indices[s] = in.readInt();
							d[s * 3] = -in.readFloat() / 16.0F;
							d[s * 3 + 1] = in.readFloat() / 16.0F;
							d[s * 3 + 2] = in.readFloat() / 16.0F;
						}
						formes.put(nomForme, new Forme(indices, d));
					}
				}
				// Le sens des triangles est celui de Blender : de Blender au fichier,
				// puis du fichier à GeckoLib, x est retourné deux fois.
				parties.add(new Partie(nom, texture, lumineuse, positions, normales, uv, osIndex, poids, triangles,
						Map.copyOf(formes)));
			}
			return Optional.of(new Maillage(os, List.copyOf(parties)));
		} catch (IOException | RuntimeException e) {
			JN1.LOGGER.error("Maillage illisible : {}", id, e);
			return Optional.empty();
		}
	}
}
