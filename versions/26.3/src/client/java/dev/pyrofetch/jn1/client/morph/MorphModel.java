package dev.pyrofetch.jn1.client.morph;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import dev.pyrofetch.jn1.client.render.CompanionModel;
import dev.pyrofetch.jn1.entity.CompanionKind;
import net.minecraft.resources.Identifier;

/**
 * Les fichiers de la compagne dont le joueur a pris l'apparence, masquage compris.
 *
 * <p>En GeckoLib 5, le modèle et la planche se lisent sur l'état de rendu
 * ({@link CompanionModel#APPARENCE}, rangé par {@link MorphRenderer}) ; les
 * animations sur l'animatable, que le rendu vient de régler.
 */
public class MorphModel extends GeoModel<MorphAnimatable> {
	/** L'inclinaison du buste accroupi, en radians : celle du joueur du jeu. */
	private static final float ACCROUPI = 0.5F;

	@Override
	public Identifier getModelResource(GeoRenderState état) {
		CompanionModel.Apparence apparence = état.getGeckolibData(CompanionModel.APPARENCE);
		return apparence == null ? CompanionModel.modèle(CompanionKind.JENNY, "")
				: CompanionModel.modèle(apparence.kind(), apparence.outfit());
	}

	@Override
	public Identifier getTextureResource(GeoRenderState état) {
		CompanionModel.Apparence apparence = état.getGeckolibData(CompanionModel.APPARENCE);
		return CompanionModel.planche(apparence == null ? CompanionKind.JENNY : apparence.kind());
	}

	@Override
	public Identifier getAnimationResource(MorphAnimatable animatable) {
		return CompanionModel.animations(animatable.kind());
	}

	/**
	 * L'ancien {@code setCustomAnimations} : le mannequin, les emplacements
	 * d'armure et les objets modélisés masqués comme chez les compagnes ; ses
	 * formes ; puis il suit le joueur.
	 */
	void poser(BakedGeoModel modèle, BoneSnapshots os, MorphRenderer.État état) {
		CompanionModel.hideUnusedParts(modèle, os);
		CompanionModel.applyShape(os, état.poitrine, état.croupe);
		this.suivreLeJoueur(os, état);
	}

	/**
	 * La tête suit le regard du joueur ({@link CompanionModel#turnHead}, commun
	 * aux compagnes), les bras ce qu'il fait de ses mains, le buste s'incline
	 * quand il s'accroupit.
	 */
	private void suivreLeJoueur(BoneSnapshots os, MorphRenderer.État état) {
		// Les signes des os de GeckoLib sont l'opposé de ceux du jeu.
		float lacet = -état.yRot;
		float tangage = -état.xRot;
		CompanionModel.turnHead(os, lacet, tangage);
		if (état.bras != null) {
			MorphArmPoses.apply(os, état.bras, lacet, tangage);
		}
		if (état.accroupi) {
			// Même sens que la tête qui regarde vers le bas : vers l'avant.
			os.ifPresent("upperBody", buste -> buste.setRotX(buste.getRotX() - ACCROUPI));
		}
	}
}
