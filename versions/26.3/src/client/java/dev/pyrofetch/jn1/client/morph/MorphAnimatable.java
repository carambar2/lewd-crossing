package dev.pyrofetch.jn1.client.morph;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.morph.MorphState;
import net.minecraft.world.entity.player.Player;

/**
 * Le corps d'emprunt d'un joueur transformé.
 *
 * <p>Un seul objet pour tous les joueurs : GeckoLib tient l'état d'animation
 * par identifiant d'entité. Le rendu lui dit, juste avant d'en relever les
 * animations, quelle compagne et quelle tenue montrer ({@link #montrer}).
 *
 * <p>La posture se déduit de ce que fait le joueur — ou du geste qu'il a
 * demandé ({@link ClientMorphs#gesture}) —, avec les mêmes rôles que
 * les compagnes ({@link CompanionKind.Role}) : un rôle qu'une compagne ne sait
 * pas tenir retombe sur la pose de repos.
 *
 * <p><strong>GeckoLib 5 (26.3).</strong> Les contrôleurs sont consultés
 * pendant le relevé de l'état de rendu ; le joueur leur parvient par cet état
 * ({@link #JOUEUR}, rangé par {@link MorphRenderer}), comme
 * {@code DataTickets.ENTITY} le faisait en GeckoLib 4.
 */
public final class MorphAnimatable implements GeoReplacedEntity {
	public static final MorphAnimatable INSTANCE = new MorphAnimatable();

	/** Le joueur dont on relève l'état de rendu. */
	public static final DataTicket<Player> JOUEUR = DataTicket.create("jn1_morph_joueur", Player.class);

	/** Au-delà de cette descente par tick, on tombe. */
	private static final double FALL_SPEED = 0.3;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private CompanionKind kind = CompanionKind.JENNY;
	private String outfit = "";

	private MorphAnimatable() {
	}

	void montrer(MorphState état) {
		CompanionKind compagne = état.companion();
		this.kind = compagne == null ? CompanionKind.JENNY : compagne;
		this.outfit = état.currentOutfit();
	}

	public CompanionKind kind() {
		return this.kind;
	}

	public String outfit() {
		return this.outfit;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Le dernier geste vu par ce contrôleur-ci : un par joueur, GeckoLib en
		// tenant un jeu par entité.
		int[] gesteVu = {-1};
		controllers.add(new AnimationController<MorphAnimatable>("posture", 5, test -> {
			if (test.getData(JOUEUR) instanceof Player joueur) {
				ClientMorphs.Geste geste = ClientMorphs.gesture(joueur);
				if (geste != null && test.isMoving()) {
					ClientMorphs.stopGesture(joueur);
				} else if (geste != null && this.kind.animation(geste.geste().role()) != null) {
					if (geste.numéro() != gesteVu[0]) {
						gesteVu[0] = geste.numéro();
						// Le même geste redemandé reprend au début.
						test.controller().reset();
					}
					return jouer(test, geste.geste().role(), geste.geste().loops());
				}
			}
			return this.posture(test);
		}).setSoundKeyframeHandler(événement -> {
			// Les repères du mod d'origine ne servent qu'aux filles : le joueur a
			// déjà les bruits du jeu.
		}));
		// Le coup porté : le premier des trois gestes de riposte.
		controllers.add(new AnimationController<MorphAnimatable>("gesture", 2, test -> {
			if (test.getData(JOUEUR) instanceof Player joueur && joueur.isSwinging()) {
				return jouer(test, CompanionKind.Role.SWIPE_A, false);
			}
			return PlayState.STOP;
		}).setSoundKeyframeHandler(événement -> {
		}));
	}

	private PlayState posture(AnimationTest<MorphAnimatable> test) {
		if (!(test.getData(JOUEUR) instanceof Player joueur)) {
			return PlayState.STOP;
		}
		CompanionKind.Role rôle;
		if (joueur.isPassenger()) {
			rôle = CompanionKind.Role.SIT;
		} else if (joueur.isInWater() || joueur.isSwimming()) {
			rôle = CompanionKind.Role.SWIM;
		} else if (joueur.isFallFlying()
				|| (!joueur.onGround() && !joueur.getAbilities().flying && joueur.getY() - joueur.yo < -FALL_SPEED)) {
			rôle = CompanionKind.Role.FALL;
		} else if (test.isMoving()) {
			rôle = joueur.isSprinting() ? CompanionKind.Role.RUN : CompanionKind.Role.WALK;
		} else {
			rôle = CompanionKind.Role.IDLE;
		}
		return jouer(test, rôle, true);
	}

	/** Joue le rôle, sinon la pose de repos, sinon rien. */
	private PlayState jouer(AnimationTest<MorphAnimatable> test, CompanionKind.Role rôle, boolean boucle) {
		String nom = this.kind.animation(rôle);
		if (nom == null && boucle) {
			nom = this.kind.animation(CompanionKind.Role.IDLE);
		}
		if (nom == null) {
			return PlayState.STOP;
		}
		return test.setAndContinue(boucle ? RawAnimation.begin().thenLoop(nom) : RawAnimation.begin().thenPlay(nom));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
