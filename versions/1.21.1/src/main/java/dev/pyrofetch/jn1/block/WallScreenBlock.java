package dev.pyrofetch.jn1.block;

import com.mojang.serialization.MapCodec;
import dev.pyrofetch.jn1.network.OpenPanelPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

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
public class WallScreenBlock extends HorizontalFacingBlock implements BlockEntityProvider {
	public static final MapCodec<WallScreenBlock> CODEC = createCodec(WallScreenBlock::new);

	public static final BooleanProperty UP = Properties.UP;
	public static final BooleanProperty DOWN = Properties.DOWN;
	/** Gauche et droite vues de face, pas selon les points cardinaux. */
	public static final BooleanProperty LEFT = BooleanProperty.of("left");
	public static final BooleanProperty RIGHT = BooleanProperty.of("right");

	/** Un tableau plus grand ne se lirait plus : au-delà, on en commence un autre. */
	public static final int CÔTÉ_MAX = 12;
	/** Épaisseur de la dalle, en pixels. */
	private static final double ÉPAISSEUR = 2.0;

	public WallScreenBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(UP, false).with(DOWN, false).with(LEFT, false).with(RIGHT, false));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, UP, DOWN, LEFT, RIGHT);
	}

	/** La droite de celui qui regarde l'écran en face. */
	public static Direction droite(Direction face) {
		return face.rotateYCounterclockwise();
	}

	public static Direction gauche(Direction face) {
		return face.rotateYClockwise();
	}

	/** Contre le mur visé, comme le terminal ; au sol ou au plafond, face au joueur. */
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction côté = context.getSide();
		Direction face = côté.getAxis().isHorizontal() ? côté : context.getHorizontalPlayerFacing().getOpposite();
		return raccorder(this.getDefaultState().with(FACING, face), context.getWorld(), context.getBlockPos());
	}

	private static BlockState raccorder(BlockState état, BlockView monde, BlockPos pos) {
		Direction face = état.get(FACING);
		return état
				.with(UP, écran(monde, pos.up(), face))
				.with(DOWN, écran(monde, pos.down(), face))
				.with(LEFT, écran(monde, pos.offset(gauche(face)), face))
				.with(RIGHT, écran(monde, pos.offset(droite(face)), face));
	}

	/** Un écran tourné du même côté : c'est la seule chose qui s'y raccorde. */
	public static boolean écran(BlockView monde, BlockPos pos, Direction face) {
		BlockState état = monde.getBlockState(pos);
		return état.getBlock() instanceof WallScreenBlock && état.get(FACING) == face;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState état, Direction direction, BlockState voisin,
	                                               WorldAccess monde, BlockPos pos, BlockPos posVoisin) {
		if (direction.getAxis().isVertical() || direction.getAxis() != état.get(FACING).getAxis()) {
			return raccorder(état, monde, pos);
		}
		return état;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState état, BlockView monde, BlockPos pos, ShapeContext contexte) {
		double e = ÉPAISSEUR / 16.0;
		return switch (état.get(FACING)) {
			case NORTH -> VoxelShapes.cuboid(0, 0, 1 - e, 1, 1, 1);
			case SOUTH -> VoxelShapes.cuboid(0, 0, 0, 1, 1, e);
			case WEST -> VoxelShapes.cuboid(1 - e, 0, 0, 1, 1, 1);
			default -> VoxelShapes.cuboid(0, 0, 0, e, 1, 1);
		};
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState état) {
		return new WallScreenBlockEntity(pos, état);
	}

	@Override
	protected ActionResult onUse(BlockState état, World monde, BlockPos pos,
	                             PlayerEntity joueur, BlockHitResult visée) {
		if (monde.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (joueur instanceof ServerPlayerEntity serveur) {
			ServerPlayNetworking.send(serveur, new OpenPanelPayload(origine(monde, pos, état.get(FACING))));
		}
		return ActionResult.CONSUME;
	}

	/**
	 * L'écran qui tient le texte : on longe l'ensemble vers la gauche puis vers
	 * le haut, tant qu'il y a des écrans.
	 */
	public static BlockPos origine(BlockView monde, BlockPos pos, Direction face) {
		BlockPos courant = pos;
		for (int pas = 0; pas < CÔTÉ_MAX * 2; pas++) {
			BlockPos àGauche = courant.offset(gauche(face));
			if (écran(monde, àGauche, face)) {
				courant = àGauche;
				continue;
			}
			BlockPos auDessus = courant.up();
			if (écran(monde, auDessus, face)) {
				courant = auDessus;
				continue;
			}
			break;
		}
		return courant;
	}

	/** Vrai si cet écran tient le texte de son ensemble. */
	public static boolean estOrigine(BlockView monde, BlockPos pos, Direction face) {
		return !écran(monde, pos.offset(gauche(face)), face) && !écran(monde, pos.up(), face);
	}

	/**
	 * La surface de l'affichage, en écrans : sa largeur vers la droite, puis sa
	 * hauteur vers le bas, tant que chaque rangée est complète.
	 */
	public static int[] taille(BlockView monde, BlockPos origine, Direction face) {
		Direction droite = droite(face);
		int largeur = 1;
		while (largeur < CÔTÉ_MAX && écran(monde, origine.offset(droite, largeur), face)) {
			largeur++;
		}
		int hauteur = 1;
		rangées:
		while (hauteur < CÔTÉ_MAX) {
			BlockPos début = origine.down(hauteur);
			for (int x = 0; x < largeur; x++) {
				if (!écran(monde, début.offset(droite, x), face)) {
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
