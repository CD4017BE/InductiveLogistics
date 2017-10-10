package cd4017be.indlog.util;

import cd4017be.lib.Gui.ItemGuiData.IInventoryItem;
import cd4017be.lib.templates.AbstractInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;

public class ItemInventoryHandler extends AbstractInventory implements IInventoryItem, ICapabilityProvider {

	public final ItemStack inv;
	public final ItemStack[] cache;
	public final int stacksize;
	

	public ItemInventoryHandler(ItemStack inv, int slots, int stacksize) {
		this.inv = inv;
		this.cache = new ItemStack[slots];
		this.stacksize = stacksize;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		cache[slot] = stack;
		String k = Integer.toHexString(slot);
		if (stack.isEmpty()) inv.removeSubCompound(k);
		else inv.setTagInfo(k, stack.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public int getSlots() {
		return cache.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		ItemStack stack = cache[slot];
		if (stack != null) return stack;
		NBTTagCompound nbt = inv.getSubCompound(Integer.toHexString(slot));
		return cache[slot] = nbt == null ? ItemStack.EMPTY : new ItemStack(nbt);
	}

	@Override
	public int getSlotLimit(int slot) {
		return stacksize;
	}

	@Override
	public int insertAm(int slot, ItemStack item) {
		return stacksize;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) this : null;
	}

	@Override
	public void update() {
		for (int i = 0; i < cache.length; i++) {
			NBTTagCompound nbt = inv.getSubCompound(Integer.toHexString(i));
			cache[i] = nbt == null ? ItemStack.EMPTY : new ItemStack(nbt);
		}
	}

}
