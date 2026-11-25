package dev.pyrofetch.jn1.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/** Ellie. Tout le comportement vient de {@link CompanionEntity}. */
public class EllieEntity extends CompanionEntity {
	public EllieEntity(EntityType<? extends EllieEntity> type, World world) {
		super(type, world);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.ELLIE;
	}
}
