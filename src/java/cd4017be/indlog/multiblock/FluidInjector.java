package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterFluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * @author CD4017BE
 *
 */
public class FluidInjector extends FluidComp implements ITickable {

	private int slotIdx;

	public FluidInjector(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
		slotIdx = 0;
	}

	@Override
	public void update() {
		if (!isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return;
		byte pr = filter == null || (filter.mode & 2) == 0 ? Byte.MIN_VALUE : filter.priority;
		IFluidTankProperties[] tprs = acc.getTankProperties();
		int m = tprs.length;
		for (int i = slotIdx; i < slotIdx + m; i++) {
			IFluidTankProperties prop = tprs[i % m];
			if (!prop.canFill()) continue;
			FluidStack stack = prop.getContents();
			int n = prop.getCapacity();
			if (n <= 0) continue;
			if (stack == null) {
				stack = pipe.network.extractFluid((fluid)-> acceptAm(fluid.copy(), acc), pr);
				if (stack != null) acc.fill(stack, true);
			} else if (stack.amount < n) {
				if ((n = acceptAm(stack, acc)) <= 0) continue;
				if ((n = pipe.network.extractFluid(stack, n, pr)) > 0) {
					stack.amount = n;
					acc.fill(stack, true);
				}
			} else continue;
			slotIdx = (i + 1) % m;
			return;
		}
	}

	private int acceptAm(FluidStack fluid, IFluidHandler acc) {
		fluid.amount = 65536;
		int n = acc.fill(fluid, false);
		if (n <= 0) return 0;
		if (PipeFilterFluid.isNullEq(filter)) return n;
		fluid.amount = n;
		return filter.insertAmount(fluid, acc);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 1);
	}

}
