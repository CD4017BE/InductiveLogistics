package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cd4017be.indlog.util.VariableInventory;
import cd4017be.indlog.util.VariableInventory.GroupAccess;
import cd4017be.lib.block.AdvancedBlock.IComparatorSource;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * 
 * @author cd4017be
 */
public class Buffer extends BaseTileEntity implements ITilePlaceHarvest, IGuiData, ClientPacketReceiver, IComparatorSource {

	public static final int[] SLOTS = {12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
							STACKS = {64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public final VariableInventory inventory;
	public final GroupAccess[] sideAccs = new GroupAccess[6];
	public byte type;
	private int comparator;

	public Buffer() {
		inventory = new VariableInventory(0, this::markDirty);
	}

	public Buffer(IBlockState state) {
		super(state);
		type = (byte)blockType.getMetaFromState(state);
		inventory = new VariableInventory(SLOTS[type], this::markDirty);
		inventory.stackSize = STACKS[type];
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			if (facing == null) return (T) inventory;
			IItemHandler acc = sideAccs[facing.ordinal()];
			return (T) (acc != null ? acc : inventory);
		} else return null;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("type", type);
		nbt.setBoolean("lock", inventory.locked);
		save(nbt);
		for (int i = 0; i < sideAccs.length; i++) {
			GroupAccess acc = sideAccs[i];
			if (acc == null) continue;
			NBTTagCompound tag = new NBTTagCompound();
			tag.setByte("s", (byte)acc.start);
			tag.setByte("e", (byte)(acc.start + acc.size));
			nbt.setTag("sacc" + i, tag);
		}
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		type = nbt.getByte("type");
		inventory.items = new ItemStack[SLOTS[type]];
		inventory.locked = nbt.getBoolean("lock");
		load(nbt);
		for (int i = 0; i < sideAccs.length; i++) {
			String k = "sacc" + i;
			if (nbt.hasKey(k, Constants.NBT.TAG_COMPOUND)) {
				NBTTagCompound tag = nbt.getCompoundTag(k);
				GroupAccess acc = inventory.new GroupAccess();
				acc.setRange(tag.getByte("s") & 0xff, tag.getByte("e") & 0xff);
				sideAccs[i] = acc;
			} else sideAccs[i] = null;
		}
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (item.hasTagCompound()) load(item.getTagCompound());
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		NBTTagCompound nbt = new NBTTagCompound();
		save(nbt);
		return makeDefaultDrops(nbt);
	}

	private void load(NBTTagCompound nbt) {
		inventory.slots = nbt.getByte("slots") & 0xff;
		inventory.stackSize = nbt.getInteger("stack");
		if (inventory.stackSize < 1) inventory.stackSize = 1;
		Arrays.fill(inventory.items, ItemStack.EMPTY);
		for (NBTBase t : nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound tag = (NBTTagCompound)t;
			int s = tag.getByte("Slot") & 0xff;
			if (s < inventory.items.length) {
				ItemStack stack = new ItemStack(tag);
				stack.setCount(tag.getInteger("Num"));
				inventory.items[s] = stack;
			}
		}
		comparator = inventory.getComparatorValue();
	}

	private void save(NBTTagCompound nbt) {
		nbt.setByte("slots", (byte) inventory.slots);
		nbt.setInteger("stack", inventory.stackSize);
		NBTTagList list = new NBTTagList();
		for (int s = 0; s < inventory.items.length; s++){
			ItemStack stack = inventory.items[s];
			if (!stack.isEmpty()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte)s);
				stack.writeToNBT(tag);
				tag.removeTag("Count");
				tag.setInteger("Num", stack.getCount());
				list.appendTag(tag);
			}
		}
		nbt.setTag("Items", list);
	}

	public byte selSide = -1;

	public int getStart() {
		if (selSide < 0 || selSide >= sideAccs.length) return 0;
		GroupAccess acc = sideAccs[selSide];
		return acc == null ? 0 : acc.start;
	}

	public int getEnd() {
		if (selSide < 0 || selSide >= sideAccs.length) return inventory.slots;
		GroupAccess acc = sideAccs[selSide];
		return acc == null ? inventory.slots : acc.start + acc.size;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		int n = inventory.items.length;
		for (int i = 0; i < n; i++)
			cont.addItemSlot(new GlitchSaveSlot(inventory, i, 8 + i % 12 * 18, 16 + i / 12 * 18));
		cont.addPlayerInventory(8, 32 + (n + 11) / 12 * 18);
	}

	@Override
	public int[] getSyncVariables() {
		int[] data = new int[8];
		data[0] = inventory.slots | (inventory.locked ? 0x10000 : 0);
		data[1] = inventory.stackSize;
		for (int i = 0; i < sideAccs.length; i++) {
			GroupAccess acc = sideAccs[i];
			data[i + 2] = acc == null ? 0 : acc.start & 0xff | acc.size << 8 & 0xff00 | 0x10000;
		}
		return data;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0:
			inventory.slots = v & 0xffff;
			inventory.locked = (v & 0x10000) != 0;
			break;
		case 1: inventory.stackSize = v; break;
		default: if ((i-=2) >= sideAccs.length) return;
			if (v == 0) sideAccs[i] = null;
			else {
				GroupAccess acc = sideAccs[i];
				if (acc == null) sideAccs[i] = acc = inventory.new GroupAccess();
				acc.setRange(v & 0xff, (v & 0xff) + (v >> 8 & 0xff));
			}
		}
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		switch(data.readByte()) {
		case 0:
			inventory.slots = data.readByte() & 0xff;
			if (inventory.slots > SLOTS[type]) inventory.slots = SLOTS[type];
			break;
		case 1:
			inventory.stackSize = data.readInt();
			if (inventory.stackSize > STACKS[type]) inventory.stackSize = STACKS[type];
			else if (inventory.stackSize < 1) inventory.stackSize = 1;
			break;
		case 2: {
			int s = data.readByte();
			if (s < 0 || s >= sideAccs.length) return;
			sideAccs[s] = null;
		} break;
		case 3: {
			int s = data.readByte();
			if (s < 0 || s >= sideAccs.length) return;
			GroupAccess acc = sideAccs[s];
			if (acc == null) sideAccs[s] = acc = inventory.new GroupAccess();
			acc.setRange(data.readByte() & 0xff, data.readByte() & 0xff);
		} break;
		case 4:
			inventory.locked ^= true;
			break;
		}
		markDirty();
	}

	@Override
	public void markDirty() {
		if (!world.isRemote && comparator >= 0) {
			comparator = -1;
			TickRegistry.instance.updates.add(()-> world.updateComparatorOutputLevel(pos, blockType));
			//The way this is implemented, there will be no further comparator updates triggered as long as nobody has asked for the comparator signal in the mean time, which makes this super performance efficient.
		}
		super.markDirty();
	}

	@Override
	public int comparatorValue() {
		return comparator < 0 ? comparator = inventory.getComparatorValue() : comparator;
	}

}
