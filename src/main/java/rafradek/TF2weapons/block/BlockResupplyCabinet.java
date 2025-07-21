package rafradek.TF2weapons.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import rafradek.TF2weapons.tileentity.TileEntityResupplyCabinet;
import rafradek.TF2weapons.tileentity.TileEntityRobotDeploy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.Direction;

public class BlockResupplyCabinet extends Block {

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

	@Override
	public RenderShape getRenderShape(BlockState state){
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity createNewTileEntity(World world, int meta) {
		return  ((meta & 4) == 4) ? new TileEntityResupplyCabinet() : null;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {}
		return false;
	}

	@Override
	//public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
	//	return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	//}
	
	public BlockState getStateForPlacement(BlockPlaceContext context){
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HOLDER, true);
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		this.updateState(world, pos, state);
	}

	private void updateState(World world, BlockPos pos, IBlockState state) {
		BlockEntity ent = world.getTileEntity(pos);
		if (ent instanceof TileEntityResupplyCabinet && ((TileEntityResupplyCabinet) ent).redstoneActivate)
			((TileEntityResupplyCabinet) ent).setEnabled(world.isBlockPowered(pos));
	}

	@Override
	public void onBlockDestroyedByPlayer(World world, BlockPos pos, IBlockState state) {

	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		this.updateState(world, fromPos, state);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		world.setBlockState(pos, state = state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
		if (placer instanceof EntityPlayer) {}
		if (world.isAirBlock(pos.up()))
			world.setBlockState(pos.up(), state.withProperty(HOLDER, false), 2);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
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
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return super.canPlaceBlockAt(world, pos)
				&& world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.getFront((meta & 3) + 2)).withProperty(HOLDER, (meta & 4) == 4);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
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
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos) {
		return  world.getTileEntity(pos) instanceof TileEntityResupplyCabinet
				&& ((TileEntityResupplyCabinet) world.getTileEntity(pos)).cooldownUse.size() > 0 ? 15 : 0;
	}
}
