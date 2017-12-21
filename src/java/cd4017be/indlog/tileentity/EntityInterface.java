package cd4017be.indlog.tileentity;

import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

/**
 * 
 * @author cd4017be
 */
public class EntityInterface extends BaseTileEntity {

	public static int INTERVAL;

	private Entity entity;
	private long lastCheck;

	private void getEntity(Capability<?> cap, EnumFacing side) {
		long t = world.getTotalWorldTime();
		if (t - lastCheck >= INTERVAL) {
			lastCheck = t;
			entity = null;
			BlockPos pos = this.pos.offset(getOrientation().front);
			for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos, pos.add(1, 1, 1)))) {
				if (entity == null || e.hasCapability(cap, side))
					entity = e;
			}
		} else if (entity != null && entity.isDead) entity = null;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing side) {
		getEntity(cap, side);
		return entity != null && entity.hasCapability(cap, side)
				|| cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
				|| cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing side) {
		getEntity(cap, side);
		if (entity != null) {
			T obj = entity.getCapability(cap, side);
			if (obj != null) return obj;
		}
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) EmptyHandler.INSTANCE;
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T) EmptyFluidHandler.INSTANCE;
		return null;
	}

}
