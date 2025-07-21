package rafradek.TF2weapons.tileentity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.Level;
import rafradek.TF2weapons.TF2weapons;
import net.minecraft.core.BlockPos;

public class EntityOutputManager {
	public Level world;
	public Multimap<String, Tuple<BlockPos, Integer>> outputs = HashMultimap.create();
	public String name = "";
	public IEntityConfigurable entity;

	public EntityOutputManager(IEntityConfigurable entity) {
		this.entity = entity;
	}

	public void setWorld(Level world) {
		this.world = world;
	}

	public void loadOutputs(CompoundTag tag) {
		outputs.clear();
		for (String key : tag.getAllKeys()) {
			ListTag list = tag.getList(key, Tag.TAG_INT_ARRAY);
			for (int i = 0; i < list.size(); i++) {
				int[] arr = ((IntArrayTag) list.get(i)).getAsIntArray();
				outputs.put(key, new Tuple<>(new BlockPos(arr[0], arr[1], arr[2]), arr[3]));
			}
		}
	}

	public CompoundTag saveOutputs(CompoundTag tag) {
		for (String key : outputs.keySet()) {
			ListTag list = new ListTag();
			for (Tuple<BlockPos, Integer> pos : outputs.get(key)) {
				list.add(new IntArrayTag(new int[] { pos.getA().getX(), pos.getA().getY(),
						pos.getA().getZ(), pos.getB() }));
			}
			tag.put(key, list);
		}
		return tag;
	}

	public void readConfig(CompoundTag tag) {
		this.loadOutputs(tag.getCompound("Outputs"));
		this.name = tag.getString("Link Name");
		this.entity.readConfig(tag);
	}

	public CompoundTag writeConfig(CompoundTag tag) {
		tag.put("Outputs", this.saveOutputs(new CompoundTag()));
		tag.putString("Link Name", name);
		this.entity.writeConfig(tag);
		return tag;
	}

	public void activateOutput(String output, float power, int minTime) {
		TF2weapons.LOGGER.info("activated " + output);
		for (Tuple<BlockPos, Integer> tup : outputs.get(output)) {
			BlockPos pos = tup.getA();
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof BlockButton) {
				world.setBlockState(pos, state.withProperty(BlockButton.POWERED, Boolean.valueOf(true)), 3);
				world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
				world.notifyNeighborsOfStateChange(pos.offset(state.getValue(BlockDirectional.FACING).getOpposite()),
						state.getBlock(), false);
				world.scheduleUpdate(pos, state.getBlock(),
						(int) (state.getBlock().tickRate(world) * power * 15f / tup.getSecond()));
			} else if (state.getBlock() instanceof BlockLever) {
				state = state.withProperty(BlockLever.POWERED, tup.getSecond() != 0);
				EnumFacing enumfacing = state.getValue(BlockLever.FACING).getFacing();
				world.setBlockState(pos, state, 3);
				world.notifyNeighborsOfStateChange(pos.offset(enumfacing.getOpposite()), state.getBlock(), false);
				world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
			} else if (state.getBlock() instanceof BlockRedstoneComparator) {
				world.setBlockState(pos, state.withProperty(BlockButton.POWERED, Boolean.valueOf(true)), 3);
				if (world.getTileEntity(pos) != null)
					((TileEntityComparator) world.getTileEntity(pos)).setOutputSignal((int) (tup.getSecond() * power));

				world.scheduleUpdate(pos, state.getBlock(), minTime);
				world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
			} else if (state.getBlock() instanceof BlockRedstoneWire) {
				state = state.withProperty(BlockRedstoneWire.POWER, 15);
				world.setBlockState(pos, state, 3);
				world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
			}
		}
	}
}
