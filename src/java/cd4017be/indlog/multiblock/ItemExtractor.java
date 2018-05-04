package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.lib.TickRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * 
 * @author CD4017BE
 *
 */
public class ItemExtractor extends ItemComp implements IActiveCon {

	public static int INTERVAL = 4;

	private int timer = Integer.MIN_VALUE;
	private int slotIdx;

	public ItemExtractor(WarpPipeNode pipe, byte side) {
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
		int s, target = -1, m = acc.getSlots();
		ItemStack stack = ItemStack.EMPTY;
		for (int i = slotIdx; i < slotIdx + m; i++) {
			stack = acc.extractItem(s = i % m, 65536, true);
			if (stack.getCount() > 0 && (filter == null || (stack = filter.getExtract(stack, acc)).getCount() > 0)) {
				target = s;
				slotIdx = (i + 1) % m;
				break;
			}
		}
		m = stack.getCount();
		if (target < 0 || m == 0) return true;
		m -= pipe.network.insertItem(stack, filter == null || (filter.mode & 2) == 0 ? Byte.MAX_VALUE : filter.priority).getCount();
		if (m > 0) acc.extractItem(target, m, false);
		return true;
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 2);
	}

}
