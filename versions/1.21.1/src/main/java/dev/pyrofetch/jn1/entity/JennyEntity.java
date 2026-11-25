package dev.pyrofetch.jn1.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/** Jenny. Tout le comportement vient de {@link CompanionEntity}. */
public class JennyEntity extends CompanionEntity {
	public JennyEntity(EntityType<? extends JennyEntity> type, World world) {
		super(type, world);
	}

	@Override
	public CompanionKind kind() {
		return CompanionKind.JENNY;
	}
}
