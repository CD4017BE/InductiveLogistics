package cd4017be.indlog.multiblock;

import java.util.function.ToIntFunction;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IFluidSrc;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


/**
 * @author CD4017BE
 *
 */
public class FluidSource extends FluidComp implements IFluidSrc {

	public FluidSource(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public boolean blockFluid() {
		return filter != null && filter.blocking();
	}

	@Override
	public int extractFluid(FluidStack fluid, int max) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return 0;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return 0;
		fluid = new FluidStack(fluid, max);
		if (filter != null && !filter.noEffect()) {
			fluid = filter.getExtract(fluid, acc);
			if (fluid == null) return 0;
		}
		fluid = acc.drain(fluid, true);
		return fluid == null ? 0 : fluid.amount;
	}

	@Override
	public FluidStack findFluid(ToIntFunction<FluidStack> acceptor) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return null;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return null;
		FluidStack stack;
		IFluidTankProperties[] tankProps = acc.getTankProperties();
		if (tankProps == null) return null;
		for (IFluidTankProperties inf : tankProps) {
			if (inf == null) continue;
			if((stack = inf.getContents()) != null && inf.canDrainFluidType(stack)) {
				if (filter != null && !filter.noEffect()) {
					stack = filter.getExtract(stack, acc);
					if (stack == null) continue;
				}
				int n = acceptor.applyAsInt(stack);
				if (n > 0) return new FluidStack(stack, n);
			}
		}
		return null;
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 4);
	}

}
