package cd4017be.indlog.util;

import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 *
 * @author CD4017BE
 */
public class PipeFilterFluid implements PipeFilter<FluidStack, IFluidHandler> {

	public int maxAmount;
	public Fluid[] list = new Fluid[0];
	/** 1=invert; 2=force; 4=invertRS; 8=redstone */
	public byte mode;
	public byte priority;

	@Override
	public boolean active(boolean rs) {
		return (mode & 8) == 0 || (rs ^ (mode & 4) != 0);
	}

	@Override
	public int insertAmount(FluidStack stack, IFluidHandler inv) {
		if (stack == null) return 0;
		if (!matches(stack)) return 0;
		if (maxAmount == 0) return stack.amount;
		int am = maxAmount;
		FluidStack fluid;
		for (IFluidTankProperties inf : inv.getTankProperties())
			if ((fluid = inf.getContents()) != null && fluid.isFluidEqual(stack)) am -= fluid.amount;
		if (am <= 0) return 0;
		else if (am >= stack.amount) return stack.amount;
		else return am;
	}

	@Override
	public FluidStack getExtract(FluidStack stack, IFluidHandler inv) {
		if (stack == null) {
			for (IFluidTankProperties inf : inv.getTankProperties())
				if((stack = inf.getContents()) != null && inf.canDrainFluidType(stack) && (stack = getExtract(stack, inv)) != null) return stack;
			return null;
		}
		if (!matches(stack)) return null;
		stack = inv.drain(stack, false);
		if (maxAmount > 0 && stack != null) stack.amount -= maxAmount; 
		return stack;
	}

	@Override
	public boolean transfer(FluidStack stack) {
		return (mode & 2) == 0 || (stack != null && !matches(stack));
	}

	@Override
	public boolean blocking() {
		return (mode & 2) != 0;
	}

	public boolean matches(FluidStack stack) {
		Fluid f = stack.getFluid();
		for (Fluid fluid : list)
			if (f == fluid)
				return (mode & 1) == 0;
		return (mode & 1) != 0;
	}

	public static PipeFilterFluid load(NBTTagCompound nbt) {
		PipeFilterFluid upgrade = new PipeFilterFluid();
		upgrade.mode = nbt.getByte("mode");
		upgrade.maxAmount = nbt.getInteger("maxAm");
		upgrade.priority = nbt.getByte("prior");
		if (upgrade.maxAmount < 0) upgrade.maxAmount = 0;
		if (nbt.hasKey(ItemFluidUtil.Tag_FluidList)) {
			NBTTagList list = nbt.getTagList(ItemFluidUtil.Tag_FluidList, 8);
			upgrade.list = new Fluid[list.tagCount()];
			for (int i = 0; i < list.tagCount(); i++)
				upgrade.list[i] = FluidRegistry.getFluid(list.getStringTagAt(i));
		}
		return upgrade;
	}

	public void save(NBTTagCompound nbt) {
		nbt.setByte("mode", mode);
		nbt.setInteger("maxAm", maxAmount);
		nbt.setByte("prior", priority);
		if (list.length > 0) {
			NBTTagList list = new NBTTagList();
			for (Fluid fluid : this.list)
				list.appendTag(new NBTTagString(fluid.getName()));
			nbt.setTag(ItemFluidUtil.Tag_FluidList, list);
		}
	}

	public static NBTTagCompound save(PipeFilterFluid upgrade) {
		NBTTagCompound nbt = new NBTTagCompound();
		upgrade.save(nbt);
		return nbt;
	}

	public static boolean isNullEq(PipeFilterFluid filter) {
		return filter == null || (filter.list.length == 0 && (filter.mode & 1) != 0 && filter.maxAmount == 0);
	}

}