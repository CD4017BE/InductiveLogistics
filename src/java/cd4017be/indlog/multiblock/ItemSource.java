package cd4017be.indlog.multiblock;

import java.util.function.ToIntFunction;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipePhysics.IItemSrc;
import cd4017be.indlog.util.PipeFilterItem;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author CD4017BE
 *
 */
public class ItemSource extends ItemComp implements IItemSrc {

	public ItemSource(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public boolean blockItem() {
		return filter != null && filter.blocking();
	}

	@Override
	public int extractItem(ItemStack item, int max) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return 0;
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return 0;
		if (!PipeFilterItem.isNullEq(filter)) {
			filter.mode ^= 1;
			max = filter.getExtract(ItemHandlerHelper.copyStackWithSize(item, max), acc).getCount();
			filter.mode ^= 1;
			if (max <= 0) return 0;
		}
		return ItemFluidUtil.drain(acc, item, max);
	}

	@Override
	public ItemStack findItem(ToIntFunction<ItemStack> acceptor) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return null;
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return null;
		for (int i = 0; i < acc.getSlots(); i++) {
			ItemStack stack = acc.getStackInSlot(i);
			if (stack.getCount() <= 0 || !PipeFilterItem.isNullEq(filter) && filter.getExtract(stack, acc) == ItemStack.EMPTY) continue;
			int n = acceptor.applyAsInt(stack);
			if (n > 0) return ItemHandlerHelper.copyStackWithSize(stack, n);
		}
		return null;
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 4);
	}

}
