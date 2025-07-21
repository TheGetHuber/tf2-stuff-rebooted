package rafradek.TF2weapons.tileentity;

import net.minecraft.nbt.CompoundTag;

public interface IEntityConfigurable {

	public CompoundTag writeConfig(CompoundTag tag);

	public void readConfig(CompoundTag tag);

	public EntityOutputManager getOutputManager();

	default public void activateOutput(String output) {
		this.activateOutput(output, 1f, 1);
	}

	default public void activateOutput(String output, float power, int minTime) {
		this.getOutputManager().activateOutput(output, power, minTime);
	};

	default public String getLinkName() {
		return this.getOutputManager().name;
	}

	public String[] getOutputs();

	default public String[] getAllowedValues(String attribute) {
		return null;
	}
}
