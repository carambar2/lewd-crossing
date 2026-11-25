package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
	/** Poussière de gravure qui remonte d'un cuivre à mémoire. */
	public static final SimpleParticleType GLYPH_MOTE = register("glyph_mote");
	/** L'onde d'une fiole de silence. */
	public static final SimpleParticleType SILENCE = register("silence");

	private ModParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(Registries.PARTICLE_TYPE, JN1.id(name), FabricParticleTypes.simple());
	}

	public static void register() {
		JN1.LOGGER.debug("Particules enregistrées.");
	}
}
