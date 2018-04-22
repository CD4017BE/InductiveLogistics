package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipePhysics.IFluidDest;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
public class FluidAccess extends FluidSource implements IFluidDest {

	public FluidAccess(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 0);
	}

}
