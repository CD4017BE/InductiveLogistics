package cd4017be.indlog.multiblock;

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
public class FluidProvider extends FluidComp implements ITickable {

	public FluidProvider(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void update() {
		if (!isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return;
		int am = 0;
		for (IFluidTankProperties prop : acc.getTankProperties())
			if (prop.canFill()) {
				FluidStack stack = prop.getContents();
				int n = prop.getCapacity();
				if (stack == null) am += n;
				else if (stack.amount < n && (PipeFilterFluid.isNullEq(filter) || (n = filter.insertAmount(stack, acc)) > 0)) {
					stack = new FluidStack(stack, n);
					n = acc.fill(stack, false);
					if (n <= 0) continue;
					stack.amount = n;
					//TODO pipe.network.extractFluid(stack)
					if (stack.amount > 0) {
						acc.fill(stack, true);
						return;
					}
				}
			}
		if (am <= 0) return;
		//TODO get any
	}

	@Override
	protected ItemStack moduleItem() {
		// TODO Auto-generated method stub
		return null;
	}

}
