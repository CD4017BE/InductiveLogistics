package cd4017be.indlog.multiblock;

import net.minecraft.item.ItemStack;
import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipePhysics.IFluidDest;

/**
 * 
 * @author CD4017BE
 *
 */
public class FluidDestination extends FluidComp implements IFluidDest {

	public FluidDestination(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.FLUID_PIPE, 1, 1);
	}

}
