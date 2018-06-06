package cd4017be.indlog.tileentity;

import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author CD4017BE
 *
 */
public class OverflowTrash extends BaseTileEntity implements IItemHandler, IFluidHandler {

	private static final IFluidTankProperties[] TANK_PROPS = {new FluidTankProperties(null, Integer.MAX_VALUE, true, false)};
	/**to prevent infinite recursive calls to itself when placed in a loop */
	private static boolean RECURSION = false; 

	private IItemHandler targetItem;
	private IFluidHandler targetFluid;

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
				&& facing != getOrientation().front && (!RECURSION || world.isRemote);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (world.isRemote) return hasCapability(cap, facing) ? (T) this : null;
		EnumFacing dir = getOrientation().front;
		if (dir == facing || RECURSION) return null;
		try {RECURSION = true;
			if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
				targetItem = Utils.neighborCapability(this, dir, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
			else if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
				targetFluid = Utils.neighborCapability(this, dir, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
			else return null;
			return (T) this;
		} finally {RECURSION = false;}
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (simulate || targetItem == null || RECURSION) return ItemStack.EMPTY;
		try {RECURSION = true; ItemHandlerHelper.insertItem(targetItem, stack, simulate);} finally {RECURSION = false;}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return TANK_PROPS;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (doFill && targetFluid != null && !RECURSION)
			try {RECURSION = true; targetFluid.fill(resource, doFill); } finally {RECURSION = false;}
		return resource.amount;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}

}
