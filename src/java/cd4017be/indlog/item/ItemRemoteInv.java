package cd4017be.indlog.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import java.util.Arrays;
import java.util.List;
import cd4017be.indlog.render.gui.GuiRemoteInventory;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.SlotItemType;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.capability.BasicInventory;
import cd4017be.lib.capability.InventoryItem;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Utils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ItemRemoteInv extends ItemFilteredSubInventory {

	public static int INTERVAL = 20, MAX_SLOTS = 48;

	public ItemRemoteInv(String id) {
		super(id);
		this.setMaxStackSize(1);
	}

	@Override
	protected int tickTime() {
		return INTERVAL;
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return new CapabilityProvider(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) list.add(TooltipUtil.formatLink(BlockPos.fromLong(nbt.getLong("pos")), EnumFacing.getFront(nbt.getByte("s")), nbt.getInteger("d")));
		super.addInformation(item, player, list, b);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing s, float X, float Y, float Z) {
		if (!player.isSneaking()) return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		ItemStack item = player.getHeldItem(hand);
		TileEntity te = world.getTileEntity(pos);
		IItemHandler acc = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, s) : null;
		if (acc == null) {
			player.sendMessage(new TextComponentString(TooltipUtil.translate("gui.cd4017be.remote.invalid")));
			return EnumActionResult.SUCCESS;
		}
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) item.setTagCompound(nbt = new NBTTagCompound());
		nbt.setLong("pos", pos.toLong());
		nbt.setByte("s", (byte)s.getIndex());
		nbt.setInteger("d", player.dimension);
		player.sendMessage(new TextComponentString(TooltipUtil.translate("gui.cd4017be.remote.linked")));
		return EnumActionResult.SUCCESS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		if (!player.isSneaking() && !world.isRemote) {
			IItemHandler acc = item.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			BlockGuiHandler.openGui(player, world, new BlockPos(acc == null ? 0 : acc.getSlots(), -1, 0), hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40);
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new TileContainer(new GuiData(pos.getX()), player);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiRemoteInventory((TileContainer) getContainer(item, player, world, pos, slot));
	}

	@Override
	protected void customPlayerCommand(ItemStack item, EntityPlayer player, byte cmd, PacketBuffer dis) {
		if (cmd == 2 && player.openContainer instanceof TileContainer) {//set all reference ItemStacks to null, so the server thinks they changed and sends the data again.
			for (Slot s : player.openContainer.inventorySlots)
				if (s instanceof GlitchSaveSlot)
					player.openContainer.inventoryItemStacks.set(s.slotNumber, ItemStack.EMPTY);
			Arrays.fill(((TileContainer)player.openContainer).refInts, 0);
		} else if (cmd == 3) {
			NBTTagCompound nbt = item.getTagCompound();
			if (nbt == null) return;
			int offset = dis.readInt();
			if (offset < 0) offset = 0;
			nbt.setInteger("ofs", offset);
			if (player.openContainer instanceof TileContainer) {
				TileContainer cont = (TileContainer)player.openContainer;
				if (cont.data instanceof GuiData) {
					GuiData data = (GuiData)cont.data;
					data.offset = offset;
				}
			}
		}
	}

	public class GuiData extends ItemGuiData implements IItemHandler {

		public TileEntity link;
		public IItemHandler linkedInv;
		public final int size, ofsY;
		public int offset, slots;

		public GuiData(int size) {
			super(ItemRemoteInv.this);
			if (size > MAX_SLOTS) size = MAX_SLOTS;
			else if (size < 12 && size != 0) size = 12;
			this.size = size;
			this.ofsY = (size - 1) / 12 * 18 - 18;
		}

		@Override
		public void initContainer(DataContainer container) {
			TileContainer cont = (TileContainer)container;
			this.inv = new InventoryItem(cont.player);
			ItemStack item = cont.player.inventory.mainInventory.get(cont.player.inventory.currentItem);
			IItemHandler inventory = null;
			if (!cont.player.world.isRemote) {
				NBTTagCompound nbt = item.getTagCompound();
				link = getLink(nbt);
				linkedInv = link != null ? link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[(nbt.getByte("s") & 0xff) % 6]) : null;
				if (linkedInv != null && (linkedInv == cont.player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN)
						|| linkedInv == cont.player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH)
						|| linkedInv == cont.player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null))) {
					cont.player.sendMessage(new TextComponentString(TooltipUtil.translate("gui.cd4017be.remote.selfAcc")));
					link = null;
				}
				if (linkedInv != null) {
					inventory = this;
					slots = linkedInv.getSlots(); 
					offset = Math.max(0, Math.min(nbt.getInteger("ofs"), slots - size));
				}
			} else if (size > 0) {
				linkedInv = inventory = new BasicInventory(size);
				//Workaround to fix an inventory sync bug. Sends a request to server that it should send the inventory data again.
				PacketBuffer dos = BlockGuiHandler.getPacketForItem(cont.player.inventory.currentItem);
				dos.writeByte(2);
				BlockGuiHandler.sendPacketToServer(dos);
			}
			if (inventory == null) linkedInv = inventory = new BasicInventory(0);
			cont.addItemSlot(new SlotItemType(inv, 0, 8, 86 + ofsY, FILTER_SLOT));
			cont.addItemSlot(new SlotItemType(inv, 1, 26, 86 + ofsY, FILTER_SLOT));
			if (size > 0) {
				int h = size / 12;
				int w = size % 12;
				for (int j = 0; j < h; j++)
					for (int i = 0; i < 12; i++)
						cont.addItemSlot(new GlitchSaveSlot(inventory, i + 12 * j, 8 + i * 18, 16 + j * 18, false));
				for (int i = 0; i < w; i++)
					cont.addItemSlot(new GlitchSaveSlot(inventory, i + 12 * h, 8 + i * 18, 16 + h * 18, false));
			}
			cont.addPlayerInventory(62, 68 + ofsY, false, true);
		}

		@Override
		public int[] getSyncVariables() {
			return new int[] {slots = linkedInv == null ? 0 : linkedInv.getSlots(), offset};
		}

		@Override
		public void setSyncVariable(int i, int v) {
			switch(i) {
			case 0: slots = v; break;
			case 1: offset = v; break;
			}
		}

		@Override
		public boolean canPlayerAccessUI(EntityPlayer player) {
			ItemStack item = player.inventory.mainInventory.get(player.inventory.currentItem);
			if (player.isDead || item.getItem() != this.item || !item.hasTagCompound()) return false;
			if (size == 0) return true;
			if (link == null || link != Utils.getTileAt(link.getWorld(), link.getPos())) return false;
			IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[(item.getTagCompound().getByte("s") & 0xff) % 6]);
			if (acc == null) return false;
			linkedInv = acc;
			if (acc.getSlots() - size <= 0) offset = 0;
			return true;
		}

		@Override
		public int getSlots() {
			return size;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			slot += offset;
			return slot < linkedInv.getSlots() ? linkedInv.getStackInSlot(slot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if ((slot += offset) < linkedInv.getSlots()) return linkedInv.insertItem(slot, stack, simulate);
			else return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if ((slot += offset) < linkedInv.getSlots()) return linkedInv.extractItem(slot, amount, simulate);
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return (slot += offset) < linkedInv.getSlots() ? linkedInv.getSlotLimit(slot) : 0;
		}

	}

	public static TileEntity getLink(NBTTagCompound nbt) {
		if (nbt == null) return null;
		int d = nbt.getInteger("d");
		World world = DimensionManager.getWorld(d);
		if (world == null) return null;
		return Utils.getTileAt(world, BlockPos.fromLong(nbt.getLong("pos")));
	}

	public class CapabilityProvider implements ICapabilityProvider {

		public final ItemStack holder;

		public CapabilityProvider(ItemStack holder) {
			this.holder = holder;
		}

		@Override
		public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
			if (cap != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return false;
			TileEntity te = getLink(holder.getTagCompound());
			return te != null && te.hasCapability(cap, facing);
		}

		@Override
		public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
			if (cap != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return null;
			NBTTagCompound nbt = holder.getTagCompound();
			TileEntity te = getLink(nbt);
			if (te == null) return null;
			return te.getCapability(cap, EnumFacing.VALUES[(nbt.getByte("s") & 0xff) % 6]);
		}

	}

}
