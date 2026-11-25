package dev.pyrofetch.jn1.registry;

import dev.pyrofetch.jn1.JN1;
import dev.pyrofetch.jn1.block.PaperNoteBlockEntity;
import dev.pyrofetch.jn1.lab.StasisPodBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
	public static final BlockEntityType<StasisPodBlockEntity> STASIS_POD =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("stasis_pod"),
					BlockEntityType.Builder.create(StasisPodBlockEntity::new,
							ModBlocks.CAPSULE_POD).build());

	/** Ce qu'une note a d'écrit dessus. */
	public static final BlockEntityType<PaperNoteBlockEntity> NOTE =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("note"),
					BlockEntityType.Builder.create(PaperNoteBlockEntity::new,
							ModBlocks.NOTE).build());

	/** Le tirage accroché à un cadre. */
	public static final BlockEntityType<dev.pyrofetch.jn1.block.PhotoFrameBlockEntity> PHOTO_FRAME =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("photo_frame"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.block.PhotoFrameBlockEntity::new,
							ModBlocks.PHOTO_FRAME).build());

	/** L'appareil posé sur un trépied. */
	public static final BlockEntityType<dev.pyrofetch.jn1.block.CameraStandBlockEntity> CAMERA_STAND =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("camera_stand"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.block.CameraStandBlockEntity::new,
							ModBlocks.CAMERA_STAND).build());

	/** Le texte d'un écran mural. */
	public static final BlockEntityType<dev.pyrofetch.jn1.block.WallScreenBlockEntity> WALL_SCREEN =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("wall_screen"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.block.WallScreenBlockEntity::new,
							ModBlocks.WALL_SCREEN).build());

	/** Un bloc de plateforme d'ascenseur : son apparence, son groupe. */
	public static final BlockEntityType<dev.pyrofetch.jn1.lift.PlatformBlockEntity> LIFT_PLATFORM =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("lift_platform"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.lift.PlatformBlockEntity::new,
							ModBlocks.LIFT_PLATFORM).build());

	/** Un sélecteur d'étage : son décalage, le nom de l'étage. */
	public static final BlockEntityType<dev.pyrofetch.jn1.lift.SelectorBlockEntity> LIFT_SELECTOR =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("lift_selector"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.lift.SelectorBlockEntity::new,
							ModBlocks.LIFT_SELECTOR).build());

	/** Le terminal : vide, il ne sert qu'à faire battre la radio côté client. */
	public static final BlockEntityType<dev.pyrofetch.jn1.block.ComputerBlockEntity> COMPUTER =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("computer"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.block.ComputerBlockEntity::new,
							ModBlocks.COMPUTER).build());

	/** Le stabilisateur de faille : vide, pour le faisceau que le client dessine. */
	public static final BlockEntityType<dev.pyrofetch.jn1.rift.RiftStabilizerBlockEntity> RIFT_STABILIZER =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("rift_stabilizer"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.rift.RiftStabilizerBlockEntity::new,
							ModBlocks.RIFT_STABILIZER).build());

	/** La brèche : elle tient sa faille ouverte et contamine alentour. */
	public static final BlockEntityType<dev.pyrofetch.jn1.rift.BreachBlockEntity> BREACH =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("breach"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.rift.BreachBlockEntity::new,
							ModBlocks.BREACH).build());

	/** Le projecteur d'Aura : il retient où il l'a fait paraître. */
	public static final BlockEntityType<dev.pyrofetch.jn1.rift.AuraProjectorBlockEntity> AURA_PROJECTOR =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("aura_projector"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.rift.AuraProjectorBlockEntity::new,
							ModBlocks.AURA_PROJECTOR).build());

	/** Les rayons d'un distributeur garni à la main. */
	public static final BlockEntityType<dev.pyrofetch.jn1.block.VendingMachineBlockEntity> VENDING_MACHINE =
			Registry.register(Registries.BLOCK_ENTITY_TYPE, JN1.id("vending_machine"),
					BlockEntityType.Builder.create(dev.pyrofetch.jn1.block.VendingMachineBlockEntity::new,
							ModBlocks.VENDING_MACHINE).build());

	private ModBlockEntities() {
	}

	public static void register() {
		JN1.LOGGER.debug("Blocs-entités enregistrés.");
	}
}
