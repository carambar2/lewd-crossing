package dev.pyrofetch.jn1.client.render;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.model.GeoModel;

public class CompanionModel<T extends CompanionEntity> extends GeoModel<T> {

	/**
	 * Sous-arbres entiers à ne jamais afficher.
	 *
	 * <p>Le modèle embarque un personnage joueur complet et un porte-objet, que
	 * l'on n'affiche que dans des scènes précises. Sans consigne, GeckoLib rend
	 * tout ce qu'il trouve : c'est le grand mannequin qui apparaissait à côté
	 * d'elle.
	 *
	 * <p>Le porte-objet n'y est plus : il sert de point d'accroche au cadeau
	 * qu'elle tient (voir {@link #ITEM_ANCHOR}).
	 */
	private static final String[] HIDDEN_SUBTREES = {"steve"};

	/**
	 * Le porte-objet : on garde l'os, pour que {@link GiftLayer} y accroche le
	 * cadeau reçu, mais on masque les objets modélisés qu'il porte.
	 */
	private static final String ITEM_ANCHOR = "items";

	/**
	 * Pièces éparpillées dans le squelette du corps, à masquer une par une.
	 *
	 * <p>Les {@code armor*} sont les emplacements d'équipement : dans un mod qui
	 * gère l'armure, ils s'allument selon ce que porte le personnage. Ici rien ne
	 * les pilote, donc ils s'affichaient tous en même temps — d'où l'armure
	 * turquoise.
	 *
	 * <p>Les {@code boobPreview*} figuraient dans cette liste, pris pour des
	 * repères de modélisation. Ils portent en réalité treize cubes chacun : la
	 * peau du décolleté. Les masquer laissait un creux à leur place chez Jenny et
	 * Allie, mais pas chez Ellie, dont les os équivalents s'appellent
	 * {@code preview*} et échappaient donc à la liste.
	 */
	private static final String[] HIDDEN_BONES = {
			"armorHelmet", "armorBoobs", "armorShoulderR", "armorShoulderL", "armorChest",
			"armorBootyL", "armorBootyR", "armorHip",
			"armorPantsUpL", "armorPantsLowL", "armorShoesL",
			"armorPantsUpR", "armorPantsLowR", "armorShoesR",
	};

	// Chaque compagne désigne ses propres fichiers : un seul modèle générique
	// suffit alors pour les trois.
	//
	// La géométrie est la seule chose qui change d'une tenue à l'autre : la
	// planche et les animations restent celles de la compagne, tous ses modèles
	// étant bâtis sur le même squelette et les mêmes coordonnées de texture.
	@Override
	public Identifier getModelResource(T animatable) {
		return JN1.id(animatable.kind().modelPath(animatable.getOutfit()));
	}

	@Override
	public Identifier getTextureResource(T animatable) {
		return JN1.id(animatable.kind().texturePath());
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return JN1.id(animatable.kind().animationPath());
	}

	@Override
	public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
		super.setCustomAnimations(animatable, instanceId, state);

		hideUnusedParts(this.getAnimationProcessor());
		if (animatable.showsScenePartner()) {
			revealPartner(this.getAnimationProcessor(), animatable);
		} else if (animatable.showsSceneProp()) {
			revealSceneProp(this.getAnimationProcessor(), animatable.kind().sceneProp());
		}

		applyShape(this.getAnimationProcessor(),
				animatable.getShape(dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS),
				animatable.getShape(dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY));

		// Plus de ballant calculé de la poitrine (retiré en 0.38.1) : elle ne bouge que si l'animation la bouge.
		// Le code est dans l'historique git, JigglePhysics.java.

		// Elle tourne la tête vers ce qu'elle regarde — le joueur, le plus
		// souvent. Pas pendant une scène : l'animation y tient la tête.
		if (!animatable.hasScene()) {
			turnHead(this.getAnimationProcessor(), state.getData(DataTickets.ENTITY_MODEL_DATA));
		}
	}

	/** Les os de la croupe : flancs et courbes des cuisses font la silhouette. */
	private static final String[] CROUPE = {"sideL", "sideR", "curvesL", "curvesR"};

	/**
	 * Les potions de forme : une échelle sur la poitrine et sur la croupe, autour
	 * de leurs pivots, multipliée à celle de l'animation.
	 *
	 * <p>La croupe ne touche pas à {@code booty} : ses facettes ont des pivots
	 * loin du corps, et l'agrandir les éparpillait jusqu'aux épaules — vu au
	 * rendu. Ce sont les flancs et les courbes des cuisses qui font la
	 * silhouette. Multiplier, et non fixer, garde une échelle animée ; d'où
	 * {@code resetStateChanges}, sans quoi l'échelle s'empilerait d'image en
	 * image comme le regard l'a fait.
	 */
	public static void applyShape(AnimationProcessor<?> processor, int poitrine, int croupe) {
		if (poitrine != 0) {
			échelle(processor.getBone("boobs"), dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS.facteur(poitrine));
		}
		if (croupe != 0) {
			float f = dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY.facteur(croupe);
			for (String nom : CROUPE) {
				échelle(processor.getBone(nom), f);
			}
		}
	}

	private static void échelle(@org.jetbrains.annotations.Nullable GeoBone os, float f) {
		if (os == null) {
			return;
		}
		os.setScaleX(os.getScaleX() * f);
		os.setScaleY(os.getScaleY() * f);
		os.setScaleZ(os.getScaleZ() * f);
		os.resetStateChanges();
	}

	/** Ce que la tête peut tourner par rapport au corps, en degrés. */
	private static final float YAW_LIMIT = 75.0F;
	private static final float PITCH_LIMIT = 80.0F;

	/**
	 * La tête suit le regard : celui de la compagne, ou du joueur transformé.
	 *
	 * <p>Les angles sont <em>ajoutés</em> à ceux de l'animation, là où GeckoLib
	 * les substitue ({@code DefaultedEntityGeoModel}) : la tête garde ainsi ses
	 * mouvements animés. Les signes sont ceux de GeckoLib, qui fournit l'écart
	 * entre tête et corps et le tangage déjà prêts pour ses os. L'écart est
	 * borné : une tête qui tourne plus loin que les épaules ne le permettent se
	 * tord.
	 *
	 * <p><strong>Attention :</strong> {@code setRotX} marque l'os comme animé, et
	 * GeckoLib n'efface ses marques qu'<em>avant</em> cet appel. À l'image
	 * suivante, un os que l'animation en cours ne pilote pas passait donc pour
	 * déjà traité et n'était pas remis au repos : l'ajout s'empilait d'image en
	 * image et la tête tournait sur elle-même. C'est ce qui avait fait retirer le
	 * suivi du regard des compagnes. D'où {@code resetStateChanges} après
	 * l'ajout — à faire après toute retouche d'os.
	 */
	public static void turnHead(AnimationProcessor<?> processor, @org.jetbrains.annotations.Nullable EntityModelData regard) {
		GeoBone tête = processor.getBone("head");
		if (regard == null || tête == null) {
			return;
		}
		float lacet = MathHelper.clamp(regard.netHeadYaw(), -YAW_LIMIT, YAW_LIMIT);
		float tangage = MathHelper.clamp(regard.headPitch(), -PITCH_LIMIT, PITCH_LIMIT);
		tête.setRotX(tête.getRotX() + tangage * MathHelper.RADIANS_PER_DEGREE);
		tête.setRotY(tête.getRotY() + lacet * MathHelper.RADIANS_PER_DEGREE);
		tête.resetStateChanges();
	}

	/**
	 * Un joueur tient la place de {@code steve} : le mannequin paraît, habillé de
	 * son skin par {@link CompanionRenderer}.
	 *
	 * <p>Le modèle porte les deux jeux de bras d'un joueur, larges ({@code *Steve})
	 * et fins ({@code *Alex}) : on garde ceux du skin. La pièce de scène suit son
	 * réglage ; les os qui l'accompagnent sous le torse ({@code ballL},
	 * {@code ballR}) restent masqués. À la première personne, la tête du
	 * mannequin disparaît pour le joueur qui voit par ses yeux — la caméra est
	 * dedans —, mais l'os garde sa matrice suivie : c'est d'elle qu'on tire le
	 * point des yeux.
	 */
	static void revealPartner(AnimationProcessor<?> processor, CompanionEntity compagne) {
		GeoBone steve = processor.getBone("steve");
		if (steve == null) {
			return;
		}
		boolean fin = SceneView.skin(compagne).model() == net.minecraft.client.util.SkinTextures.Model.SLIM;
		java.util.ArrayDeque<GeoBone> pile = new java.util.ArrayDeque<>(steve.getChildBones());
		steve.setHidden(false);
		steve.setChildrenHidden(false);
		while (!pile.isEmpty()) {
			GeoBone os = pile.pop();
			String nom = os.getName();
			boolean masqué = (fin && nom.endsWith("Steve")) || (!fin && nom.endsWith("Alex"))
					|| nom.equals("ballL") || nom.equals("ballR")
					|| (nom.equals(compagne.kind().sceneProp())
							&& (!compagne.isScenePropShown() || compagne.isScenePartnerFemale()));
			os.setHidden(masqué);
			os.setChildrenHidden(masqué);
			if (!masqué) {
				pile.addAll(os.getChildBones());
			}
		}
		GeoBone tête = processor.getBone("Head2");
		if (tête != null) {
			tête.setTrackingMatrices(true);
			tête.setHidden(SceneView.participating() == compagne && SceneView.firstPerson());
		}
	}

	/**
	 * Fait paraître la pièce de scène, prise dans un sous-arbre masqué.
	 *
	 * <p>GeckoLib ne descend pas sous un os qui cache ses enfants : chaque
	 * ancêtre de la pièce jusqu'à la racine masquée garde ses propres cubes
	 * cachés mais laisse passer vers ses enfants, dont seuls ceux qui mènent à
	 * la pièce restent visibles. Le reste du mannequin demeure invisible.
	 * L'état se refait à chaque image, comme le masquage qu'il corrige : il
	 * vaut pour cette entité seulement.
	 */
	static void revealSceneProp(AnimationProcessor<?> processor, String name) {
		GeoBone pièce = processor.getBone(name);
		if (pièce == null) {
			return;
		}
		pièce.setHidden(false);
		pièce.setChildrenHidden(false);
		GeoBone enfant = pièce;
		for (GeoBone parent = pièce.getParent(); parent != null; enfant = parent, parent = parent.getParent()) {
			parent.setHidden(true);
			parent.setChildrenHidden(false);
			for (GeoBone frère : parent.getChildBones()) {
				if (frère != enfant) {
					frère.setHidden(true);
					frère.setChildrenHidden(true);
				}
			}
			if (java.util.Arrays.asList(HIDDEN_SUBTREES).contains(parent.getName())) {
				break;
			}
		}
	}

	/**
	 * À refaire à chaque image : l'animation en cours peut réafficher un os, et
	 * le cache d'instances de GeckoLib est partagé entre les entités du même type.
	 */
	/** Aussi appelé par le modèle du joueur transformé, bâti sur les mêmes squelettes. */
	public static void hideUnusedParts(AnimationProcessor<?> processor) {
		for (String name : HIDDEN_SUBTREES) {
			GeoBone bone = processor.getBone(name);
			if (bone != null) {
				bone.setHidden(true);
				bone.setChildrenHidden(true);
			}
		}
		for (String name : HIDDEN_BONES) {
			GeoBone bone = processor.getBone(name);
			if (bone != null) {
				bone.setHidden(true);
				bone.setChildrenHidden(true);
			}
		}
		GeoBone ancre = processor.getBone(ITEM_ANCHOR);
		if (ancre != null) {
			ancre.setHidden(false);
			ancre.setChildrenHidden(false);
			for (GeoBone porté : ancre.getChildBones()) {
				porté.setHidden(true);
				porté.setChildrenHidden(true);
			}
		}
	}
}
