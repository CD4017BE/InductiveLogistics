package cd4017be.indlog.util;

import java.util.Arrays;

import cd4017be.lib.templates.AbstractInventory;
import net.minecraft.item.ItemStack;

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

}
