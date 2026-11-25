package dev.pyrofetch.jn1.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.pyrofetch.jn1.JN1;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * L'archive des photos d'un monde, sauvegardée avec lui.
 *
 * <p>Chaque photo y reçoit un numéro ; les tirages et les cadres ne portent que
 * ce numéro, jamais l'image, qui reste côté serveur et n'est envoyée aux
 * clients qu'à la demande.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class PhotoArchive extends SavedData {
	private static final String NOM = "jn1_photos";
	private static final String CLÉ_SUIVANT = "next_id";
	private static final String CLÉ_PHOTOS = "photos";
	private final Map<Integer, PhotoImage> photos = new HashMap<>();
	/**
	 * Qui est sur chaque photo, relevé à la prise ({@link dev.pyrofetch.jn1.onlyfaps.Sujets}) ;
	 * absent pour celles d'avant OnlyFaps. Et celles déjà publiées : une seule fois chacune.
	 */
	private final Map<Integer, List<dev.pyrofetch.jn1.onlyfaps.Sujet>> sujets = new HashMap<>();
	private final java.util.Set<Integer> publiées = new java.util.HashSet<>();
	private static final Codec<List<dev.pyrofetch.jn1.onlyfaps.Sujet>> SUJETS =
			dev.pyrofetch.jn1.onlyfaps.Sujet.CODEC.listOf();
	private int suivant = 1;
	// 26.3 : une donnée sauvegardée est un type (identifiant, codec) ; le contenu
	// garde les mêmes clés.
	private static final Codec<PhotoArchive> CODEC =
			CompoundTag.CODEC.xmap(PhotoArchive::lire, archive -> archive.save(new CompoundTag()));
	public static final net.minecraft.world.level.saveddata.SavedDataType<PhotoArchive> TYPE =
			new net.minecraft.world.level.saveddata.SavedDataType<>(JN1.id("photos"), PhotoArchive::new, CODEC, null);

	/**
	 * Une seule archive par monde. La 1.21.1 la rangeait avec le monde principal,
	 * dans {@code data/jn1_photos.dat} ; ce dossier est en 26.3 celui des données
	 * communes au serveur, où elle est désormais.
	 */
	public static PhotoArchive of(ServerLevel monde) {
		net.minecraft.server.MinecraftServer serveur = monde.getServer();
		return dev.pyrofetch.jn1.config.AnciennesDonnees.charger(serveur.getDataStorage(), TYPE,
				dev.pyrofetch.jn1.config.AnciennesDonnees.dossierRacine(serveur).resolve(NOM + ".dat"),
				serveur.registryAccess());
	}

	@Nullable
	public PhotoImage get(int numéro) {
		return this.photos.get(numéro);
	}

	public int ajouter(PhotoImage image) {
		int numéro = this.suivant++;
		this.photos.put(numéro, image);
		this.setDirty();
		return numéro;
	}

	/** Null pour une photo prise avant qu'on relève les sujets. */
	@Nullable
	public List<dev.pyrofetch.jn1.onlyfaps.Sujet> sujets(int numéro) {
		return this.sujets.get(numéro);
	}

	public void poserSujets(int numéro, List<dev.pyrofetch.jn1.onlyfaps.Sujet> liste) {
		this.sujets.put(numéro, List.copyOf(liste));
		this.setDirty();
	}

	public boolean publiée(int numéro) {
		return this.publiées.contains(numéro);
	}

	public void publier(int numéro) {
		this.publiées.add(numéro);
		this.setDirty();
	}

	public int compte() {
		return this.photos.size();
	}

	private static PhotoArchive lire(CompoundTag nbt) {
		PhotoArchive archive = new PhotoArchive();
		archive.suivant = Math.max(1, nbt.getIntOr("next_id", 0));
		CompoundTag rangées = nbt.getCompoundOrEmpty("photos");

		for (String clé : rangées.keySet()) {
			int numéro;
			try {
				numéro = Integer.parseInt(clé);
			} catch (NumberFormatException ignoré) {
				JN1.LOGGER.warn("Numéro de photo illisible : « {} ». Entrée ignorée.", clé);
				continue;
			}

			DataResult<PhotoImage> lu = PhotoImage.CODEC.parse(NbtOps.INSTANCE, rangées.get(clé));
			lu.resultOrPartial(erreur -> JN1.LOGGER.error("Photo {} illisible : {}", clé, erreur))
				.ifPresent(
					image -> {
						if (!image.estCohérente()) {
							JN1.LOGGER
								.warn(
									"Photo {} écartée : {} octets pour un format {} qui en attend {}. Elle vient d'une définition antérieure et le cadre restera vide.",
									new Object[]{clé, image.pixels().length, image.format().getSerializedName(), image.format().octets()}
								);
						} else {
							archive.photos.put(numéro, image);
						}
					}
				);
		}

		CompoundTag relevés = nbt.getCompoundOrEmpty("subjects");
		for (String clé : relevés.keySet()) {
			try {
				int numéro = Integer.parseInt(clé);
				SUJETS.parse(NbtOps.INSTANCE, relevés.get(clé))
						.resultOrPartial(erreur -> JN1.LOGGER.error("Sujets de la photo {} illisibles : {}", clé, erreur))
						.ifPresent(liste -> archive.sujets.put(numéro, liste));
			} catch (NumberFormatException ignoré) {
				JN1.LOGGER.warn("Numéro de photo illisible dans les sujets : « {} ».", clé);
			}
		}
		for (int numéro : nbt.getIntArray("published").orElse(new int[0])) {
			archive.publiées.add(numéro);
		}
		JN1.LOGGER.debug("{} photo(s) relue(s).", archive.photos.size());
		return archive;
	}

	private CompoundTag save(CompoundTag nbt) {
		nbt.putInt("next_id", this.suivant);
		CompoundTag rangées = new CompoundTag();
		this.photos
			.forEach(
				(numéro, image) -> PhotoImage.CODEC
					.encodeStart(NbtOps.INSTANCE, image)
					.resultOrPartial(erreur -> JN1.LOGGER.error("Photo {} non écrite : {}", numéro, erreur))
					.ifPresent(écrit -> rangées.put(String.valueOf(numéro), écrit))
			);
		nbt.put("photos", rangées);
		CompoundTag relevés = new CompoundTag();
		this.sujets.forEach((numéro, liste) -> SUJETS.encodeStart(NbtOps.INSTANCE, liste)
				.resultOrPartial(erreur -> JN1.LOGGER.error("Sujets de la photo {} non écrits : {}", numéro, erreur))
				.ifPresent(écrit -> relevés.put(String.valueOf(numéro), écrit)));
		nbt.put("subjects", relevés);
		nbt.putIntArray("published", this.publiées.stream().mapToInt(Integer::intValue).toArray());
		return nbt;
	}

	public static Codec<PhotoImage> codec() {
		return PhotoImage.CODEC;
	}
}
