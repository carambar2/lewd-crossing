package dev.pyrofetch.jn1.client.morph;

import dev.pyrofetch.jn1.entity.CompanionKind;
import dev.pyrofetch.jn1.morph.MorphState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Le corps d'emprunt d'un joueur transformé.
 *
 * <p>Un seul objet pour tous les joueurs : GeckoLib tient l'état d'animation
 * par identifiant d'entité. Le rendu lui dit, juste avant de dessiner, quelle
 * compagne et quelle tenue montrer ({@link #montrer}).
 *
 * <p>La posture se déduit de ce que fait le joueur — ou du geste qu'il a
 * demandé ({@link ClientMorphs#gesture}) —, avec les mêmes rôles que
 * les compagnes ({@link CompanionKind.Role}) : un rôle qu'une compagne ne sait
 * pas tenir retombe sur la pose de repos.
 */
public final class MorphAnimatable implements GeoReplacedEntity {
	public static final MorphAnimatable INSTANCE = new MorphAnimatable();

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
	public EntityType<?> getReplacingEntityType() {
		return EntityType.PLAYER;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Le dernier geste vu par ce contrôleur-ci : un par joueur, GeckoLib en
		// tenant un jeu par entité.
		int[] gesteVu = {-1};
		controllers.add(new AnimationController<>(this, "posture", 5, state -> {
			if (state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur) {
				ClientMorphs.Geste geste = ClientMorphs.gesture(joueur);
				if (geste != null && state.isMoving()) {
					ClientMorphs.stopGesture(joueur);
				} else if (geste != null && this.kind.animation(geste.geste().role()) != null) {
					if (geste.numéro() != gesteVu[0]) {
						gesteVu[0] = geste.numéro();
						state.getController().forceAnimationReset();
					}
					return jouer(state, geste.geste().role(), geste.geste().loops());
				}
			}
			return this.posture(state);
		}).setSoundKeyframeHandler(événement -> {
			// Les repères du mod d'origine ne servent qu'aux filles : le joueur a
			// déjà les bruits du jeu.
		}));
		// Le coup porté : le premier des trois gestes de riposte.
		controllers.add(new AnimationController<>(this, "gesture", 2, state -> {
			if (state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur && joueur.handSwinging) {
				return jouer(state, CompanionKind.Role.SWIPE_A, false);
			}
			return PlayState.STOP;
		}).setSoundKeyframeHandler(événement -> {
		}));
	}

	private PlayState posture(AnimationState<MorphAnimatable> state) {
		if (!(state.getData(DataTickets.ENTITY) instanceof PlayerEntity joueur)) {
			return PlayState.STOP;
		}
		CompanionKind.Role rôle;
		if (joueur.hasVehicle()) {
			rôle = CompanionKind.Role.SIT;
		} else if (joueur.isTouchingWater() || joueur.isSwimming()) {
			rôle = CompanionKind.Role.SWIM;
		} else if (joueur.isFallFlying()
				|| (!joueur.isOnGround() && !joueur.getAbilities().flying && joueur.getY() - joueur.prevY < -FALL_SPEED)) {
			rôle = CompanionKind.Role.FALL;
		} else if (state.isMoving()) {
			rôle = joueur.isSprinting() ? CompanionKind.Role.RUN : CompanionKind.Role.WALK;
		} else {
			rôle = CompanionKind.Role.IDLE;
		}
		return jouer(state, rôle, true);
	}

	/** Joue le rôle, sinon la pose de repos, sinon rien. */
	private PlayState jouer(AnimationState<MorphAnimatable> state, CompanionKind.Role rôle, boolean boucle) {
		String nom = this.kind.animation(rôle);
		if (nom == null && boucle) {
			nom = this.kind.animation(CompanionKind.Role.IDLE);
		}
		if (nom == null) {
			return PlayState.STOP;
		}
		return state.setAndContinue(boucle ? RawAnimation.begin().thenLoop(nom) : RawAnimation.begin().thenPlay(nom));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
