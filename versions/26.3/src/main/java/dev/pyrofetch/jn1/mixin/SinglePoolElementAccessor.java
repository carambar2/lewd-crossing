package dev.pyrofetch.jn1.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Le gabarit d'une pièce de structure.
 *
 * <p>Le jeu ne le donne pas : il ne l'ouvre que pour poser la pièce. Le lidar
 * en a besoin pour savoir où tombera la porte d'une cave, dans une maison dont
 * seul le départ existe (voir {@link dev.pyrofetch.jn1.lab.VoidLabs}).
 */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
	@Accessor("template")
	Either<Identifier, StructureTemplate> jn1$location();
}
