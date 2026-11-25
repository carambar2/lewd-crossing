package dev.pyrofetch.jn1.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Ellie. Tout le comportement vient de {@link CompanionEntity}. */
public class EllieEntity extends CompanionEntity {
	public EllieEntity(EntityType<? extends EllieEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.ELLIE;
	}
}
