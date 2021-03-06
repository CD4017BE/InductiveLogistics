package cd4017be.indlog.multiblock;

import java.util.function.ToIntFunction;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IItemDest;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IItemSrc;
import cd4017be.indlog.util.filter.FilterBase;
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

	public ItemSource(WarpPipeNode pipe, byte side) {
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
		if (filter != null && !filter.noEffect()) {
			if (filter instanceof FilterBase && this instanceof IItemDest) {
				FilterBase<?,?> f = (FilterBase<?,?>)filter;
				f.mode ^= 1;
				max = filter.getExtract(ItemHandlerHelper.copyStackWithSize(item, max), acc).getCount();
				f.mode ^= 1;
			} else max = filter.getExtract(ItemHandlerHelper.copyStackWithSize(item, max), acc).getCount();
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
			if (stack.getCount() <= 0) continue;
			if (filter != null && !filter.noEffect()) {
				if (filter instanceof FilterBase && this instanceof IItemDest) {
					FilterBase<?,?> f = (FilterBase<?,?>)filter;
					f.mode ^= 1;
					stack = filter.getExtract(stack, acc);
					f.mode ^= 1;
				} else stack = filter.getExtract(stack, acc);
				if (stack == ItemStack.EMPTY) continue;
			}
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
