package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.cum.CumArrowEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

/** La flèche collante : le modèle de la flèche du jeu, sa planche nappée de blanc. */
public class CumArrowRenderer extends ProjectileEntityRenderer<CumArrowEntity> {
	private static final Identifier PLANCHE = JN1.id("textures/entity/projectiles/cum_arrow.png");

	public CumArrowRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(CumArrowEntity flèche) {
		return PLANCHE;
	}
}
