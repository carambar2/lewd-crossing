package dev.pyrofetch.jn1.worldgen;

import dev.pyrofetch.jn1.JN1;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.structure.StructureType;

public final class ModStructures {
	/** La maison, qui vérifie le terrain avant de se poser. Voir {@link HouseStructure}. */
	public static final StructureType<HouseStructure> HOUSE = register("house");

	private ModStructures() {
	}

	private static StructureType<HouseStructure> register(String name) {
		return Registry.register(Registries.STRUCTURE_TYPE, JN1.id(name),
				() -> HouseStructure.CODEC);
	}

	public static void register() {
		JN1.LOGGER.debug("Types de structures enregistrés.");
	}
}
