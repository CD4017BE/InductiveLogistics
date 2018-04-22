package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterFluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * 
 * @author CD4017BE
 *
 */
public class FluidExtractor extends FluidComp implements ITickable {

	public FluidExtractor(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void update() {
		if (!isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return;
		FluidStack stack = PipeFilterFluid.isNullEq(filter) ? acc.drain(Integer.MAX_VALUE, false) : filter.getExtract(null, acc);
		if (stack == null) return;
		int n = stack.amount;
		FluidStack result = pipe.network.insertFluid(stack.copy(), filter == null || (filter.mode & 2) == 0 ? Byte.MAX_VALUE : filter.priority);
		if (result != null) stack.amount -= result.amount;
		if (n > 0) acc.drain(stack, true);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 2);
	}

}
