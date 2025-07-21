package rafradek.TF2weapons.tileentity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rafradek.TF2weapons.TF2weapons;

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
			if (state.getBlock() instanceof ButtonBlock) {
				world.setBlock(pos, state.setValue(ButtonBlock.POWERED, Boolean.valueOf(true)), 3);
				world.updateNeighborsAt(pos, state.getBlock());
				world.updateNeighborsAt(pos.relative(state.getValue(ButtonBlock.FACING).getOpposite()),state.getBlock());
				world.scheduleTick(
					pos, state.getBlock(), (int) (20 * power * 15f / tup.getB()) // TODO: implement state.getBlock().tickRate(world) normally, not just 20
					);

			} else if (state.getBlock() instanceof LeverBlock) {
				state = state.setValue(LeverBlock.POWERED, tup.getB() != 0);
				Direction enumfacing = state.getValue(LeverBlock.FACING);
				world.setBlock(pos, state, 3);
				world.updateNeighborsAt(pos.relative(enumfacing), state.getBlock());
				world.updateNeighborsAt(pos, state.getBlock());
			} else if (state.getBlock() instanceof ComparatorBlock) {
				world.setBlock(pos, state.setValue(ButtonBlock.POWERED, Boolean.valueOf(true)), 3);
				if (world.getBlockEntity(pos) != null)
					((ComparatorBlockEntity) world.getBlockEntity(pos)).setOutputSignal((int) (tup.getB() * power));

				world.scheduleTick(pos, state.getBlock(), minTime);
				world.updateNeighborsAt(pos, state.getBlock());
			} else if (state.getBlock() instanceof RedStoneWireBlock) {
				state = state.setValue(RedStoneWireBlock.POWER, 15);
				world.setBlock(pos, state, 3);
				world.updateNeighborsAt(pos, state.getBlock());
			}
		}
	}
}
