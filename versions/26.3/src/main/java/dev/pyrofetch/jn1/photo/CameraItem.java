package dev.pyrofetch.jn1.photo;

import dev.pyrofetch.jn1.network.OpenCameraPayload;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * L'appareil photo. En main, il ouvre le viseur ; utilisé sur un trépied, il
 * s'y pose, et l'on vise alors depuis le trépied.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class CameraItem extends Item {
	public CameraItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player joueur = context.getPlayer();
		if (joueur != null && joueur.isShiftKeyDown()) {
			Level monde = context.getLevel();
			BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
			BlockPlaceContext pose = new BlockPlaceContext(context);
			if (!monde.getBlockState(pos).canBeReplaced(pose)) {
				return InteractionResult.FAIL;
			}

			if (monde.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			BlockState pied = ModBlocks.CAMERA_STAND.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, joueur.getDirection().getOpposite());
			monde.setBlockAndUpdate(pos, pied);
			monde.playSound((Player)null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
			if (!joueur.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}

	@Override
	public InteractionResult use(Level world, Player user, InteractionHand hand) {
		ItemStack pile = user.getItemInHand(hand);
		if (user.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		if (!world.isClientSide() && user instanceof ServerPlayer serveur) {
			ServerPlayNetworking.send(serveur, OpenCameraPayload.enMain());
		}

		return InteractionResult.SUCCESS;
	}
}
