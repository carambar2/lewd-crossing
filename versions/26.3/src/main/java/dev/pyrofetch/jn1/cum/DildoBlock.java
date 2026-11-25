package dev.pyrofetch.jn1.cum;

import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;

/**
 * Le dildo : un end rod collé au Cummy piston le devient
 * ({@link #convertir}), tourné comme lui. Le piston le pousse et le ramène ;
 * sur une horloge de redstone, c'est une machine.
 *
 * <p>À chaque poussée — il arrive par un piston en mouvement, le socle à deux
 * blocs derrière lui —, une fille qui se tient devant
 * réagit : cœurs, bruit, un gémissement une fois sur trois, et de loin en
 * loin ({@link #CHANCE_FIN}) elle finit comme au bout d'une scène « cu ».
 */
public class DildoBlock extends RodBlock {
	// 26.3 : les blocs n'ont plus de codec (`codec()` a disparu).
	private static final String[] CRIS = {"moan", "ahh", "mmm"};
	/** Une poussée sur… */
	private static final int CHANCE_FIN = 20;

	/** Formes de contour selon l'axe : plus larges pour le cheval et Excalibur. */
	private final net.minecraft.world.phys.shapes.VoxelShape formeY, formeZ, formeX;

	public DildoBlock(Properties settings) {
		this(settings, 2.0);
	}

	/** {@code demiLargeur} : la moitié de l'épaisseur de la tige, en pixels. */
	public DildoBlock(Properties settings, double demiLargeur) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP));
		double a = 8 - demiLargeur, b = 8 + demiLargeur;
		this.formeY = Block.box(a, 0, a, b, 16, b);
		this.formeZ = Block.box(a, a, 0, b, b, 16);
		this.formeX = Block.box(0, a, a, 16, b, b);
	}

	@Override
	protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world,
	                                                              BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return switch (state.getValue(FACING).getAxis()) {
			case X -> this.formeX;
			case Z -> this.formeZ;
			case Y -> this.formeY;
		};
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getClickedFace());
	}

	/**
	 * Un end rod devant la face {@code face} d'un Cummy piston (ou de sa tête),
	 * pointé dans le même sens, devient un dildo. Appelé par le socle et la tête
	 * quand un voisin change.
	 */
	public static void convertir(Level world, BlockPos devant, Direction face) {
		BlockState état = world.getBlockState(devant);
		if (world.isClientSide() || !état.is(Blocks.END_ROD) || état.getValue(FACING) != face) {
			return;
		}
		world.setBlock(devant, ModBlocks.DILDO.defaultBlockState().setValue(FACING, face), Block.UPDATE_ALL);
		// Le succès revient à qui est là : on ne sait pas qui a posé l'end rod.
		net.minecraft.world.entity.player.Player témoin = world.getNearestPlayer(
				devant.getX() + 0.5, devant.getY() + 0.5, devant.getZ() + 0.5, 8.0, false);
		if (témoin != null) {
			dev.pyrofetch.jn1.progress.Succes.accorder(témoin, "dildo");
		}
		world.playSound(null, devant, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
		if (world instanceof ServerLevel monde) {
			monde.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
					devant.getX() + 0.5, devant.getY() + 0.5, devant.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onPlace(state, world, pos, ancien, notify);
		if (world.isClientSide() || !ancien.is(Blocks.MOVING_PISTON)) {
			return;
		}
		// Une poussée le laisse à deux blocs du socle (la tête entre eux) ; ramené,
		// il est contre lui. Le socle ne bouge pas quand il sort, alors que la tête
		// et le socle qui rentre peuvent être encore en mouvement à cet instant.
		if (world.getBlockState(pos.relative(state.getValue(FACING).getOpposite(), 2)).is(ModBlocks.CUMMY_PISTON)) {
			this.pousser(world, pos, state.getValue(FACING));
		}
	}

	/**
	 * Les dégâts d'Excalibur : rien ne s'y oppose — ni armure, ni totem, ni
	 * créatif, ni l'invulnérabilité de FapGPT (étiquettes de
	 * {@code data/minecraft/tags/damage_type}). Le message de mort parle
	 * d'une déchirure ({@code death.attack.jn1.excalibur}).
	 */
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DÉCHIRURE =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
					dev.pyrofetch.jn1.JN1.id("excalibur"));

	private void pousser(Level world, BlockPos pos, Direction face) {
		AABB devant = new AABB(pos.relative(face)).expandTowards(face.getStepX(), face.getStepY(), face.getStepZ())
				.inflate(0.3);
		// Excalibur, poussé par un piston : tout ce qui vit devant lui meurt sur le coup.
		if (this == ModBlocks.EXCALIBUR) {
			var source = world.damageSources().source(DÉCHIRURE);
			for (net.minecraft.world.entity.LivingEntity victime : world.getEntitiesOfClass(
					net.minecraft.world.entity.LivingEntity.class, devant, v -> v.isAlive() && !v.isSpectator())) {
				victime.hurt(source, Float.MAX_VALUE);
				world.playSound(null, victime.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.BLOCKS, 1.2F, 0.6F);
				if (world instanceof ServerLevel monde) {
					monde.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, victime.getX(),
							victime.getY(0.5), victime.getZ(), 20, 0.3, 0.4, 0.3, 0.4);
				}
			}
			return;
		}
		// Le mème : un mouton au bout du piston. Il bêle, rosit, et c'est un succès secret.
		for (net.minecraft.world.entity.animal.sheep.Sheep mouton : world.getEntitiesOfClass(
				net.minecraft.world.entity.animal.sheep.Sheep.class, devant, m -> !m.isRemoved())) {
			world.playSound(null, mouton.blockPosition(), SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.6F);
			mouton.setColor(net.minecraft.world.item.DyeColor.PINK);
			if (world instanceof ServerLevel monde) {
				monde.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
						mouton.getX(), mouton.getY() + mouton.getBbHeight(), mouton.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			}
			for (net.minecraft.world.entity.player.Player témoin : world.players()) {
				if (témoin.distanceToSqr(mouton) < 24 * 24) {
					dev.pyrofetch.jn1.progress.Succes.accorder(témoin, "mouton");
				}
			}
		}
		for (CompanionEntity fille : world.getEntitiesOfClass(CompanionEntity.class, devant, f -> !f.isRemoved())) {
			fille.showHearts(2);
			SoundEvent bruit = GirlSounds.bruit("smallinserts");
			if (bruit != null) {
				world.playSound(null, fille.blockPosition(), bruit, SoundSource.NEUTRAL, 0.8F, 1.0F);
			}
			if (fille.getRandom().nextInt(3) == 0) {
				GirlSounds.crier(fille, CRIS[fille.getRandom().nextInt(CRIS.length)], 1.0F);
			}
			if (fille.getRandom().nextInt(CHANCE_FIN) == 0) {
				GirlSounds.crier(fille, "ahh", 1.0F);
				Cum.jaillir(fille);
			}
		}
	}
}
