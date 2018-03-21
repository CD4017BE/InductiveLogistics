package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidExtractor extends FluidComp implements ITickable {

	public static byte TICKS;

	private byte timer = 0;

	public FluidExtractor(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void update() {
		if ((++timer & 0xff) < TICKS || !isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return;
		timer = 0;
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
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item) {
		if (super.onClicked(player, hand, item)) return true;
		if (player.isSneaking() && player.getHeldItemMainhand().getCount() == 0) {
			if (!player.isCreative()) ItemFluidUtil.dropStack(new ItemStack(Objects.FLUID_PIPE, 1, 2), player);
			pipe.network.remConnector(pipe, side);
			return true;
		}
		return false;
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		list.add(new ItemStack(Objects.FLUID_PIPE, 1, 2));
		super.dropContent(list);
	}

}
