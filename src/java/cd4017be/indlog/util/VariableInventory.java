package cd4017be.indlog.util;

import java.util.Arrays;
import cd4017be.lib.util.Callback;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author cd4017be
 */
public class VariableInventory implements IItemHandlerModifiable {

	public ItemStack[] items;
	public int slots, stackSize;
	private final Callback onModified;
	public boolean locked;

	public VariableInventory(int maxSlots, Callback modificationCallback) {
		this.onModified = modificationCallback;
		items = new ItemStack[maxSlots];
		Arrays.fill(items, ItemStack.EMPTY);
		slots = maxSlots;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		items[slot] = stack;
		onModified.call();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return items[slot];
	}

	@Override
	public int getSlots() {
		return slots;
	}

	@Override
	public int getSlotLimit(int slot) {
		return stackSize;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack item = items[slot];
		int n = item.getCount(), m = stack.getCount();
		if (m <= 0 || n != 0 && !ItemHandlerHelper.canItemStacksStack(item, stack)) return stack;
		if (n == Integer.MAX_VALUE) n = 0;
		if (m > stackSize - n && (m = stackSize - n) <= 0) return stack;
		if (!simulate) {
			if (n == 0) setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(stack, m));
			else {
				item.grow(m);
				setStackInSlot(slot, item);
			}
		}
		return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - m);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack item = items[slot];
		int n = item.getCount();
		if (n == Integer.MAX_VALUE) {
			if (!locked) setStackInSlot(slot, ItemStack.EMPTY);
			return ItemStack.EMPTY;
		}
		if (n < amount) amount = n;
		if (amount <= 0) return ItemStack.EMPTY;
		if (!simulate) {
			if (amount < n) {
				item.shrink(amount);
				setStackInSlot(slot, item);
			} else if (locked) {
				item.setCount(Integer.MAX_VALUE);
				setStackInSlot(slot, item);
			} else setStackInSlot(slot, ItemStack.EMPTY);
		}
		return ItemHandlerHelper.copyStackWithSize(item, amount);
	}

	public class GroupAccess implements IItemHandler {

		public int start, size;

		public GroupAccess() {
			this.start = 0;
			this.size = slots;
		}

		public void setRange(int start, int end) {
			if (end > items.length) end = items.length;
			if (start < 0) start = 0;
			this.start = start;
			this.size = end < start ? 0 : end - start;
		}

		@Override
		public int getSlots() {
			return size;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return items[slot + start];
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return VariableInventory.this.insertItem(slot + start, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return VariableInventory.this.extractItem(slot + start, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return stackSize;
		}

	}

}
