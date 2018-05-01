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
public class DropedItemInterface extends BaseTileEntity implements IGuiData, ClientPacketReceiver, IItemHandler {

	public static int INTERVAL = 20, MAX_RANGE = 15, INV_SIZE = 5;

	private List<EntityItem> entities = new ArrayList<EntityItem>(1);
	private AxisAlignedBB area;
	private int count;
	/**drop, front, right, left, down, up */
	public final int[] settings = new int[] {1, 1, 0, 0, 0, 0};
	private long lastCheck;

	private void check() {
		if (area == null) area = getOrientation().rotate(new AxisAlignedBB(-settings[2], -settings[4], -settings[1], settings[3] + 1, settings[5] + 1, 0)).offset(pos);
		entities = world.getEntitiesWithinAABB(EntityItem.class, area);
		entities.add(null);
		count = entities.size();
	}

	@Override
	public int getSlots() {
		return count;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		EntityItem e = entities.get(slot);
		if (e != null) {
			if (e.isDead) entities.set(slot, null);
			else return e.getEntityItem();
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		int n = stack.getCount();
		if (n <= 0) return ItemStack.EMPTY;
		EntityItem e = entities.get(slot);
		if (e == null || e.isDead) {
			n -= stack.getMaxStackSize();
			if (!simulate) {
				stack = stack.copy();
				if (n > 0) stack.grow(-n);
				BlockPos pos = this.pos.offset(getOrientation().front, settings[0]);
				e = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
				e.motionX = 0;
				e.motionY = 0;
				e.motionZ = 0;
				world.spawnEntity(e);
				entities.set(slot, e);
				if (++slot == count && count < INV_SIZE) {
					entities.add(slot, null);
					count++;
				}
			}
			return n > 0 ? ItemHandlerHelper.copyStackWithSize(stack, n) : ItemStack.EMPTY;
		}
		ItemStack item = e.getEntityItem();
		int m = item.getMaxStackSize() - item.getCount();
		if (m < 0 || !ItemHandlerHelper.canItemStacksStack(stack, item)) return stack;
		if (!simulate) {
			item.grow(n < m ? n : m);
			e.setEntityItemStack(item);
		}
		return (n -= m) > 0 ? ItemHandlerHelper.copyStackWithSize(item, n) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		EntityItem e = entities.get(slot);
		if (e == null) return ItemStack.EMPTY;
		if (!e.isDead) {
			ItemStack stack = e.getEntityItem();
			int n = stack.getCount();
			if (n > 0) {
				if (n < amount) amount = n;
				ItemStack item = ItemHandlerHelper.copyStackWithSize(stack, amount);
				if (!simulate) {
					stack.setCount(n -= amount);
					e.setEntityItemStack(stack);
					if (n <= 0) entities.set(slot, null);
				}
				return item;
			}
		}
		entities.set(slot, null);
		return ItemStack.EMPTY;
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
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			long t = world.getTotalWorldTime();
			if (t - lastCheck > INTERVAL) {
				lastCheck = t;
				check();
			}
			return (T) this;
		}
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		int[] arr = nbt.getIntArray("settings");
		System.arraycopy(arr, 0, settings, 0, Math.min(arr.length, settings.length));
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
		if (cmd == 0 && v > settings[1]) v = settings[1];
		settings[cmd] = v;
		if (cmd == 1 && settings[0] > v) settings[0] = v;
		if (cmd > 0) area = null;
		markDirty();
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
