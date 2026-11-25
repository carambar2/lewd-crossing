package dev.pyrofetch.jn1.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Jenny. Tout le comportement vient de {@link CompanionEntity}. */
public class JennyEntity extends CompanionEntity {
	public JennyEntity(EntityType<? extends JennyEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.JENNY;
	}
}
