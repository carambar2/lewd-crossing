package dev.pyrofetch.jn1.client.render;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Le modèle d'une plateforme d'ascenseur camouflée : il dessine le modèle du
 * bloc qu'elle imite (l'apparence que donne son entité de bloc,
 * {@code getRenderData}), avec la lumière et l'occlusion du chunk comme un
 * vrai bloc ; sans apparence, le sien. Branché sur les modèles de son état de
 * bloc, après leur cuisson ({@code JN1Client}).
 *
 * <p>En 26.3, un modèle d'état de bloc ({@link BlockStateModel}) émet ses
 * facettes par l'API de rendu de Fabric ({@code emitQuads}) ; celui-ci ne se
 * met pas en cache par sa géométrie, qui dépend de l'entité de bloc.
 */
public class CamoModel extends WrapperBlockStateModel {
	public CamoModel(BlockStateModel modèle) {
		super(modèle);
	}

	@Nullable
	private static BlockState imité(BlockAndTintGetter vue, BlockPos pos) {
		return vue.getBlockEntityRenderData(pos) instanceof BlockState imité ? imité : null;
	}

	private static BlockStateModel modèle(BlockState imité) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(imité);
	}

	@Override
	public void emitQuads(QuadEmitter émetteur, BlockAndTintGetter vue, BlockPos pos, BlockState état,
	                      RandomSource hasard, Predicate<Direction> cacher) {
		BlockState imité = imité(vue, pos);
		if (imité != null) {
			modèle(imité).emitQuads(émetteur, vue, pos, imité, hasard, cacher);
			return;
		}
		super.emitQuads(émetteur, vue, pos, état, hasard, cacher);
	}

	/** Sa géométrie suit l'apparence : pas de clé de cache commune (le modèle enveloppé en aurait une). */
	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter vue, BlockPos pos, BlockState état, RandomSource hasard) {
		return null;
	}

	/** Ses matières sont celles du bloc imité (verre, feuillage…), que le chunk range à part. */
	@Override
	public int materialFlags(BlockAndTintGetter vue, BlockPos pos, BlockState état, RandomSource hasard) {
		BlockState imité = imité(vue, pos);
		return imité != null ? modèle(imité).materialFlags(vue, pos, imité, hasard)
				: super.materialFlags(vue, pos, état, hasard);
	}
}
