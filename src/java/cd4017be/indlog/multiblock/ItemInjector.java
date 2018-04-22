package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * @author CD4017BE
 *
 */
public class ItemInjector extends ItemComp implements ITickable {

	private int slotIdx;

	public ItemInjector(WarpPipeNode pipe, byte side) {
		super(pipe, side);
		slotIdx = 0;
	}

	@Override
	public void update() {
		if (!isValid() || (filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return;
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[this.side^1]);
		if (acc == null) return;
		byte pr = filter == null || (filter.mode & 2) == 0 ? Byte.MIN_VALUE : filter.priority;
		int m = acc.getSlots();
		for (int i = slotIdx; i < slotIdx + m; i++) {
			int s = i % m;
			ItemStack stack = acc.getStackInSlot(s);
			if (stack.getCount() <= 0) {
				stack = pipe.network.extractItem((item)-> acceptAm(item.copy(), acc, s), pr);
				if (stack != null) acc.insertItem(s, stack, false);
			} else {
				int n = acceptAm(stack = stack.copy(), acc, s);
				if (n <= 0) continue;
				if ((n = pipe.network.extractItem(stack, n, pr)) > 0) {
					stack.setCount(n);
					acc.insertItem(s, stack, false);
				}
			}
			slotIdx = s + 1;
			return;
		}
	}

	private int acceptAm(ItemStack item, IItemHandler acc, int s) {
		int n = 65536;
		item.setCount(n);
		if ((n -= acc.insertItem(s, item, true).getCount()) <= 0) return 0;
		if (PipeFilterItem.isNullEq(filter)) return n;
		item.setCount(n);
		return filter.insertAmount(item, acc);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 1);
	}

}
