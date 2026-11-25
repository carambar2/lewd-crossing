package dev.pyrofetch.jn1.cum;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.entity.CompanionEntity;
import dev.pyrofetch.jn1.registry.ModBlocks;
import dev.pyrofetch.jn1.sound.GirlSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RodBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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
	public static final MapCodec<DildoBlock> CODEC = createCodec(DildoBlock::new);
	private static final String[] CRIS = {"moan", "ahh", "mmm"};
	/** Une poussée sur… */
	private static final int CHANCE_FIN = 20;

	/** Formes de contour selon l'axe : plus larges pour le cheval et Excalibur. */
	private final net.minecraft.util.shape.VoxelShape formeY, formeZ, formeX;

	public DildoBlock(Settings settings) {
		this(settings, 2.0);
	}

	/** {@code demiLargeur} : la moitié de l'épaisseur de la tige, en pixels. */
	public DildoBlock(Settings settings, double demiLargeur) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(FACING, Direction.UP));
		double a = 8 - demiLargeur, b = 8 + demiLargeur;
		this.formeY = Block.createCuboidShape(a, 0, a, b, 16, b);
		this.formeZ = Block.createCuboidShape(a, a, 0, b, b, 16);
		this.formeX = Block.createCuboidShape(0, a, a, 16, b, b);
	}

	@Override
	protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
	                                                              BlockPos pos, net.minecraft.block.ShapeContext context) {
		return switch (state.get(FACING).getAxis()) {
			case X -> this.formeX;
			case Z -> this.formeZ;
			case Y -> this.formeY;
		};
	}

	@Override
	protected MapCodec<? extends RodBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getSide());
	}

	/**
	 * Un end rod devant la face {@code face} d'un Cummy piston (ou de sa tête),
	 * pointé dans le même sens, devient un dildo. Appelé par le socle et la tête
	 * quand un voisin change.
	 */
	public static void convertir(World world, BlockPos devant, Direction face) {
		BlockState état = world.getBlockState(devant);
		if (world.isClient() || !état.isOf(Blocks.END_ROD) || état.get(FACING) != face) {
			return;
		}
		world.setBlockState(devant, ModBlocks.DILDO.getDefaultState().with(FACING, face), Block.NOTIFY_ALL);
		// Le succès revient à qui est là : on ne sait pas qui a posé l'end rod.
		net.minecraft.entity.player.PlayerEntity témoin = world.getClosestPlayer(
				devant.getX() + 0.5, devant.getY() + 0.5, devant.getZ() + 0.5, 8.0, false);
		if (témoin != null) {
			dev.pyrofetch.jn1.progress.Succes.accorder(témoin, "dildo");
		}
		world.playSound(null, devant, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8F, 1.3F);
		if (world instanceof ServerWorld monde) {
			monde.spawnParticles(net.minecraft.particle.ParticleTypes.HEART,
					devant.getX() + 0.5, devant.getY() + 0.5, devant.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
		}
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState ancien, boolean notify) {
		super.onBlockAdded(state, world, pos, ancien, notify);
		if (world.isClient() || !ancien.isOf(Blocks.MOVING_PISTON)) {
			return;
		}
		// Une poussée le laisse à deux blocs du socle (la tête entre eux) ; ramené,
		// il est contre lui. Le socle ne bouge pas quand il sort, alors que la tête
		// et le socle qui rentre peuvent être encore en mouvement à cet instant.
		if (world.getBlockState(pos.offset(state.get(FACING).getOpposite(), 2)).isOf(ModBlocks.CUMMY_PISTON)) {
			this.pousser(world, pos, state.get(FACING));
		}
	}

	/**
	 * Les dégâts d'Excalibur : rien ne s'y oppose — ni armure, ni totem, ni
	 * créatif, ni l'invulnérabilité de FapGPT (étiquettes de
	 * {@code data/minecraft/tags/damage_type}). Le message de mort parle
	 * d'une déchirure ({@code death.attack.jn1.excalibur}).
	 */
	public static final net.minecraft.registry.RegistryKey<net.minecraft.entity.damage.DamageType> DÉCHIRURE =
			net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,
					dev.pyrofetch.jn1.JN1.id("excalibur"));

	private void pousser(World world, BlockPos pos, Direction face) {
		Box devant = new Box(pos.offset(face)).stretch(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ())
				.expand(0.3);
		// Excalibur, poussé par un piston : tout ce qui vit devant lui meurt sur le coup.
		if (this == ModBlocks.EXCALIBUR) {
			var source = world.getDamageSources().create(DÉCHIRURE);
			for (net.minecraft.entity.LivingEntity victime : world.getEntitiesByClass(
					net.minecraft.entity.LivingEntity.class, devant, v -> v.isAlive() && !v.isSpectator())) {
				victime.damage(source, Float.MAX_VALUE);
				world.playSound(null, victime.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.BLOCKS, 1.2F, 0.6F);
				if (world instanceof ServerWorld monde) {
					monde.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, victime.getX(),
							victime.getBodyY(0.5), victime.getZ(), 20, 0.3, 0.4, 0.3, 0.4);
				}
			}
			return;
		}
		// Le mème : un mouton au bout du piston. Il bêle, rosit, et c'est un succès secret.
		for (net.minecraft.entity.passive.SheepEntity mouton : world.getEntitiesByClass(
				net.minecraft.entity.passive.SheepEntity.class, devant, m -> !m.isRemoved())) {
			world.playSound(null, mouton.getBlockPos(), SoundEvents.ENTITY_SHEEP_AMBIENT, SoundCategory.NEUTRAL, 1.0F, 1.6F);
			mouton.setColor(net.minecraft.util.DyeColor.PINK);
			if (world instanceof ServerWorld monde) {
				monde.spawnParticles(net.minecraft.particle.ParticleTypes.HEART,
						mouton.getX(), mouton.getY() + mouton.getHeight(), mouton.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			}
			for (net.minecraft.entity.player.PlayerEntity témoin : world.getPlayers()) {
				if (témoin.squaredDistanceTo(mouton) < 24 * 24) {
					dev.pyrofetch.jn1.progress.Succes.accorder(témoin, "mouton");
				}
			}
		}
		for (CompanionEntity fille : world.getEntitiesByClass(CompanionEntity.class, devant, f -> !f.isRemoved())) {
			fille.showHearts(2);
			SoundEvent bruit = GirlSounds.bruit("smallinserts");
			if (bruit != null) {
				world.playSound(null, fille.getBlockPos(), bruit, SoundCategory.NEUTRAL, 0.8F, 1.0F);
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
