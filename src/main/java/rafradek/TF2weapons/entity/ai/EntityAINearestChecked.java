package rafradek.TF2weapons.entity.ai;

import com.google.common.base.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import rafradek.TF2weapons.TF2ConfigVars;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.entity.mercenary.EntityMedic;
import rafradek.TF2weapons.entity.mercenary.EntityTF2Character;
import rafradek.TF2weapons.item.ItemDisguiseKit;
import rafradek.TF2weapons.util.TF2Util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntityAINearestChecked<T extends LivingEntity> extends TargetGoal {
	public int targetChoosen = 0;
	private Class<T> targetClass;
	private Comparator<Entity> theNearestAttackableTargetSorter;
	private Predicate<T> targetEntitySelector;
	public LivingEntity targetEntity;
	private boolean targetLock;
	private int targetUnseenTicks;

	public EntityAINearestChecked(PathfinderMob p_i1665_1_, Class<T> p_i1665_2_, boolean p_i1665_4_,
			boolean p_i1665_5_, final Predicate<T> p_i1665_6_, boolean targetLock, boolean allowBehind) {
		super(p_i1665_1_, p_i1665_4_, p_i1665_5_);
		this.targetClass = p_i1665_2_;
		this.theNearestAttackableTargetSorter = new EntityAINearestAttackableTarget.Sorter(p_i1665_1_);
		this.setMutexBits(1);
		this.targetLock = targetLock;
		this.targetEntitySelector = target -> {
			if (p_i1665_6_ != null && !p_i1665_6_.apply(target))
				return false;
			else {
				// System.out.println("found "+target.getClass().getName()+"
				// "+EntityAINearestChecked.this.taskOwner.getClass().getName());
				if (target instanceof LivingEntity) {
					double d0 = EntityAINearestChecked.this.mob.distanceTo(this.mob.getTarget());

					if (target.isShiftKeyDown())
						d0 *= 0.800000011920929D;

					if (target instanceof Player && target.isInvisible()) {
						float f = ((Player) target).getArmorCoverPercentage();

						if (f < 0.1F)
							f = 0.1F;

						d0 *= 0.7F * f;
					}
					if (target.hasCapability(TF2weapons.WEAPONS_CAP, null)
							&& (target.getCapability(TF2weapons.WEAPONS_CAP, null).invisTicks >= 20
									|| ItemDisguiseKit.isDisguised(target, this.mob)))
						d0 = 1;
					boolean fastCheck = allowBehind || (!(target instanceof Player)
							&& (TF2ConfigVars.naturalCheck.equals("Fast") && this.mob instanceof EntityTF2Character
									&& ((EntityTF2Character) this.mob).natural));
					if (target.getDistance(this.mob) > d0 || (!fastCheck && !TF2Util.lookingAtFast(this.mob, 86,
							target.posX, target.posY + target.getEyeHeight(), target.posZ)))
						return false;

				}

				return EntityAINearestChecked.this.isSuitableTarget(target, false);
			}
		};
	}

	@Override
	public boolean canUse() {
		double d0 = this.mob.distanceTo(this.mob.getTarget()) / 2;
		if (((this.mob.getTarget() == null)
				|| this.mob.getTarget().distanceTo(mob) > d0 * d0))
			this.targetChoosen++;
		if (((this.mob.getTarget() == null) && this.targetChoosen > 1) || this.targetChoosen > 5
				|| !this.targetLock) {
			// System.out.println("executing
			// "+this.taskOwner.getClass().getName());
			this.targetChoosen = 0;
			double d1 = this.mob.distanceTo(this.mob.getTarget());
			List<? extends LivingEntity> list = this.mob.level().getEntitiesWithinAABB(this.targetClass,
					this.mob.getBoundingBox().grow(d1, d0, d1), this.targetEntitySelector);
			Collections.sort(list, this.theNearestAttackableTargetSorter);

			if (list.isEmpty())
				// System.out.println("emptylist
				// "+this.taskOwner.getClass().getName());
				return false;
			else {
				this.targetEntity = list.get(0);
				return true;
			}
		}
		return false;
	}

	@Override
	public void start() {
		this.taskOwner.setAttackTarget(this.targetEntity);
		if (this.taskOwner instanceof EntityTF2Character && this.taskOwner.getAttackTarget() != null) {
			EntityTF2Character shooter = ((EntityTF2Character) this.taskOwner);
			shooter.targetPrevPos[0] = shooter.getAttackTarget().posX;
			shooter.targetPrevPos[2] = shooter.getAttackTarget().posY;
			shooter.targetPrevPos[4] = shooter.getAttackTarget().posZ;
		}
		super.start();
	}

	/**
	 * A method used to see if an entity is a suitable target through a number of
	 * checks. Args : entity, canTargetInvinciblePlayer
	 */
	@Override
	public boolean canContinueToUse() {
		LivingEntity LivingEntity = this.taskOwner.getAttackTarget();

		if (LivingEntity == null)
			return false;
		else if (!LivingEntity.isEntityAlive())
			return false;
		else if (!this.targetLock && this.taskOwner.ticksExisted % 13 == 0)
			return this.shouldExecute();
		else {
			Team team = this.taskOwner.getTeam();
			Team team1 = LivingEntity.getTeam();

			if ((team != null && team1 == team) && !(this.taskOwner instanceof EntityMedic))
				return false;
			else {
				double d0 = this.getTargetDistance();

				if (this.taskOwner.getDistanceSq(LivingEntity) > d0 * d0)
					return false;
				else {
					if (this.shouldCheckSight)
						if (this.taskOwner.getEntitySenses().canSee(LivingEntity))
							this.targetUnseenTicks = 0;
						else if (++this.targetUnseenTicks > 60)
							return false;

					return !(LivingEntity instanceof Player)
							|| !((Player) LivingEntity).capabilities.disableDamage;
				}
			}
		}
	}

	@Override
	protected boolean isSuitableTarget(LivingEntity target, boolean includeInvincibles) {
		if (target == null)
			return false;
		else if (target == this.taskOwner)
			return false;
		else if (!target.isEntityAlive())
			return false;
		else if (!this.taskOwner.canAttackClass(target.getClass()))
			return false;
		else {
			Team team = this.taskOwner.getTeam();
			Team team1 = target.getTeam();
			boolean medic = (this.taskOwner instanceof EntityMedic);
			if ((team != null && team1 == team) && !medic)
				return false;
			else {
				if (!medic && this.taskOwner instanceof OwnableEntity
						&& ((OwnableEntity) this.taskOwner).getOwnerId() != null) {
					if (target instanceof OwnableEntity && ((OwnableEntity) this.taskOwner).getOwnerId()
							.equals(((OwnableEntity) target).getOwnerId()))
						return false;

					if (target == ((OwnableEntity) this.taskOwner).getOwner())
						return false;
				}
				if (target instanceof Player && !includeInvincibles
						&& ((Player) target).capabilities.disableDamage)
					return false;

				return !this.shouldCheckSight || this.taskOwner.getEntitySenses().canSee(target);
			}
		}
	}
}
