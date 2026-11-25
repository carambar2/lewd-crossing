package dev.pyrofetch.jn1.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.pyrofetch.jn1.JN1;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Réglages du mod, dans {@code config/jn1.json}. Écrit avec ses valeurs par
 * défaut au premier lancement.
 */
public final class JN1Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("jn1.json");

	private static JN1Config instance;

	// --- Dialogue ---
	private boolean ambientDialogue = true;
	private boolean actionbarEcho = true;
	private float dialogueDurationScale = 1.0F;

	// --- Fin B : décohérence du réseau ---
	private boolean decoherence = true;
	private float sculkSpreadChance = 0.35F;
	private float wardenSurfaceChance = 0.05F;

	public static JN1Config get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static void reload() {
		instance = load();
	}

	private static JN1Config load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				JN1Config loaded = GSON.fromJson(reader, JN1Config.class);
				if (loaded != null) {
					// Réécrite aussitôt : une clé ajoutée depuis le dernier
					// lancement y paraît avec sa valeur par défaut.
					loaded.save();
					return loaded;
				}
			} catch (IOException | RuntimeException exception) {
				JN1.LOGGER.error("Config illisible, retour aux valeurs par défaut.", exception);
			}
		}

		JN1Config fresh = new JN1Config();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			JN1.LOGGER.error("Écriture de la config impossible.", exception);
		}
	}

	public boolean ambientDialogueEnabled() {
		return this.ambientDialogue;
	}

	public boolean actionbarEcho() {
		return this.actionbarEcho;
	}

	public float dialogueDurationScale() {
		return Math.clamp(this.dialogueDurationScale, 0.25F, 4.0F);
	}

	public boolean decoherenceEnabled() {
		return this.decoherence;
	}

	public float sculkSpreadChance() {
		return Math.clamp(this.sculkSpreadChance, 0.0F, 1.0F);
	}

	public float wardenSurfaceChance() {
		return Math.clamp(this.wardenSurfaceChance, 0.0F, 1.0F);
	}
}
