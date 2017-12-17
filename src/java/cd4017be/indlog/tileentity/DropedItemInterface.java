package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author CD4017BE
 */
public class DropedItemInterface extends BaseTileEntity implements IGuiData, ITickable, ClientPacketReceiver, IItemHandler {

	public static int INTERVAL, MAX_RANGE;

	private List<EntityItem> entities = new ArrayList<EntityItem>(1);
	private ItemStack[] stacks;
	private AxisAlignedBB area;
	private int count;
	/**drop, front, right, left, down, up */
	private int[] settings = new int[6];

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % INTERVAL != 0) return;
		if (area == null) area = getOrientation().rotate(new AxisAlignedBB(
				pos.add(-settings[2], -settings[4], -settings[1]),
				pos.add(settings[3] + 1, settings[5] + 1, 0)
			));
		entities = world.getEntitiesWithinAABB(EntityItem.class, area);
		count = entities.size();
		stacks = null;
	}

	@Override
	public int getSlots() {
		return entities.size() + 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot >= count) return ItemStack.EMPTY;
		if (stacks == null) stacks = new ItemStack[count];
		ItemStack item = stacks[slot];
		if (item == null) stacks[slot] = item = entities.get(slot).getEntityItem();
		return item;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (simulate) return ItemStack.EMPTY;
		int n = stack.getCount();
		if (n <= 0) return ItemStack.EMPTY;
		boolean insert = false;
		if (slot < count) {
			EntityItem e = entities.get(slot);
			if (!e.isDead) {
				ItemStack item = e.getEntityItem();
				int m = item.getMaxStackSize() - item.getCount();
				if (m > 0 && ItemHandlerHelper.canItemStacksStack(stack, item)) {
					item.grow(n < m ? n : m);
					e.setEntityItemStack(item);
					if (stacks != null) stacks[slot] = item;
					if ((n -= m) <= 0) return ItemStack.EMPTY;
				}
			} else insert = true;
		}
		stack = stack.copy();
		stack.setCount(n);
		BlockPos pos = this.pos.offset(getOrientation().front, settings[0]);
		EntityItem ei = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
		world.spawnEntity(ei);
		if (insert) entities.set(slot, ei);
		else {
			entities.add(ei);
			count++;
			stacks = null;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (slot >= count) return ItemStack.EMPTY;
		EntityItem e = entities.get(slot);
		if (e.isDead) return ItemStack.EMPTY;
		ItemStack stack = e.getEntityItem();
		int n = stack.getCount();
		if (n <= 0) return ItemStack.EMPTY;
		if (n < amount) amount = n;
		if (!simulate) {
			stack.setCount(n - amount);
			e.setEntityItemStack(stack);
		}
		if (stacks != null) stacks[slot] = stack;
		return ItemHandlerHelper.copyStackWithSize(stack, amount);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
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
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		settings = nbt.getIntArray("settings");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setIntArray("settings", settings);
		return super.writeToNBT(nbt);
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd < 0 || cmd >= 6) return;
		int v = data.readByte() & 0xff;
		if (v > MAX_RANGE) v = MAX_RANGE;
		settings[cmd] = v;
		if (cmd > 0) area = null;
	}

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public int[] getSyncVariables() {
		return settings;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		settings[i] = v;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

}
