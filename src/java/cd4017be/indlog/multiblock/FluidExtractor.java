package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.lib.TickRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * 
 * @author CD4017BE
 *
 */
public class FluidExtractor extends FluidComp implements IActiveCon {

	public static int INTERVAL = 4;

	private int timer = Integer.MIN_VALUE;

	public FluidExtractor(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void enable() {
		if (timer < 0 && !pipe.invalid()) {
			timer = 0;
			TickRegistry.instance.add(this);
		}
	}

	@Override
	public void disable() {
		timer = Integer.MIN_VALUE;
	}

	@Override
	public boolean tick() {
		if (++timer < INTERVAL) return timer > 0;
		if (pipe.invalid()) {
			disable();
			return false;
		} else timer = 0;
		
		if (!isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return true;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return true;
		FluidStack stack = filter.noEffect() ? acc.drain(Integer.MAX_VALUE, false) : filter.getExtract(null, acc);
		if (stack == null) return true;
		int n = stack.amount;
		FluidStack result = pipe.network.insertFluid(stack.copy(), filter == null || !filter.blocking() ? Byte.MAX_VALUE : filter.priority());
		if (result != null) stack.amount -= result.amount;
		if (n > 0) acc.drain(stack, true);
		return true;
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.fluid_pipe, 1, 2);
	}

}
