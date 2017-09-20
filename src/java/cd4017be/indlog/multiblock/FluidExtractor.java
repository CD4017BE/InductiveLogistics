package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.block.BaseTileEntity;
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

	public FluidExtractor(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void update() {
		if (!this.isValid()) return;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null || (filter != null && !filter.active(pipe.redstone))) return;
		FluidStack stack = PipeFilterFluid.isNullEq(filter) ? acc.drain(Integer.MAX_VALUE, false) : filter.getExtract(null, acc);
		if (stack == null) return;
		int n = stack.amount;
		FluidStack result = pipe.network.insertFluid(stack.copy(), filter == null || (filter.mode & 2) == 0 ? Byte.MAX_VALUE : filter.priority);
		if (result != null) stack.amount -= result.amount;
		if (n > 0) acc.drain(stack, true);
	}
	
	@Override
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item, long uid) {
		if (super.onClicked(player, hand, item, uid)) return true;
		if (player.isSneaking() && player.getHeldItemMainhand().getCount() == 0) {
			ItemFluidUtil.dropStack(new ItemStack(Objects.fluidPipe, 1, 2), player);
			pipe.con[side] = 0;
			pipe.network.remConnector(pipe, side);
			pipe.updateCon = true;
			pipe.hasFilters &= ~(1 << side);
			((BaseTileEntity)pipe.tile).markUpdate();
			return true;
		}
		return false;
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		list.add(new ItemStack(Objects.fluidPipe, 1, 2));
		super.dropContent(list);
	}

}
