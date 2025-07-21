package rafradek.TF2weapons.tileentity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.scores.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.entity.mercenary.EntityTF2Character;
import rafradek.TF2weapons.item.ItemAmmoPackage;
import rafradek.TF2weapons.item.ItemFromData;
import rafradek.TF2weapons.item.ItemWeapon;
import rafradek.TF2weapons.util.PropertyType;
import rafradek.TF2weapons.util.TF2Util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TileEntityResupplyCabinet extends BlockEntity implements TickingBlockEntity, IEntityConfigurable {

	private static final String[] OUTPUT_NAMES = { "OnResupply", "OnResupplyLeave" };
	public Team team;
	public Map<LivingEntity, Integer> cooldownUse = new HashMap<>();
	public boolean usedBy;
	public boolean enabled = true;
	public boolean redstoneActivate;
	private EntityOutputManager outputManager = new EntityOutputManager(this);

	public void setEnabled(boolean enable) {

		this.level.setBlock(this.worldPosition, this.getBlockState(), 0, enabled ? 1 : 0);
		if (this.enabled != enable) {
			this.enabled = enable;
			this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
		}

	}

	@Override
	public void tick() {
		if (!this.level.isClientSide) {
			int playersold = cooldownUse.size();
			cooldownUse.entrySet().removeIf(entry -> {
				entry.setValue(entry.getValue() - 1);
				return entry.getValue() <= 0;
			});

			if (this.enabled)
				for (EntityLivingBase living : this.level.getEntitiesWithinAABB(EntityLivingBase.class,
						new AxisAlignedBB(this.worldPosition).grow(2),
						entityf -> (entityf.isEntityAlive() && (team == null || entityf.getTeam() == team)
								&& !cooldownUse.containsKey(entityf)
								&& entityf.hasCapability(TF2weapons.WEAPONS_CAP, null)))) {
					living.setHealth(living.getMaxHealth());
					ArrayList<Potion> badEffects = new ArrayList<>();
					for (Entry<Potion, PotionEffect> entry : living.getActivePotionMap().entrySet()) {
						if (entry.getKey().isBadEffect())
							badEffects.add(entry.getKey());
					}
					for (Potion potion : badEffects) {
						living.removePotionEffect(potion);
					}

					if (living instanceof EntityTF2Character) {
						((EntityTF2Character) living).restoreAmmo(1);
					} else if (living instanceof Player) {
						Player player = ((Player) living);
						player.getFoodData().eat(20, 20f);
						for (int i = 0; i < player.getInventory().INVENTORY_SIZE; i++) {
							ItemStack stack = player.getInventory().getItem(i);
							if (stack.getItem() instanceof ItemFromData) {
								int ammotype = ((ItemFromData) stack.getItem()).getAmmoType(stack);
								int ammocount = ItemFromData.getAmmoAmountType(player, ammotype);
								if (ammocount < ItemFromData.getData(stack).getInt(PropertyType.MAX_AMMO)) {
									TF2Util.pickAmmo(ItemAmmoPackage.getAmmoForType(ammotype,
											ItemFromData.getData(stack).getInt(PropertyType.MAX_AMMO) - ammocount),
											player, true);
								}
							}
							if (stack.getItem() instanceof ItemWeapon) {
								((ItemWeapon) stack.getItem()).setClip(stack,
										((ItemWeapon) stack.getItem()).getWeaponClipSize(stack, living));
							}
						}
					}
					this.activateOutput("OnResupply");
					cooldownUse.put(living, 50);
					this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
				}
			if (playersold > 0 && cooldownUse.size() == 0) {
				this.activateOutput("OnResupplyLeave");
				this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
			}
		}
	}

	@Override
	public CompoundTag saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		compound.put("Config", this.getOutputManager().writeConfig(new CompoundTag()));
		compound.putBoolean("Enabled", this.enabled);
		return compound;
	}

	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		this.getOutputManager().readConfig(compound.getCompound("Config"));
		this.enabled = compound.getBoolean("Enabled");
	}

	@Override
	public boolean receiveClientEvent(int id, int type) {
		if (id == 1) {
			this.enabled = type != 0;
			return true;
		} else {
			return super.receiveClientEvent(id, type);
		}
	}

	/*
	 * @Nullable public SPacketUpdateTileEntity getUpdatePacket() { NBTTagCompound
	 * tag = new NBTTagCompound(); if (this.maxprogress > 0) { tag.setByte("P",
	 * (byte) ((float)this.progress/(float)this.maxprogress*7f)); if (this.progress
	 * > 0) tag.setByte("C", (byte)
	 * ItemToken.getClassID(TF2Util.getWeaponUsedByClass(this.weapon.extractItem(
	 * hasWeapon,64,true)))); } return new SPacketUpdateTileEntity(this.worldPosition, 9999,
	 * tag); }
	 * 
	 * public void onDataPacket(net.minecraft.network.NetworkManager net,
	 * net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
	 * this.progressClient = pkt.getNbtCompound().getByte("P"); this.classType =
	 * pkt.getNbtCompound().getByte("C"); }
	 */

	@Override
	public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability,
			@Nullable net.minecraft.util.EnumFacing facing) {
		if (facing != null && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	@Nullable
	public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
			@Nullable net.minecraft.util.EnumFacing facing) {
		/*
		 * if (facing != null && capability ==
		 * net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) { if
		 * (facing.getAxis() == Axis.Y) return
		 * CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.weapon); else if
		 * (facing.getAxis() == Axis.X) return
		 * CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.parts); else return
		 * CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.money); } else
		 */
		return super.getCapability(capability, facing);
	}

	@Override
	public void onLoad() {}

	@Override
	public boolean shouldRefresh(Level world, BlockPos pos, BlockState oldState, BlockState newSate) {
		return super.shouldRefresh(world, pos, oldState, newSate);
	}

	@Override
	protected void setWorldCreate(Level world) {
		this.setWorld(world);
	}

	@Override
	public void setWorld(Level world) {
		super.setWorld(world);
		this.getOutputManager().world = world;
	}

	@Override
	public CompoundTag writeConfig(CompoundTag tag) {
		tag.setTag("Outputs", this.getOutputManager().saveOutputs(new NBTTagCompound()));
		if (team != null)
			tag.setString("T:Team", this.team.getName());
		else
			tag.setString("T:Team", "");
		tag.setBoolean("Redstone Activates", this.redstoneActivate);
		return tag;
	}

	@Override
	public void readConfig(CompoundTag tag) {
		this.getOutputManager().loadOutputs(tag.get("Outputs"));
		this.redstoneActivate = tag.getBoolean("Redstone Activates");
		if (this.hasWorld() && tag.hasKey("T:Team"))
			this.team = this.level.getScoreboard().getTeam(tag.getString("T:Team"));
	}

	@Override
	public EntityOutputManager getOutputManager() {
		return this.outputManager;
	}

	@Override
	public String[] getOutputs() {
		return OUTPUT_NAMES;
	}

}
