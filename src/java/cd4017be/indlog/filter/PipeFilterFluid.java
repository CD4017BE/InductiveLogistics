package cd4017be.indlog.filter;

import static cd4017be.lib.util.ItemFluidUtil.listTanks;
import cd4017be.api.indlog.filter.FilterBase;
import cd4017be.indlog.Objects;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.item.Item;
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
public class PipeFilterFluid extends FilterBase<FluidStack, IFluidHandler> {

	public int maxAmount;
	public Fluid[] list = new Fluid[0];
	//mode 1=invert; 2=force; 4=invertRS; 8=redstone

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
		for (IFluidTankProperties inf : listTanks(inv))
			if ((fluid = inf.getContents()) != null && fluid.isFluidEqual(stack)) am -= fluid.amount;
		if (am <= 0) return 0;
		else if (am >= stack.amount) return stack.amount;
		else return am;
	}

	@Override
	public FluidStack getExtract(FluidStack stack, IFluidHandler inv) {
		if (stack == null) {
			for (IFluidTankProperties inf : listTanks(inv))
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

	public boolean matches(FluidStack stack) {
		Fluid f = stack.getFluid();
		for (Fluid fluid : list)
			if (f == fluid)
				return (mode & 1) == 0;
		return (mode & 1) != 0;
	}

	@Override
	public boolean noEffect() {
		return list.length == 0 && (mode & 1) != 0 && maxAmount == 0;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("maxAm", maxAmount);
		if (list.length > 0) {
			NBTTagList list = new NBTTagList();
			for (Fluid fluid : this.list)
				list.appendTag(new NBTTagString(fluid.getName()));
			nbt.setTag(ItemFluidUtil.Tag_FluidList, list);
		}
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		maxAmount = nbt.getInteger("maxAm");
		if (maxAmount < 0) maxAmount = 0;
		if (nbt.hasKey(ItemFluidUtil.Tag_FluidList)) {
			NBTTagList list = nbt.getTagList(ItemFluidUtil.Tag_FluidList, 8);
			this.list = new Fluid[list.tagCount()];
			for (int i = 0; i < list.tagCount(); i++)
				this.list[i] = FluidRegistry.getFluid(list.getStringTagAt(i));
		}
		super.deserializeNBT(nbt);
	}

	@Override
	public Item item() {
		return Objects.fluid_filter;
	}

}