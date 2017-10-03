package cd4017be.indlog.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public class FluidHandlerDirectNBT extends FluidHandlerItemStack {

	public FluidHandlerDirectNBT(ItemStack container, int capacity) {
		super(container, capacity);
	}

	@Override
	public FluidStack getFluid() {
		return container.hasTagCompound() ? FluidStack.loadFluidStackFromNBT(container.getTagCompound()) : null;
	}

	@Override
	protected void setFluid(FluidStack fluid) {
		NBTTagCompound nbt;
		if (container.hasTagCompound()) nbt = container.getTagCompound();
		else container.setTagCompound(nbt = new NBTTagCompound());
		fluid.writeToNBT(nbt);
	}

	@Override
	protected void setContainerToEmpty() {
		container.setTagCompound(null);
	}

}
