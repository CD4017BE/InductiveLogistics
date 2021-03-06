package cd4017be.indlog.multiblock;

import net.minecraft.item.ItemStack;
import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IFluidDest;

/**
 * 
 * @author CD4017BE
 *
 */
public class FluidDestination extends FluidComp implements IFluidDest {

	public FluidDestination(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 3);
	}

}
