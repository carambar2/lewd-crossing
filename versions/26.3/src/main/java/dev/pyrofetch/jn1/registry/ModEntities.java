package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.capsule.CapsuleEntity;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.entity.AuraEntity;
import dev.pyrofetch.jn1.entity.EllieEntity;
import dev.pyrofetch.jn1.entity.JennyEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<JennyEntity> JENNY = companion("jenny", JennyEntity::new);
	public static final EntityType<EllieEntity> ELLIE = companion("ellie", EllieEntity::new);
	/** Aura, l'IA du laboratoire : trouvée dans le vide, jamais vendue. */
	public static final EntityType<AuraEntity> AURA = companion("aura", AuraEntity::new);
	/** Les trois partagent gabarit et portée de suivi : seul le modèle change. */
	private static <T extends CompanionEntity> EntityType<T> companion(
			String path, EntityType.EntityFactory<T> factory) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, JN1.id(path),
				EntityType.Builder.of(factory, MobCategory.MISC)
						.sized(0.6F, 1.93F)
						.clientTrackingRange(24)
						.build(clé(path)));
	}

	/** La capsule en vol. Petite, et suivie de loin pour rester visible. */
	public static final EntityType<CapsuleEntity> CAPSULE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JN1.id("capsule"),
			EntityType.Builder.<CapsuleEntity>of(CapsuleEntity::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(64)
					.updateInterval(10)
					.build(clé("capsule"))
	);

	/** La flèche collante en vol : le gabarit et le suivi d'une flèche. */
	public static final EntityType<dev.pyrofetch.jn1.cum.CumArrowEntity> CUM_ARROW = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JN1.id("cum_arrow"),
			EntityType.Builder.<dev.pyrofetch.jn1.cum.CumArrowEntity>of(
							dev.pyrofetch.jn1.cum.CumArrowEntity::new, MobCategory.MISC)
					.sized(0.5F, 0.5F)
					.eyeHeight(0.13F)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(clé("cum_arrow"))
	);

	/** La fiole à jeter en vol : celui d'une potion jetable. */
	public static final EntityType<dev.pyrofetch.jn1.cum.CumFlaskEntity> SPLASH_CUM = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JN1.id("splash_cum"),
			EntityType.Builder.<dev.pyrofetch.jn1.cum.CumFlaskEntity>of(
							dev.pyrofetch.jn1.cum.CumFlaskEntity::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.build(clé("splash_cum"))
	);

	/**
	 * La faille : haute comme une porte, suivie de loin — on doit la voir
	 * briller au bout de la plaine avant d'être dessus.
	 */
	public static final EntityType<dev.pyrofetch.jn1.rift.RiftEntity> RIFT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JN1.id("rift"),
			EntityType.Builder.<dev.pyrofetch.jn1.rift.RiftEntity>of(
							dev.pyrofetch.jn1.rift.RiftEntity::new, MobCategory.MISC)
					.sized(1.0F, 2.6F)
					.fireImmune()
					.clientTrackingRange(10)
					.updateInterval(20)
					.build(clé("rift"))
	);

	/**
	 * Une plateforme d'ascenseur en route ({@link dev.pyrofetch.jn1.lift.PlatformEntity}) :
	 * ses blocs, portés entre deux étages. Sa boîte est son emprise (les
	 * dimensions ci-dessous ne servent qu'avant qu'elle la connaisse). Sa
	 * hauteur, client et serveur la calculent chacun de l'heure du monde.
	 */
	public static final EntityType<dev.pyrofetch.jn1.lift.PlatformEntity> LIFT_PLATFORM =
			Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					JN1.id("lift_platform"),
					EntityType.Builder.<dev.pyrofetch.jn1.lift.PlatformEntity>of(
									dev.pyrofetch.jn1.lift.PlatformEntity::new, MobCategory.MISC)
							.sized(1.0F, 1.0F)
							.fireImmune()
							.clientTrackingRange(10)
							.updateInterval(5)
							.build(clé("lift_platform"))
			);

	/** Gaster, derrière l'arbre du vide : une apparition, pas une compagne. */
	public static final EntityType<dev.pyrofetch.jn1.gaster.GasterEntity> GASTER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JN1.id("gaster"),
			EntityType.Builder.of(dev.pyrofetch.jn1.gaster.GasterEntity::new, MobCategory.MISC)
					.sized(0.8F, 2.5F)
					.fireImmune()
					.clientTrackingRange(10)
					.build(clé("gaster"))
	);

	private ModEntities() {
	}

	/** 26.3 : un type d'entité se construit avec sa clé de registre (et non plus son nom). */
	private static ResourceKey<EntityType<?>> clé(String path) {
		return ResourceKey.create(Registries.ENTITY_TYPE, JN1.id(path));
	}

	public static void register() {
		// Le sérialiseur des blocs portés par la plateforme d'ascenseur (26.3 : plus de NBT suivi dans le jeu).
		dev.pyrofetch.jn1.lift.PlatformEntity.enregistrerSérialiseur();
		FabricDefaultAttributeRegistry.register(JENNY, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(ELLIE, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(AURA, CompanionEntity.createJennyAttributes());
		FabricDefaultAttributeRegistry.register(GASTER, dev.pyrofetch.jn1.gaster.GasterEntity.createAttributes());
		JN1.LOGGER.debug("Entités enregistrées.");
	}
}
