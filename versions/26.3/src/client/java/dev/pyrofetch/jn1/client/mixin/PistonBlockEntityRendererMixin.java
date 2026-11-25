package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Le dessin d'un piston en mouvement. Il reconnaît la tête qui sort à
 * {@code is(PISTON_HEAD)} (pour la dessiner avec sa tige), le socle collant
 * à {@code is(STICKY_PISTON)}, et reconstitue la tête qui rentre à partir de
 * {@code PISTON_HEAD}. Les nôtres passent par les mêmes chemins.
 *
 * <p>26.3 : ces tests se font à la relève de l'état de rendu
 * ({@code extractRenderState}), plus au dessin. {@code is(Block)} est la
 * méthode générique {@code is(T)} de {@code TypedInstance} : son descripteur
 * prend un {@code Object} (vérifié dans le bytecode).
 */
@Mixin(PistonHeadRenderer.class)
public abstract class PistonBlockEntityRendererMixin {
	private static final String RELEVÉ = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

	@Redirect(method = RELEVÉ, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean jn1$reconnaître(BlockState état, Object bloc) {
		if (bloc == Blocks.PISTON_HEAD && état.is(ModBlocks.CUMMY_PISTON_HEAD)) {
			return true;
		}
		if (bloc == Blocks.STICKY_PISTON && état.is(ModBlocks.CUMMY_PISTON)) {
			return true;
		}
		return état.is((Block) bloc);
	}

	@Redirect(method = RELEVÉ, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block jn1$tête(PistonMovingBlockEntity piston, PistonHeadRenderState état, float instant, Vec3 caméra,
	                       ModelFeatureRenderer.CrumblingOverlay casse) {
		return piston.getMovedState().is(ModBlocks.CUMMY_PISTON) ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}
}
