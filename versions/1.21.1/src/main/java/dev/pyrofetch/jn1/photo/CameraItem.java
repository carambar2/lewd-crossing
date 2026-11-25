package dev.pyrofetch.jn1.photo;

import dev.pyrofetch.jn1.network.OpenCameraPayload;
import dev.pyrofetch.jn1.registry.ModBlocks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Item.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * L'appareil photo. En main, il ouvre le viseur ; utilisé sur un trépied, il
 * s'y pose, et l'on vise alors depuis le trépied.
 *
 * <p>Reconstituée depuis le jar 0.32.0, dont les sources ont été perdues :
 * les commentaires de détail d'origine n'ont pas survécu.
 */
public class CameraItem extends Item {
	public CameraItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity joueur = context.getPlayer();
		if (joueur != null && joueur.isSneaking()) {
			World monde = context.getWorld();
			BlockPos pos = context.getBlockPos().offset(context.getSide());
			ItemPlacementContext pose = new ItemPlacementContext(context);
			if (!monde.getBlockState(pos).canReplace(pose)) {
				return ActionResult.FAIL;
			}

			if (monde.isClient) {
				return ActionResult.SUCCESS;
			}

			BlockState pied = ModBlocks.CAMERA_STAND.getDefaultState().with(Properties.HORIZONTAL_FACING, joueur.getHorizontalFacing().getOpposite());
			monde.setBlockState(pos, pied);
			monde.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_LANTERN_PLACE, SoundCategory.BLOCKS, 0.7F, 1.4F);
			if (!joueur.getAbilities().creativeMode) {
				context.getStack().decrement(1);
			}

			return ActionResult.SUCCESS;
		} else {
			return ActionResult.PASS;
		}
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack pile = user.getStackInHand(hand);
		if (user.isSneaking()) {
			return TypedActionResult.pass(pile);
		}

		if (!world.isClient && user instanceof ServerPlayerEntity serveur) {
			ServerPlayNetworking.send(serveur, OpenCameraPayload.enMain());
		}

		return TypedActionResult.success(pile, world.isClient);
	}
}
