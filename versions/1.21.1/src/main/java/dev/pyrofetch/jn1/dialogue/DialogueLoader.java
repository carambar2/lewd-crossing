package dev.pyrofetch.jn1.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge {@code data/<namespace>/dialogue/*.json} à chaque {@code /reload}.
 *
 * <p>Un fichier peut contenir une entrée seule ou un tableau d'entrées — un
 * « pack » de répliques. Écrire du lore ne demande donc jamais de recompiler.
 */
public final class DialogueLoader implements SimpleSynchronousResourceReloadListener {
	public static final Identifier LISTENER_ID = JN1.id("dialogue");
	private static final String DIRECTORY = "dialogue";

	private static final DialogueLoader INSTANCE = new DialogueLoader();

	private List<DialogueEntry> entries = List.of();
	private Map<String, DialogueEntry> byId = Map.of();

	private DialogueLoader() {
	}

	public static DialogueLoader getInstance() {
		return INSTANCE;
	}

	public static void register() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(INSTANCE);
	}

	public List<DialogueEntry> entries() {
		return this.entries;
	}

	public DialogueEntry byId(String id) {
		return this.byId.get(id);
	}

	@Override
	public Identifier getFabricId() {
		return LISTENER_ID;
	}

	@Override
	public void reload(ResourceManager manager) {
		List<DialogueEntry> loaded = new ArrayList<>();
		Map<String, DialogueEntry> index = new HashMap<>();

		Map<Identifier, Resource> found = manager.findResources(
				DIRECTORY, path -> path.getPath().endsWith(".json"));

		for (Map.Entry<Identifier, Resource> resource : found.entrySet()) {
			try (BufferedReader reader = resource.getValue().getReader()) {
				JsonElement root = stripNulls(JsonParser.parseReader(reader));
				if (root == null) {
					continue;
				}
				List<JsonObject> objects = new ArrayList<>();
				if (root.isJsonArray()) {
					for (JsonElement element : root.getAsJsonArray()) {
						if (element.isJsonObject()) {
							objects.add(element.getAsJsonObject());
						}
					}
				} else if (root.isJsonObject()) {
					objects.add(root.getAsJsonObject());
				}

				for (JsonObject object : objects) {
					DialogueEntry.CODEC.parse(JsonOps.INSTANCE, object)
							.resultOrPartial(error -> JN1.LOGGER.error(
									"Entrée de dialogue invalide dans {} : {}", resource.getKey(), error))
							.ifPresent(entry -> {
								if (!entry.isPlayable()) {
									JN1.LOGGER.warn("L'entrée « {} » n'a aucune réplique jouable.", entry.id());
									return;
								}
								if (index.containsKey(entry.id())) {
									JN1.LOGGER.warn("Identifiant de dialogue en double : « {} ». "
											+ "La dernière définition l'emporte.", entry.id());
								}
								index.put(entry.id(), entry);
							});
				}
			} catch (Exception exception) {
				JN1.LOGGER.error("Lecture impossible du fichier de dialogue {}", resource.getKey(), exception);
			}
		}

		loaded.addAll(index.values());
		loaded.sort(Comparator.comparingInt(DialogueEntry::priority).reversed()
				.thenComparing(DialogueEntry::id));

		this.entries = List.copyOf(loaded);
		this.byId = Map.copyOf(index);
		JN1.LOGGER.info("{} entrée(s) de dialogue chargée(s).", this.entries.size());
	}

	/**
	 * La spec écrit les champs non contraints à {@code null} plutôt que de les
	 * omettre. Les codecs refusent un champ présent mais nul, donc on nettoie
	 * l'arbre avant de décoder : les deux écritures deviennent équivalentes.
	 */
	private static JsonElement stripNulls(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		if (element.isJsonObject()) {
			JsonObject cleaned = new JsonObject();
			for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
				JsonElement value = stripNulls(field.getValue());
				if (value != null) {
					cleaned.add(field.getKey(), value);
				}
			}
			return cleaned;
		}
		if (element.isJsonArray()) {
			JsonArray cleaned = new JsonArray();
			for (JsonElement item : element.getAsJsonArray()) {
				JsonElement value = stripNulls(item);
				if (value != null) {
					cleaned.add(value);
				}
			}
			return cleaned;
		}
		return element;
	}
}
