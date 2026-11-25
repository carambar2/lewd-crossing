package dev.pyrofetch.jn1.client.render;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.function.Supplier;

/**
 * Le modèle d'une plateforme d'ascenseur camouflée : il dessine le modèle du
 * bloc qu'elle imite (l'apparence que donne son entité de bloc,
 * {@code getRenderData}), avec la lumière et l'occlusion du chunk comme un
 * vrai bloc ; sans apparence, le sien. Branché sur les modèles de son état de
 * bloc, après leur cuisson ({@code JN1Client}).
 */
public class CamoModel extends ForwardingBakedModel {
	public CamoModel(BakedModel modèle) {
		this.wrapped = modèle;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView vue, BlockState état, BlockPos pos, Supplier<Random> hasard,
	                           RenderContext contexte) {
		if (vue.getBlockEntityRenderData(pos) instanceof BlockState imité) {
			BakedModel modèle = MinecraftClient.getInstance().getBlockRenderManager().getModel(imité);
			modèle.emitBlockQuads(vue, imité, pos, hasard, contexte);
			return;
		}
		super.emitBlockQuads(vue, état, pos, hasard, contexte);
	}
}
