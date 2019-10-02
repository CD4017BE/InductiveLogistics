package cd4017be.indlog.modCompat;

import static cd4017be.lib.util.ItemFluidUtil.listTanks;
import cd4017be.api.indlog.filter.FluidFilterProvider;
import cd4017be.api.indlog.filter.PipeFilter;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.indlog.filter.DummyFilter;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


/**
 * @author CD4017BE
 *
 */
public class FilteredFluidSensor implements IBlockSensor {

	final PipeFilter<FluidStack, IFluidHandler> filter;

	public FilteredFluidSensor(ItemStack stack) {
		if (stack.getItem() instanceof FluidFilterProvider)
			this.filter = ((FluidFilterProvider)stack.getItem()).getFluidFilter(stack);
		else this.filter = new DummyFilter<>((byte)0);
	}

	@Override
	public int readValue(BlockReference block) {
		IFluidHandler inv = block.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (inv == null) return 0;
		int val = 0;
		for (IFluidTankProperties prop : listTanks(inv)) {
			FluidStack stack = prop.getContents();
			if (stack != null && filter.matches(stack))
				val += stack.amount;
		}
		return val;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.indlog.fluid_filter");
	}

	@Override
	public ResourceLocation getModel() {
		return new ResourceLocation("rs_ctr:block/_sensor.fluid()");
	}

}
