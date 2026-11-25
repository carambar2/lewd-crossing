package dev.pyrofetch.jn1.client.mixin;

import dev.pyrofetch.jn1.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.PistonBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Le dessin d'un piston en mouvement. Il reconnaît la tête qui sort à
 * {@code isOf(PISTON_HEAD)} (pour la dessiner avec sa tige), le socle collant
 * à {@code isOf(STICKY_PISTON)}, et reconstitue la tête qui rentre à partir de
 * {@code PISTON_HEAD}. Les nôtres passent par les mêmes chemins.
 */
@Mixin(PistonBlockEntityRenderer.class)
public abstract class PistonBlockEntityRendererMixin {
	private static final String RENDER = "render(Lnet/minecraft/block/entity/PistonBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V";

	@Redirect(method = RENDER, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
	private boolean jn1$reconnaître(BlockState état, Block bloc) {
		if (bloc == Blocks.PISTON_HEAD && état.isOf(ModBlocks.CUMMY_PISTON_HEAD)) {
			return true;
		}
		if (bloc == Blocks.STICKY_PISTON && état.isOf(ModBlocks.CUMMY_PISTON)) {
			return true;
		}
		return état.isOf(bloc);
	}

	@Redirect(method = RENDER, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
			target = "Lnet/minecraft/block/Blocks;PISTON_HEAD:Lnet/minecraft/block/Block;"))
	private Block jn1$tête(PistonBlockEntity piston, float tickDelta, MatrixStack matrices,
	                       VertexConsumerProvider vertices, int light, int overlay) {
		return piston.getPushedBlock().isOf(ModBlocks.CUMMY_PISTON) ? ModBlocks.CUMMY_PISTON_HEAD : Blocks.PISTON_HEAD;
	}
}
