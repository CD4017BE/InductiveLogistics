package cd4017be.indlog.util;

import java.util.Arrays;

import cd4017be.lib.capability.AbstractInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class VariableInventory extends AbstractInventory {

	public ItemStack[] items;
	public int slots, stackSize;

	public VariableInventory(int maxSlots) {
		items = new ItemStack[maxSlots];
		Arrays.fill(items, ItemStack.EMPTY);
		slots = maxSlots;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		items[slot] = stack;
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
	public int insertAm(int slot, ItemStack item) {
		return stackSize;
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
