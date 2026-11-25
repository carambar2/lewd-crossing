package dev.pyrofetch.jn1.client.photo;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.client.camera.PhotoCapture;
import dev.pyrofetch.jn1.network.PhotoRequestPayload;
import dev.pyrofetch.jn1.photo.PhotoImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Les photos reçues, converties en textures et gardées en mémoire. Une photo
 * pas encore reçue est demandée au serveur une seule fois.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public final class PhotoTextures {
	private static final Map<Integer, Identifier> TEXTURES = new HashMap<>();
	private static final Set<Integer> RÉCLAMÉES = new HashSet<>();
	/**
	 * Quand chaque photo a servi pour la dernière fois. Une image pleine pèse
	 * 1,5 Mo de mémoire graphique : au-delà de {@link #GARDÉES}, celles qu'on
	 * n'a plus regardées depuis {@link #OUBLI} sont libérées, et redemandées
	 * au serveur si elles reparaissent.
	 */
	private static final Map<Integer, Long> USAGE = new HashMap<>();
	private static final int GARDÉES = 64;
	private static final long OUBLI = 30_000L;

	private PhotoTextures() {
	}

	@Nullable
	public static Identifier get(int numéro) {
		if (numéro <= 0) {
			return null;
		}

		Identifier prête = TEXTURES.get(numéro);
		if (prête != null) {
			USAGE.put(numéro, System.currentTimeMillis());
			return prête;
		}

		if (RÉCLAMÉES.add(numéro) && ClientPlayNetworking.canSend(PhotoRequestPayload.ID)) {
			ClientPlayNetworking.send(new PhotoRequestPayload(numéro));
		}

		return null;
	}

	public static void reçue(int numéro, PhotoImage image) {
		if (!image.estCohérente()) {
			JN1.LOGGER.warn("Photo {} reçue avec {} octets pour un format qui en veut {}.", new Object[]{numéro, image.pixels().length, image.format().octets()});
		} else {
			MinecraftClient client = MinecraftClient.getInstance();
			NativeImage native_ = new NativeImage(image.largeur(), image.hauteur(), false);
			int[] palette = PhotoCapture.palette();

			for (int y = 0; y < image.hauteur(); y++) {
				for (int x = 0; x < image.largeur(); x++) {
					native_.setColor(x, y, palette[image.pixel(x, y) & 0xFF]);
				}
			}

			Identifier id = JN1.id("photo/" + numéro);
			NativeImageBackedTexture texture = new NativeImageBackedTexture(native_);
			texture.setFilter(false, false);
			client.getTextureManager().registerTexture(id, texture);
			TEXTURES.put(numéro, id);
			USAGE.put(numéro, System.currentTimeMillis());
			élaguer(client);
		}
	}

	private static void élaguer(MinecraftClient client) {
		if (TEXTURES.size() <= GARDÉES) {
			return;
		}
		long seuil = System.currentTimeMillis() - OUBLI;
		TEXTURES.entrySet().removeIf(entrée -> {
			if (TEXTURES.size() <= GARDÉES || USAGE.getOrDefault(entrée.getKey(), 0L) > seuil) {
				return false;
			}
			client.getTextureManager().destroyTexture(entrée.getValue());
			USAGE.remove(entrée.getKey());
			RÉCLAMÉES.remove(entrée.getKey());
			return true;
		});
	}

	public static void clear() {
		MinecraftClient client = MinecraftClient.getInstance();
		TEXTURES.values().forEach(id -> client.getTextureManager().destroyTexture(id));
		TEXTURES.clear();
		RÉCLAMÉES.clear();
		USAGE.clear();
	}
}
