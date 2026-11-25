package dev.pyrofetch.jn1.block;

import dev.pyrofetch.jn1.network.OpenPanelPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * L'écran mural : une dalle d'affichage qu'on accole à d'autres pour en faire
 * un grand tableau.
 *
 * <p>Des écrans voisins, tournés du même côté, forment un seul affichage. Le
 * texte est tenu par celui du coin supérieur gauche — vu de face —, qui le
 * dessine sur toute la surface ; les autres ne portent que leur dalle. Le cadre
 * ne court que sur le pourtour : chaque écran sait par son état de bloc de quel
 * côté il a un voisin, et le modèle omet la baguette de ce côté-là.
 *
 * <p>On l'écrit comme une note, avec le même éditeur ; un clic sur n'importe
 * lequel des écrans ouvre le texte de l'ensemble.
 */
public class WallScreenBlock extends HorizontalDirectionalBlock implements EntityBlock {

	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	/** Gauche et droite vues de face, pas selon les points cardinaux. */
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	/** Un tableau plus grand ne se lirait plus : au-delà, on en commence un autre. */
	public static final int CÔTÉ_MAX = 12;
	/** Épaisseur de la dalle, en pixels. */
	private static final double ÉPAISSEUR = 2.0;

	public WallScreenBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FACING, Direction.NORTH)
				.setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, UP, DOWN, LEFT, RIGHT);
	}

	/** La droite de celui qui regarde l'écran en face. */
	public static Direction droite(Direction face) {
		return face.getCounterClockWise();
	}

	public static Direction gauche(Direction face) {
		return face.getClockWise();
	}

	/** Contre le mur visé, comme le terminal ; au sol ou au plafond, face au joueur. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction côté = context.getClickedFace();
		Direction face = côté.getAxis().isHorizontal() ? côté : context.getHorizontalDirection().getOpposite();
		return raccorder(this.defaultBlockState().setValue(FACING, face), context.getLevel(), context.getClickedPos());
	}

	private static BlockState raccorder(BlockState état, BlockGetter monde, BlockPos pos) {
		Direction face = état.getValue(FACING);
		return état
				.setValue(UP, écran(monde, pos.above(), face))
				.setValue(DOWN, écran(monde, pos.below(), face))
				.setValue(LEFT, écran(monde, pos.relative(gauche(face)), face))
				.setValue(RIGHT, écran(monde, pos.relative(droite(face)), face));
	}

	/** Un écran tourné du même côté : c'est la seule chose qui s'y raccorde. */
	public static boolean écran(BlockGetter monde, BlockPos pos, Direction face) {
		BlockState état = monde.getBlockState(pos);
		return état.getBlock() instanceof WallScreenBlock && état.getValue(FACING) == face;
	}

	@Override
	protected BlockState updateShape(BlockState état, LevelReader monde, ScheduledTickAccess ticks, BlockPos pos,
	                                 Direction direction, BlockPos posVoisin, BlockState voisin, RandomSource random) {
		if (direction.getAxis().isVertical() || direction.getAxis() != état.getValue(FACING).getAxis()) {
			return raccorder(état, monde, pos);
		}
		return état;
	}

	@Override
	protected VoxelShape getShape(BlockState état, BlockGetter monde, BlockPos pos, CollisionContext contexte) {
		double e = ÉPAISSEUR / 16.0;
		return switch (état.getValue(FACING)) {
			case NORTH -> Shapes.box(0, 0, 1 - e, 1, 1, 1);
			case SOUTH -> Shapes.box(0, 0, 0, 1, 1, e);
			case WEST -> Shapes.box(1 - e, 0, 0, 1, 1, 1);
			default -> Shapes.box(0, 0, 0, e, 1, 1);
		};
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState état) {
		return new WallScreenBlockEntity(pos, état);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState état, Level monde, BlockPos pos,
	                             Player joueur, BlockHitResult visée) {
		if (monde.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (joueur instanceof ServerPlayer serveur) {
			ServerPlayNetworking.send(serveur, new OpenPanelPayload(origine(monde, pos, état.getValue(FACING))));
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * L'écran qui tient le texte : on longe l'ensemble vers la gauche puis vers
	 * le haut, tant qu'il y a des écrans.
	 */
	public static BlockPos origine(BlockGetter monde, BlockPos pos, Direction face) {
		BlockPos courant = pos;
		for (int pas = 0; pas < CÔTÉ_MAX * 2; pas++) {
			BlockPos àGauche = courant.relative(gauche(face));
			if (écran(monde, àGauche, face)) {
				courant = àGauche;
				continue;
			}
			BlockPos auDessus = courant.above();
			if (écran(monde, auDessus, face)) {
				courant = auDessus;
				continue;
			}
			break;
		}
		return courant;
	}

	/** Vrai si cet écran tient le texte de son ensemble. */
	public static boolean estOrigine(BlockGetter monde, BlockPos pos, Direction face) {
		return !écran(monde, pos.relative(gauche(face)), face) && !écran(monde, pos.above(), face);
	}

	/**
	 * La surface de l'affichage, en écrans : sa largeur vers la droite, puis sa
	 * hauteur vers le bas, tant que chaque rangée est complète.
	 */
	public static int[] taille(BlockGetter monde, BlockPos origine, Direction face) {
		Direction droite = droite(face);
		int largeur = 1;
		while (largeur < CÔTÉ_MAX && écran(monde, origine.relative(droite, largeur), face)) {
			largeur++;
		}
		int hauteur = 1;
		rangées:
		while (hauteur < CÔTÉ_MAX) {
			BlockPos début = origine.below(hauteur);
			for (int x = 0; x < largeur; x++) {
				if (!écran(monde, début.relative(droite, x), face)) {
					break rangées;
				}
			}
			hauteur++;
		}
		return new int[] {largeur, hauteur};
	}

	/** La marge, en blocs, entre le bord de l'ensemble et le texte (celle du rendu). */
	public static final float MARGE = 0.12F;
	/**
	 * Le grain le plus fin du texte : 96 points de police par bloc, soit une
	 * ligne d'un dixième de bloc. Plus serré, il ne se lirait plus.
	 */
	public static final float DENSITÉ = 96.0F;
	/** Au plus : la largeur que l'éditeur peut montrer, et un nombre de lignes raisonnable. */
	public static final int LARGEUR_TEXTE_MAX = 480;
	public static final int LIGNES_TEXTE_MAX = dev.pyrofetch.jn1.network.SaveNotePayload.LIGNES_MAX;

	/**
	 * Ce qu'un ensemble d'écrans peut afficher (0.83.1) : la largeur d'une ligne en points de
	 * police, et le nombre de lignes. Un 1 × 1 : 73 points (une douzaine de
	 * caractères) et 8 lignes ; un 3 × 2 : 265 points et 18 lignes.
	 */
	public static int[] capacité(int[] taille) {
		int largeur = (int) ((taille[0] - 2 * MARGE) * DENSITÉ);
		int lignes = (int) ((taille[1] - 2 * MARGE) * DENSITÉ / 9.0F);
		return new int[] {Math.min(largeur, LARGEUR_TEXTE_MAX), Math.min(lignes, LIGNES_TEXTE_MAX)};
	}
}
