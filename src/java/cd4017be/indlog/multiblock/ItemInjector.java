package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.lib.TickRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * @author CD4017BE
 *
 */
public class ItemInjector extends ItemComp implements IActiveCon {

	public static int INTERVAL = 4;

	private int timer = Integer.MIN_VALUE;
	private int slotIdx;

	public ItemInjector(WarpPipeNode pipe, byte side) {
		super(pipe, side);
		slotIdx = 0;
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
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[this.side^1]);
		if (acc == null) return true;
		byte pr = filter == null || !filter.blocking() ? Byte.MIN_VALUE : filter.priority();
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
			return true;
		}
		return true;
	}

	private int acceptAm(ItemStack item, IItemHandler acc, int s) {
		int n = 65536;
		item.setCount(n);
		if ((n -= acc.insertItem(s, item, true).getCount()) <= 0) return 0;
		if (filter == null || filter.noEffect()) return n;
		item.setCount(n);
		return filter.insertAmount(item, acc);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 1);
	}

}
