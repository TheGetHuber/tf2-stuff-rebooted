package rafradek.TF2weapons.block;

import rafradek.TF2weapons.tileentity.TileEntityResupplyCabinet;
import rafradek.TF2weapons.tileentity.TileEntityRobotDeploy;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockResupplyCabinet extends Block implements EntityBlock{

	public static final BooleanProperty HOLDER = BooleanProperty.create("holder");
	public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
	
	public BlockResupplyCabinet() {
		super(Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.5f, 6.0f)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
		);
		
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HOLDER, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HOLDER);
	}

	// already implemented in BlockBehaviour.class
	// @Override
	// public RenderShape getRenderShape(BlockState state){
	// 	return RenderShape.MODEL;
	// }

	@Override
	public BlockEntity createNewBlockEntity(World world, int meta) {
		return  ((meta & 4) == 4) ? new TileEntityResupplyCabinet() : null;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player ply, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide) {}
		return InteractionResult.PASS;
	}

	@Override
	//public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
	//	return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	//}
	
	public BlockState getStateForPlacement(BlockPlaceContext context){
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HOLDER, true);
	}

	@Override
	public void onBlockAdded(Level world, BlockPos pos, BlockState state) {
		this.updateState(world, pos, state);
	}

	private void updateState(Level world, BlockPos pos, BlockState state) {
		BlockEntity ent = world.getBlockEntity(pos);
		if (ent instanceof TileEntityResupplyCabinet && ((TileEntityResupplyCabinet) ent).redstoneActivate)
			((TileEntityResupplyCabinet) ent).setEnabled(world.isBlockPowered(pos));
	}

	@Override
	public void onBlockDestroyedByPlayer(Level world, BlockPos pos, BlockState state) {

	}

	@Override
	public void neighborChanged(IBlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		this.updateState(world, fromPos, state);
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		world.setBlockState(pos, state = state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
		if (placer instanceof EntityPlayer) {}
		if (world.isAirBlock(pos.up()))
			world.setBlockState(pos.up(), state.withProperty(HOLDER, false), 2);
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, IBlockState state) {
		if (state.getValue(HOLDER)) {
			if (world.getBlockState(pos.up()).getBlock() == this) world.setBlockToAir(pos.up());
		} else {
			if (world.getBlockState(pos.down()).getBlock() == this) world.setBlockToAir(pos.down());
			if (world.getBlockState(pos.up()).getBlock() == this && !world.getBlockState(pos.up()).getValue(HOLDER)) world.setBlockToAir(pos.up());
		}
		BlockEntity ent = world.getTileEntity(pos);
		if (ent instanceof TileEntityRobotDeploy) ((TileEntityRobotDeploy) ent).dropInventory();
	}

	@Override
	public boolean canPlaceBlockAt(Level world, BlockPos pos) {
		return super.canPlaceBlockAt(world, pos)
				&& world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
	}

	@Override
	public IBlockState withRotation(BlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(BlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.getFront((meta & 3) + 2)).withProperty(HOLDER, (meta & 4) == 4);
	}

	@Override
	public int getMetaFromState(BlockState state) {
		return state.getValue(FACING).getIndex() - 2 + (state.getValue(HOLDER) ? 4 : 0);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { FACING, HOLDER });
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
		list.add(new ItemStack(this, 1, 4));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public boolean isOpaqueCube(BlockState state) {
		return false;
	}

	@Override
	public boolean isCollisionShapeFullBlock(BlockGetter getter, BlockPos pos){
		return false;
	}

	@Override
	public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos) {
		return  world.getTileEntity(pos) instanceof TileEntityResupplyCabinet
				&& ((TileEntityResupplyCabinet) world.getTileEntity(pos)).cooldownUse.size() > 0 ? 15 : 0;
	}
}
