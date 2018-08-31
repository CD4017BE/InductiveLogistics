package cd4017be.indlog.item;

import java.io.IOException;
import java.util.function.ToIntFunction;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.filter.ItemFilterProvider;
import cd4017be.indlog.util.filter.PipeFilter;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
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
		items[0] = loadFilter(inv.getSubCompound("fin"));
		items[1] = loadFilter(inv.getSubCompound("fout"));
		return items;
	}

	@Override
	public void saveInventory(ItemStack inv, EntityPlayer player, ItemStack[] items) {
		NBTTagCompound nbt;
		if (inv.hasTagCompound()) nbt = inv.getTagCompound();
		else inv.setTagCompound(nbt = new NBTTagCompound());
		ItemStack fin = items[0], fout = items[1]; items[0] = ItemStack.EMPTY; items[1] = ItemStack.EMPTY;
		NBTTagCompound tag;
		if ((tag = saveFilter(fin)) != null) {
			nbt.setTag("fin", tag);
			items[0] = fin;
		} else nbt.removeTag("fin");
		if ((tag = saveFilter(fout)) != null) {
			nbt.setTag("fout", tag);
			items[1] = fout;
		} else nbt.removeTag("fout");
	}

	public static ItemStack loadFilter(NBTTagCompound nbt) {
		if (nbt == null) return ItemStack.EMPTY;
		Item i = Item.getByNameOrId(nbt.getString("id"));
		if (i == null) i = Objects.item_filter;
		ItemStack stack = new ItemStack(i);
		stack.setTagCompound(nbt);
		return stack;
	}

	public static NBTTagCompound saveFilter(ItemStack item) {
		Item i = item.getItem();
		if (i instanceof ItemFilterProvider) {
			NBTTagCompound nbt = item.getTagCompound();
			if (nbt == null) nbt = new NBTTagCompound();
			ResourceLocation loc = i.getRegistryName();
			if (loc != null) nbt.setString("id", loc.toString());
			return nbt;
		} else return null;
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
				PipeFilter<ItemStack, IItemHandler> in = item.getTagCompound().hasKey("fin") ? ItemFilterProvider.load(item.getTagCompound().getCompoundTag("fin")) : null;
				PipeFilter<ItemStack, IItemHandler> out = item.getTagCompound().hasKey("fout") ? ItemFilterProvider.load(item.getTagCompound().getCompoundTag("fout")) : null;
				this.updateItem(item, player, inv, s, in, out);
				item.getTagCompound().setByte("t", (byte)t);
			}
		}
	}

	protected void updateItem(ItemStack item, EntityPlayer player, InventoryPlayer inv, int s, PipeFilter<ItemStack, IItemHandler> in, PipeFilter<ItemStack, IItemHandler> out) {
		IItemHandler acc = item.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		IItemHandler pacc = new PlayerMainInvWrapper(inv);
		if (acc == null) return;
		if (in != null && in.active(false)) ItemFluidUtil.transferItems(pacc, acc, in, notMe);
		if (out != null && out.active(false)) ItemFluidUtil.transferItems(acc, pacc, null, out);
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

	public static final ToIntFunction<ItemStack> FILTER_SLOT = (item) -> item.getItem() instanceof ItemFilterProvider ? 1 : 0;

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
