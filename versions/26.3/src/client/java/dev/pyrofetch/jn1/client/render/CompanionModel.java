package dev.pyrofetch.jn1.client.render;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.CompanionKind;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Les fichiers d'une compagne, et ce qu'on fait de ses os à chaque image :
 * masquage, formes, regard, mannequin et pièce de scène.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Le modèle ne voit plus l'entité :
 * ses fichiers se choisissent sur l'état de rendu, où le rendu a rangé la
 * compagne et sa tenue ({@link #APPARENCE}). Et les os du modèle cuit ne
 * changent plus : chaque image, GeckoLib leur donne des « instantanés »
 * ({@link BoneSnapshot}) neufs, que l'animation remplit et que l'on retouche
 * ensuite ({@link CompanionRenderer#adjustModelBonesForRender}). Un instantané
 * porte l'écart à la pose de repos — rotation, translation, échelle, masquage
 * — et repart de zéro à chaque image : plus rien ne s'empile d'une image à
 * l'autre, d'où la disparition des {@code resetStateChanges} de GeckoLib 4.
 */
public class CompanionModel<T extends CompanionEntity> extends GeoModel<T> {

	/** La compagne et sa tenue, relevées par le rendu dans l'état de rendu. */
	public record Apparence(CompanionKind kind, String outfit) {
	}

	public static final DataTicket<Apparence> APPARENCE = DataTicket.create("jn1_apparence", Apparence.class);

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
	public Identifier getModelResource(GeoRenderState état) {
		Apparence apparence = état.getGeckolibData(APPARENCE);
		return apparence == null ? modèle(CompanionKind.JENNY, "") : modèle(apparence.kind(), apparence.outfit());
	}

	@Override
	public Identifier getTextureResource(GeoRenderState état) {
		Apparence apparence = état.getGeckolibData(APPARENCE);
		return planche(apparence == null ? CompanionKind.JENNY : apparence.kind());
	}

	@Override
	public Identifier getAnimationResource(T animatable) {
		return animations(animatable.kind());
	}

	/**
	 * Le modèle d'une tenue, tel que GeckoLib 5 le range : sans le dossier ni
	 * le suffixe ({@code geo/entity/jenny.geo.json} → {@code jn1:entity/jenny} ;
	 * le build les range sous {@code geckolib/models/}).
	 */
	public static Identifier modèle(CompanionKind kind, String outfit) {
		return JN1.id(nu(kind.modelPath(outfit), "geo/", ".geo.json"));
	}

	public static Identifier planche(CompanionKind kind) {
		return JN1.id(kind.texturePath());
	}

	/** Le fichier d'animations, même rangement ({@code jn1:entity/jenny}). */
	public static Identifier animations(CompanionKind kind) {
		return JN1.id(nu(kind.animationPath(), "animations/", ".animation.json"));
	}

	private static String nu(String chemin, String préfixe, String suffixe) {
		String c = chemin.startsWith(préfixe) ? chemin.substring(préfixe.length()) : chemin;
		return c.endsWith(suffixe) ? c.substring(0, c.length() - suffixe.length()) : c;
	}

	/**
	 * Ce que la compagne fait de ses os à cette image — l'ancien
	 * {@code setCustomAnimations} : appelé par le rendu une fois l'animation
	 * posée.
	 *
	 * @param lacet   l'écart de la tête au corps, en degrés, au signe des os de GeckoLib
	 * @param tangage le tangage de la tête, en degrés, au même signe
	 */
	void poser(BakedGeoModel modèle, BoneSnapshots os, CompanionRenderer.Scène scène, float lacet, float tangage) {
		hideUnusedParts(modèle, os);
		if (scène.partenaire()) {
			revealPartner(modèle, os, scène);
		} else if (scène.pièce()) {
			revealSceneProp(modèle, os, scène.kind().sceneProp());
		}

		applyShape(os, scène.poitrine(), scène.croupe());

		// Plus de ballant calculé de la poitrine (retiré en 0.38.1) : elle ne bouge que si l'animation la bouge.
		// Le code est dans l'historique git, JigglePhysics.java.

		// Elle tourne la tête vers ce qu'elle regarde — le joueur, le plus
		// souvent. Pas pendant une scène : l'animation y tient la tête.
		if (!scène.enScène()) {
			turnHead(os, lacet, tangage);
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
	 * silhouette. Multiplier, et non fixer, garde une échelle animée.
	 */
	public static void applyShape(BoneSnapshots os, int poitrine, int croupe) {
		if (poitrine != 0) {
			échelle(os, "boobs", dev.pyrofetch.jn1.shape.Shapes.Part.BOOBS.facteur(poitrine));
		}
		if (croupe != 0) {
			float f = dev.pyrofetch.jn1.shape.Shapes.Part.BOOTY.facteur(croupe);
			for (String nom : CROUPE) {
				échelle(os, nom, f);
			}
		}
	}

	private static void échelle(BoneSnapshots os, String nom, float f) {
		os.ifPresent(nom, instantané -> instantané.setScale(
				instantané.getScaleX() * f, instantané.getScaleY() * f, instantané.getScaleZ() * f));
	}

	/** Ce que la tête peut tourner par rapport au corps, en degrés. */
	private static final float YAW_LIMIT = 75.0F;
	private static final float PITCH_LIMIT = 80.0F;

	/**
	 * La tête suit le regard : celui de la compagne, ou du joueur transformé.
	 *
	 * <p>Les angles sont <em>ajoutés</em> à ceux de l'animation : la tête garde
	 * ainsi ses mouvements animés. Les signes sont ceux des os de GeckoLib —
	 * l'opposé de ceux du jeu : {@code -yRot} et {@code -xRot} de l'état de
	 * rendu, ce que GeckoLib 4 fournissait déjà prêt ({@code EntityModelData}).
	 * L'écart est borné : une tête qui tourne plus loin que les épaules ne le
	 * permettent se tord.
	 *
	 * <p>Avec GeckoLib 4, il fallait {@code resetStateChanges} après l'ajout,
	 * sans quoi il s'empilait d'image en image et la tête tournait sur
	 * elle-même ; les instantanés de GeckoLib 5 repartent de zéro à chaque image.
	 */
	public static void turnHead(BoneSnapshots os, float lacet, float tangage) {
		float l = Mth.clamp(lacet, -YAW_LIMIT, YAW_LIMIT);
		float t = Mth.clamp(tangage, -PITCH_LIMIT, PITCH_LIMIT);
		os.ifPresent("head", tête -> {
			tête.setRotX(tête.getRotX() + t * Mth.DEG_TO_RAD);
			tête.setRotY(tête.getRotY() + l * Mth.DEG_TO_RAD);
		});
	}

	/** Masque un os, et sa descendance si {@code enfants}. */
	private static void masquer(BoneSnapshots os, GeoBone bone, boolean lui, boolean enfants) {
		BoneSnapshot instantané = os.get(bone);
		instantané.skipRender(lui);
		instantané.skipChildrenRender(enfants);
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
	 * dedans —, mais le rendu relève encore sa place : c'est d'elle qu'on tire
	 * le point des yeux.
	 */
	static void revealPartner(BakedGeoModel modèle, BoneSnapshots os, CompanionRenderer.Scène scène) {
		GeoBone steve = modèle.getBone("steve").orElse(null);
		if (steve == null) {
			return;
		}
		boolean fin = scène.skinFin();
		String pièce = scène.kind().sceneProp();
		java.util.ArrayDeque<GeoBone> pile = new java.util.ArrayDeque<>(java.util.List.of(steve.children()));
		masquer(os, steve, false, false);
		while (!pile.isEmpty()) {
			GeoBone bone = pile.pop();
			String nom = bone.name();
			boolean masqué = (fin && nom.endsWith("Steve")) || (!fin && nom.endsWith("Alex"))
					|| nom.equals("ballL") || nom.equals("ballR")
					|| (nom.equals(pièce) && !scène.pièceDuPartenaire());
			masquer(os, bone, masqué, masqué);
			if (!masqué) {
				pile.addAll(java.util.List.of(bone.children()));
			}
		}
		modèle.getBone("Head2").ifPresent(tête ->
				os.get(tête).skipRender(scène.participe() && scène.premièrePersonne()));
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
	static void revealSceneProp(BakedGeoModel modèle, BoneSnapshots os, @Nullable String name) {
		GeoBone pièce = name == null ? null : modèle.getBone(name).orElse(null);
		if (pièce == null) {
			return;
		}
		masquer(os, pièce, false, false);
		GeoBone enfant = pièce;
		for (GeoBone parent = pièce.parent(); parent != null; enfant = parent, parent = parent.parent()) {
			masquer(os, parent, true, false);
			for (GeoBone frère : parent.children()) {
				if (frère != enfant) {
					masquer(os, frère, true, true);
				}
			}
			if (java.util.Arrays.asList(HIDDEN_SUBTREES).contains(parent.name())) {
				break;
			}
		}
	}

	/**
	 * À refaire à chaque image, avant les retouches de scène : les instantanés
	 * de GeckoLib repartent de zéro.
	 *
	 * <p>Aussi appelé par le modèle du joueur transformé, bâti sur les mêmes
	 * squelettes.
	 */
	public static void hideUnusedParts(BakedGeoModel modèle, BoneSnapshots os) {
		for (String name : HIDDEN_SUBTREES) {
			modèle.getBone(name).ifPresent(bone -> masquer(os, bone, true, true));
		}
		for (String name : HIDDEN_BONES) {
			modèle.getBone(name).ifPresent(bone -> masquer(os, bone, true, true));
		}
		modèle.getBone(ITEM_ANCHOR).ifPresent(ancre -> {
			masquer(os, ancre, false, false);
			for (GeoBone porté : ancre.children()) {
				masquer(os, porté, true, true);
			}
		});
	}
}
