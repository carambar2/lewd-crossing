package dev.pyrofetch.jn1.client.lab;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.pyrofetch.jn1.JN1;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Le dessin du ciel du vide, à la manière de l'ancien {@code Tesselator}.
 *
 * <p>En 1.21.1, le ciel se dessinait en mode immédiat : une planche, un mélange
 * ({@code blendFunc}), des quadrilatères envoyés aussitôt. Depuis 1.21.5, tout
 * passe par des « pipelines » (shader, mélange, profondeur, faces) et des
 * passes de rendu. Cette classe rend la même chose : {@link #commencer} ouvre un
 * tampon pour une planche, {@link #dessiner} l'envoie dans la passe du ciel, avec
 * le mélange choisi par {@link #additif} ou {@link #translucide}.
 *
 * <p>Comme avant : pas de profondeur écrite ni lue, pas d'élimination des faces
 * arrière (on est dans le tube), la couleur des sommets module la planche.
 * Les sommets sont donnés autour de l'œil, dans les axes du monde ; la rotation
 * de la vue est celle de la passe (la pile du modèle de {@code RenderSystem}).
 */
final class DessinCiel {
	/** Lumière ajoutée : {@code SRC_ALPHA, ONE}, ce qui est noir ne dessine rien. */
	private static final RenderPipeline ADDITIF = pipeline("void_sky_additive", BlendFunction.LIGHTNING);
	/** Le mélange ordinaire, celui de {@code defaultBlendFunc}. */
	private static final RenderPipeline TRANSLUCIDE = pipeline("void_sky_translucent", BlendFunction.TRANSLUCENT);

	/** Le tampon partagé, comme celui du {@code Tesselator} : un dessin à la fois. */
	private static final ByteBufferBuilder OCTETS = new ByteBufferBuilder(786432);
	/**
	 * Les tampons de sommets des deux dernières images : rendus au pilote une fois
	 * que la carte graphique a pu s'en servir.
	 */
	private static final List<GpuBuffer> IMAGE_PRÉCÉDENTE = new ArrayList<>();
	private static final List<GpuBuffer> CETTE_IMAGE = new ArrayList<>();

	/**
	 * Les planches, résolues avant d'ouvrir la passe : la première demande d'une
	 * texture la charge (une écriture sur la carte graphique), ce qui est
	 * interdit tant qu'une passe est ouverte (« Close the existing render pass
	 * before performing additional commands »).
	 */
	private static final Map<Identifier, AbstractTexture> PRÊTES = new HashMap<>();
	/** Les planches demandées sans avoir été annoncées : chargées avant la passe suivante. */
	private static final Set<Identifier> MANQUANTES = new HashSet<>();
	/** Assez d'indices pour le plus gros dessin du ciel (le tube : 24 × 48 quadrilatères). */
	private static final int INDICES = 6 * 4096;

	@Nullable
	private static RenderPass passe;
	private static RenderPipeline mélange = TRANSLUCIDE;
	@Nullable
	private static Identifier planche;

	private DessinCiel() {
	}

	private static RenderPipeline pipeline(String nom, BlendFunction fonction) {
		return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
				.withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.PROJECTION)
				.withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.DYNAMIC_TRANSFORMS)
				.withLocation(JN1.id("pipeline/" + nom))
				.withVertexShader("core/position_tex_color")
				.withFragmentShader("core/position_tex_color")
				.withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0)
				.withColorTargetState(new ColorTargetState(fonction))
				.withCull(false)
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				.build();
	}

	/**
	 * Ouvre la passe du ciel sur la cible principale, le temps de {@code dessin}.
	 * Tout ce qui écrit sur la carte graphique se fait avant : le chargement des
	 * {@code planches} que le dessin emploiera, et la taille du tampon d'indices.
	 */
	static void passe(RenderTarget cible, Iterable<Identifier> planches, Runnable dessin) {
		for (GpuBuffer vieux : IMAGE_PRÉCÉDENTE) {
			vieux.close();
		}
		IMAGE_PRÉCÉDENTE.clear();
		IMAGE_PRÉCÉDENTE.addAll(CETTE_IMAGE);
		CETTE_IMAGE.clear();
		var textures = Minecraft.getInstance().getTextureManager();
		for (Identifier id : planches) {
			PRÊTES.put(id, textures.getTexture(id));
		}
		for (Identifier id : MANQUANTES) {
			PRÊTES.put(id, textures.getTexture(id));
		}
		MANQUANTES.clear();
		RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(INDICES);
		try (RenderPass rp = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "jn1 void sky", cible.getColorTextureView(), Optional.empty(),
				cible.getDepthTextureView(), OptionalDouble.empty())) {
			RenderSystem.bindDefaultUniforms(rp);
			passe = rp;
			mélange = TRANSLUCIDE;
			dessin.run();
		} finally {
			passe = null;
		}
	}

	/** Lumière ajoutée, pour ce qui suit (l'ancien {@code blendFunc(SRC_ALPHA, ONE)}). */
	static void additif() {
		mélange = ADDITIF;
	}

	/** Mélange ordinaire, pour ce qui suit (l'ancien {@code defaultBlendFunc}). */
	static void translucide() {
		mélange = TRANSLUCIDE;
	}

	/** Un tampon de quadrilatères texturés et colorés, pour cette planche. */
	static BufferBuilder commencer(Identifier texture) {
		planche = texture;
		return new BufferBuilder(OCTETS, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
	}

	/** Envoie le tampon ; un tampon vide ne dessine rien. */
	static void dessiner(BufferBuilder tampon) {
		try (MeshData maillage = tampon.build()) {
			if (maillage == null || passe == null || planche == null) {
				return;
			}
			AbstractTexture texture = PRÊTES.get(planche);
			if (texture == null) {
				// Pas annoncée : on ne peut pas la charger pendant la passe ; elle
				// le sera avant la suivante.
				MANQUANTES.add(planche);
				return;
			}
			RenderPass rp = passe;
			GpuBuffer sommets = RenderSystem.getDevice().createBuffer(() -> "jn1 void sky vertices",
					GpuBuffer.USAGE_VERTEX, maillage.vertexBuffer());
			CETTE_IMAGE.add(sommets);
			int indices = maillage.drawState().indexCount();
			RenderSystem.AutoStorageIndexBuffer séquence = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
			GpuBuffer tamponIndices = séquence.getBuffer(indices);
			rp.setPipeline(RenderSystem.getCompiledPipeline(mélange));
			RenderSystem.bindDefaultUniforms(rp);
			rp.setUniform("DynamicTransforms",
					RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy()));
			rp.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
			rp.setVertexBuffer(0, sommets.slice());
			rp.setIndexBuffer(tamponIndices, séquence.type());
			rp.drawIndexed(indices, 1, 0, 0, 0);
		}
	}
}
