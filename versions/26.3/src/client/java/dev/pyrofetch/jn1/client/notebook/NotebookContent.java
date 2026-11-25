package dev.pyrofetch.jn1.client.notebook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pyrofetch.jn1.JN1;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Le contenu du carnet, lu dans {@code assets/jn1/notebook/carnet.json}.
 *
 * <p>Écrit en données plutôt qu'en code : une page, c'est une clé de texte, une
 * figure éventuelle et un drapeau qui la débloque. Relu à chaque ouverture — le
 * fichier est petit, et un pack de ressources peut le remplacer entre-temps.
 *
 * <p>Une figure est une petite scène : des blocs ({@code "bloc"}, écrits comme
 * dans une commande, états compris) et des objets ({@code "objet"}), chacun à
 * sa position en blocs. Un élément illisible est écarté et signalé au journal,
 * sans empêcher le carnet de s'ouvrir.
 */
final class NotebookContent {
	/** Un élément de figure : un bloc, ou un objet posé en son centre. */
	record Element(@Nullable BlockState bloc, @Nullable ItemStack objet, Vector3f position) {
	}

	/**
	 * @param taille      pixels d'interface par bloc
	 * @param inclinaison la plongée du regard, en degrés
	 */
	record Figure(List<Element> éléments, float taille, float inclinaison) {
		/** Le centre de la scène, pour la faire tourner sur elle-même. */
		Vector3f centre() {
			Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
			for (Element e : this.éléments) {
				min.min(e.position());
				max.max(new Vector3f(e.position()).add(1, 1, 1));
			}
			return min.add(max).mul(0.5F);
		}
	}

	/**
	 * @param texte     clé de traduction
	 * @param débloque  drapeau de progression qui l'écrit, ou {@code null}
	 * @param alt       écrite dans l'alphabet de la table d'enchantement
	 * @param chapitre  rang du chapitre, pour le sommaire
	 */
	record Page(String texte, @Nullable Figure figure, @Nullable String débloque, boolean alt, int chapitre) {
	}

	/** Un chapitre et la première de ses pages (rang dans le carnet entier). */
	record Chapitre(String titre, int premièrePage) {
	}

	final List<Page> pages = new ArrayList<>();
	final List<Chapitre> chapitres = new ArrayList<>();

	/** Les pages de contenu, numérotées à partir de {@code départ} (couverture et sommaire avant). */
	static NotebookContent load(int départ) {
		NotebookContent contenu = new NotebookContent();
		Identifier chemin = JN1.id("notebook/carnet.json");
		Resource ressource = Minecraft.getInstance().getResourceManager().getResource(chemin).orElse(null);
		if (ressource == null) {
			JN1.LOGGER.warn("Carnet : {} introuvable", chemin);
			return contenu;
		}
		try (Reader lecteur = ressource.openAsReader()) {
			JsonArray chapitres = JsonParser.parseReader(lecteur).getAsJsonObject().getAsJsonArray("chapitres");
			for (int c = 0; c < chapitres.size(); c++) {
				JsonObject chapitre = chapitres.get(c).getAsJsonObject();
				contenu.chapitres.add(new Chapitre(chapitre.get("titre").getAsString(), départ + contenu.pages.size()));
				for (JsonElement élément : chapitre.getAsJsonArray("pages")) {
					JsonObject page = élément.getAsJsonObject();
					contenu.pages.add(new Page(page.get("texte").getAsString(),
							page.has("figure") ? figure(page.getAsJsonObject("figure")) : null,
							page.has("debloque") ? page.get("debloque").getAsString() : null,
							page.has("police") && page.get("police").getAsString().equals("alt"), c));
				}
			}
		} catch (Exception exception) {
			JN1.LOGGER.warn("Carnet : lecture impossible de {}", chemin, exception);
		}
		return contenu;
	}

	private static Figure figure(JsonObject objet) {
		List<Element> éléments = new ArrayList<>();
		for (JsonElement brut : objet.getAsJsonArray("elements")) {
			JsonObject e = brut.getAsJsonObject();
			JsonArray p = e.getAsJsonArray("pos");
			Vector3f position = new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat());
			try {
				if (e.has("bloc")) {
					BlockState état = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK,
							e.get("bloc").getAsString(), false).blockState();
					éléments.add(new Element(état, null, position));
				} else if (e.has("objet")) {
					Identifier id = Identifier.parse(e.get("objet").getAsString());
					if (BuiltInRegistries.ITEM.containsKey(id)) {
						éléments.add(new Element(null, new ItemStack(BuiltInRegistries.ITEM.getValue(id)), position));
					} else {
						JN1.LOGGER.warn("Carnet : objet inconnu {}", id);
					}
				}
			} catch (Exception exception) {
				JN1.LOGGER.warn("Carnet : élément illisible {}", e, exception);
			}
		}
		float taille = objet.has("taille") ? objet.get("taille").getAsFloat() : 30.0F;
		float inclinaison = objet.has("inclinaison") ? objet.get("inclinaison").getAsFloat() : 25.0F;
		return new Figure(List.copyOf(éléments), taille, inclinaison);
	}
}
