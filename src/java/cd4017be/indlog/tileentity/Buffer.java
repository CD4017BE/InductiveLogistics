package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cd4017be.indlog.util.VariableInventory;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.BaseTileEntity;
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

public class Buffer extends BaseTileEntity implements ITilePlaceHarvest, IGuiData, ClientPacketReceiver {

	public static final int[] SLOTS = {12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
							STACKS = {64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public final VariableInventory inventory;
	public byte type;

	public Buffer() {
		inventory = new VariableInventory(0);
	}

	public Buffer(IBlockState state) {
		super(state);
		type = (byte)blockType.getMetaFromState(state);
		inventory = new VariableInventory(SLOTS[type]);
		inventory.stackSize = STACKS[type];
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) inventory : null;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("type", type);
		save(nbt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		type = nbt.getByte("type");
		inventory.items = new ItemStack[SLOTS[type]];
		load(nbt);
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
		inventory.stackSize = nbt.getShort("stack") & 0xffff;
		Arrays.fill(inventory.items, ItemStack.EMPTY);
		for (NBTBase t : nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound tag = (NBTTagCompound)t;
			int s = tag.getByte("Slot") & 0xff;
			if (s < inventory.items.length) {
				ItemStack stack = new ItemStack(tag);
				stack.setCount(tag.getShort("Num"));
				inventory.items[s] = stack;
			}
		}
	}

	private void save(NBTTagCompound nbt) {
		nbt.setByte("slots", (byte) inventory.slots);
		nbt.setShort("stack", (short) inventory.stackSize);
		NBTTagList list = new NBTTagList();
		for (int s = 0; s < inventory.items.length; s++){
			ItemStack stack = inventory.items[s];
			if (!stack.isEmpty()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte)s);
				stack.writeToNBT(tag);
				tag.removeTag("Count");
				tag.setShort("Num", (short)stack.getCount());
				list.appendTag(tag);
			}
		}
		nbt.setTag("Items", list);
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
		return new int[] {inventory.slots, inventory.stackSize};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0: inventory.slots = v; break;
		case 1: inventory.stackSize = v; break;
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
			inventory.stackSize = data.readShort() & 0xffff;
			if (inventory.stackSize > STACKS[type]) inventory.stackSize = STACKS[type];
			break;
		}
	}

}
