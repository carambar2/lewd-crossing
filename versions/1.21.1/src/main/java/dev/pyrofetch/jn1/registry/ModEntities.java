package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleEntity;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.AuraEntity;
import dev.pyrofetch.jn1.entity.EllieEntity;
import dev.pyrofetch.jn1.entity.JennyEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<JennyEntity> JENNY = companion("jenny", JennyEntity::new);
	public static final EntityType<EllieEntity> ELLIE = companion("ellie", EllieEntity::new);
	/** Aura, l'IA du laboratoire : trouvée dans le vide, jamais vendue. */
	public static final EntityType<AuraEntity> AURA = companion("aura", AuraEntity::new);
	/** Les trois partagent gabarit et portée de suivi : seul le modèle change. */
	private static <T extends CompanionEntity> EntityType<T> companion(
			String path, EntityType.EntityFactory<T> factory) {
		return Registry.register(Registries.ENTITY_TYPE, JN1.id(path),
				EntityType.Builder.create(factory, SpawnGroup.MISC)
						.dimensions(0.6F, 1.93F)
						.maxTrackingRange(24)
						.build(path));
	}

	/** La capsule en vol. Petite, et suivie de loin pour rester visible. */
	public static final EntityType<CapsuleEntity> CAPSULE = Registry.register(
			Registries.ENTITY_TYPE,
			JN1.id("capsule"),
			EntityType.Builder.<CapsuleEntity>create(CapsuleEntity::new, SpawnGroup.MISC)
					.dimensions(0.25F, 0.25F)
					.maxTrackingRange(64)
					.trackingTickInterval(10)
					.build("capsule")
	);

	/** La flèche collante en vol : le gabarit et le suivi d'une flèche. */
	public static final EntityType<dev.pyrofetch.jn1.cum.CumArrowEntity> CUM_ARROW = Registry.register(
			Registries.ENTITY_TYPE,
			JN1.id("cum_arrow"),
			EntityType.Builder.<dev.pyrofetch.jn1.cum.CumArrowEntity>create(
							dev.pyrofetch.jn1.cum.CumArrowEntity::new, SpawnGroup.MISC)
					.dimensions(0.5F, 0.5F)
					.eyeHeight(0.13F)
					.maxTrackingRange(4)
					.trackingTickInterval(20)
					.build("cum_arrow")
	);

	/** La fiole à jeter en vol : celui d'une potion jetable. */
	public static final EntityType<dev.pyrofetch.jn1.cum.CumFlaskEntity> SPLASH_CUM = Registry.register(
			Registries.ENTITY_TYPE,
			JN1.id("splash_cum"),
			EntityType.Builder.<dev.pyrofetch.jn1.cum.CumFlaskEntity>create(
							dev.pyrofetch.jn1.cum.CumFlaskEntity::new, SpawnGroup.MISC)
					.dimensions(0.25F, 0.25F)
					.maxTrackingRange(4)
					.trackingTickInterval(10)
					.build("splash_cum")
	);

	/**
	 * La faille : haute comme une porte, suivie de loin — on doit la voir
	 * briller au bout de la plaine avant d'être dessus.
	 */
	public static final EntityType<dev.pyrofetch.jn1.rift.RiftEntity> RIFT = Registry.register(
			Registries.ENTITY_TYPE,
			JN1.id("rift"),
			EntityType.Builder.<dev.pyrofetch.jn1.rift.RiftEntity>create(
							dev.pyrofetch.jn1.rift.RiftEntity::new, SpawnGroup.MISC)
					.dimensions(1.0F, 2.6F)
					.makeFireImmune()
					.maxTrackingRange(10)
					.trackingTickInterval(20)
					.build("rift")
	);

	/**
	 * Une plateforme d'ascenseur en route ({@link dev.pyrofetch.jn1.lift.PlatformEntity}) :
	 * ses blocs, portés entre deux étages. Sa boîte est son emprise (les
	 * dimensions ci-dessous ne servent qu'avant qu'elle la connaisse). Sa
	 * hauteur, client et serveur la calculent chacun de l'heure du monde.
	 */
	public static final EntityType<dev.pyrofetch.jn1.lift.PlatformEntity> LIFT_PLATFORM =
			Registry.register(
					Registries.ENTITY_TYPE,
					JN1.id("lift_platform"),
					EntityType.Builder.<dev.pyrofetch.jn1.lift.PlatformEntity>create(
									dev.pyrofetch.jn1.lift.PlatformEntity::new, SpawnGroup.MISC)
							.dimensions(1.0F, 1.0F)
							.makeFireImmune()
							.maxTrackingRange(10)
							.trackingTickInterval(5)
							.build("lift_platform")
			);

	/** Gaster, derrière l'arbre du vide : une apparition, pas une compagne. */
	public static final EntityType<dev.pyrofetch.jn1.gaster.GasterEntity> GASTER = Registry.register(
			Registries.ENTITY_TYPE,
			JN1.id("gaster"),
			EntityType.Builder.create(dev.pyrofetch.jn1.gaster.GasterEntity::new, SpawnGroup.MISC)
					.dimensions(0.8F, 2.5F)
					.makeFireImmune()
					.maxTrackingRange(10)
					.build("gaster")
	);

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(JENNY, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(ELLIE, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(AURA, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(GASTER, dev.pyrofetch.jn1.gaster.GasterEntity.createAttributes());
		JN1.LOGGER.debug("Entités enregistrées.");
	}
}
