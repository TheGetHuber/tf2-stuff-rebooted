package rafradek.TF2weapons.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.NonNullList;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

public class BlockProp extends Block {

	public static final EnumProperty<EnumBlockType> TYPE = EnumProperty.<EnumBlockType>create("type",
			EnumBlockType.class);

	// Material --> BlockState
	public BlockProp(Properties blockStateIn) {
		super(blockStateIn);
	}

	@OnlyIn(Dist.CLIENT)
	public void getSubBlocks(Item itemIn, CreativeModeTabs tab, NonNullList<ItemStack> list) {

	}

	// @Override
	// public BlockState getStateFromMeta(int meta) {
	// 	return this.getDefaultState().withProperty(TYPE, EnumBlockType.values()[meta]);
	// }

	// @Override
	// public int getMetaFromState(IBlockState state) {
	// 	return (state.getValue(TYPE)).ordinal();
	// }

	//@Override
	//protected BlockStateContainer createBlockState() {
	//	return new BlockStateContainer(this, new Property[] { TYPE });
	//}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE);
	}

	public enum EnumBlockType implements StringRepresentable {
		DIAMOND("diamond"), IRON("iron"), GOLD("gold"), OBSIDIAN("obsidian");

		private final String name;

		private EnumBlockType(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

	}
}
