package cd4017be.indlog.item;

import java.io.IOException;
import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterItem;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.Gui.IGuiItem;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.capability.InventoryItem.IItemInventory;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.IFilter;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public abstract class ItemFilteredSubInventory extends BaseItem implements IItemInventory, IGuiItem, ClientItemPacketReceiver {

	public ItemFilteredSubInventory(String id) {
		super(id);
		this.setMaxStackSize(1);
	}

	@Override
	public abstract ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt);

	@Override
	public ItemStack[] loadInventory(ItemStack inv, EntityPlayer player) {
		ItemStack[] items = new ItemStack[2];
		NBTTagCompound nbt;
		if ((nbt = inv.getSubCompound("fin")) != null) {
			items[0] = new ItemStack(Objects.item_filter);
			items[0].setTagCompound(nbt);
		} else items[0] = ItemStack.EMPTY;
		if ((nbt = inv.getSubCompound("fout")) != null) {
			items[1] = new ItemStack(Objects.item_filter);
			items[1].setTagCompound(nbt);
		} else items[1] = ItemStack.EMPTY;
		return items;
	}

	@Override
	public void saveInventory(ItemStack inv, EntityPlayer player, ItemStack[] items) {
		NBTTagCompound nbt;
		if (inv.hasTagCompound()) nbt = inv.getTagCompound();
		else inv.setTagCompound(nbt = new NBTTagCompound());
		ItemStack fin = items[0], fout = items[1]; items[0] = ItemStack.EMPTY; items[1] = ItemStack.EMPTY;
		if (fin.getItem() == Objects.item_filter) {
			if (!fin.hasTagCompound()) fin.setTagCompound(new NBTTagCompound());
			nbt.setTag("fin", fin.getTagCompound());
			items[0] = fin;
		} else nbt.removeTag("fin");
		if (fout.getItem() == Objects.item_filter) {
			if (!fout.hasTagCompound()) fout.setTagCompound(new NBTTagCompound());
			nbt.setTag("fout", fout.getTagCompound());
			items[1] = fout;
		} else nbt.removeTag("fout");
	}

	protected int tickTime() {
		return 20;
	}

	@Override
	public void onUpdate(ItemStack item, World world, Entity entity, int s, boolean b) {
		if (world.isRemote) return;
		if (entity instanceof EntityPlayer && item.hasTagCompound()) {
			long t = world.getTotalWorldTime();
			if ((t - (long)item.getTagCompound().getByte("t") & 0xff) >= tickTime()) {
				EntityPlayer player = (EntityPlayer)entity;
				InventoryPlayer inv = player.inventory;
				PipeFilterItem in = item.getTagCompound().hasKey("fin") ? PipeFilterItem.load(item.getTagCompound().getCompoundTag("fin")) : null;
				PipeFilterItem out = item.getTagCompound().hasKey("fout") ? PipeFilterItem.load(item.getTagCompound().getCompoundTag("fout")) : null;
				this.updateItem(item, player, inv, s, in, out);
				item.getTagCompound().setByte("t", (byte)t);
			}
		}
	}

	protected void updateItem(ItemStack item, EntityPlayer player, InventoryPlayer inv, int s, PipeFilterItem in, PipeFilterItem out) {
		IItemHandler acc = item.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		IItemHandler pacc = new PlayerMainInvWrapper(inv);
		if (acc == null) return;
		if (in != null && (in.mode & 64) != 0) {
			in.mode |= 128;
			ItemFluidUtil.transferItems(pacc, acc, in, notMe);
		}
		if (out != null && (out.mode & 64) != 0) {
			out.mode |= 128;
			ItemFluidUtil.transferItems(acc, pacc, null, out);
		}
	}

	public static final IFilter<ItemStack, IItemHandler> notMe = new IFilter<ItemStack, IItemHandler>(){
		@Override
		public int insertAmount(ItemStack obj, IItemHandler inv) {
			int n = obj.getCount();
			return n == 0 || obj.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ? 0 : n;
		}
		@Override
		public ItemStack getExtract(ItemStack obj, IItemHandler inv) {
			return obj.getCount() == 0 || obj.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ? ItemStack.EMPTY : obj;
		}
		@Override
		public boolean transfer(ItemStack obj) {
			return true;
		}
	};

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer player, ItemStack item, int slot) throws IOException {
		byte cmd = dis.readByte();
		if (cmd >= 0 && cmd < 2) {
			String name = cmd == 0 ? "fin" : "fout";
			if (item.hasTagCompound() && item.getTagCompound().hasKey(name, 10)) {
				NBTTagCompound tag = item.getTagCompound().getCompoundTag(name);
				byte m = tag.getByte("mode");
				m |= 128;
				m ^= 64;
				tag.setByte("mode", m);
				ItemGuiData.updateInventory(player, player.inventory.currentItem);
			}
		} else this.customPlayerCommand(item, player, cmd, dis);
	}

	protected void customPlayerCommand(ItemStack item, EntityPlayer player, byte cmd, PacketBuffer dis) {}

	public static boolean isFilterOn(ItemStack item, boolean in) {
		if (item.hasTagCompound() && item.getTagCompound().hasKey(in ? "fin" : "fout", 10))
			return (item.getTagCompound().getCompoundTag(in ? "fin" : "fout").getByte("mode") & 192) == 192;
		return false;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

}
