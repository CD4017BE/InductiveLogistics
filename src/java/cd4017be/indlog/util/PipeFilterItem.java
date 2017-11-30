package cd4017be.indlog.util;

import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Utils.ItemType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class PipeFilterItem implements PipeFilter<ItemStack, IItemHandler> {

	public ItemStack[] list = new ItemStack[0];
	public int[] ores;
	/** 1=invert; 2=force; 4=meta; 8=nbt; 16=ore; 32=count; 64=invertRS; 128=redstone */
	public byte mode;
	public byte priority;

	@Override
	public boolean active(boolean rs) {
		return (mode & 128) == 0 || (rs ^ (mode & 64) != 0);
	}

	public static PipeFilterItem load(NBTTagCompound nbt) {
		PipeFilterItem upgrade = new PipeFilterItem();
		upgrade.mode = nbt.getByte("mode");
		upgrade.priority = nbt.getByte("prior");
		if (nbt.hasKey(ItemFluidUtil.Tag_ItemList)) {
			NBTTagList list = nbt.getTagList(ItemFluidUtil.Tag_ItemList, 10);
			upgrade.list = new ItemStack[list.tagCount()];
			for (int i = 0; i < list.tagCount(); i++) {
				upgrade.list[i] = new ItemStack(list.getCompoundTagAt(i));
			}
		}
		if ((upgrade.mode & 16) != 0 &&
				!nbt.hasKey("ore", Constants.NBT.TAG_INT_ARRAY) ||
				(upgrade.ores = nbt.getIntArray("ore")).length != upgrade.list.length)
			upgrade.generateOres();
		return upgrade;
	}

	public void generateOres() {
		ores = new int[list.length];
		for (int i = 0; i < list.length; i++) {
			int[] l = OreDictionary.getOreIDs(list[i]);
			if (l.length > 0) ores[i] = l[0];
			else ores[i] = -1;
		}
	}

	public void save(NBTTagCompound nbt) {
		nbt.setByte("mode", mode);
		nbt.setByte("prior", priority);
		if (list.length > 0) {
			NBTTagList tlist = new NBTTagList();
			for (ItemStack item : list) {
				NBTTagCompound tag = new NBTTagCompound();
				item.writeToNBT(tag);
				tlist.appendTag(tag);
			}
			nbt.setTag(ItemFluidUtil.Tag_ItemList, tlist);
		}
		if (ores != null) nbt.setIntArray("ore", ores);
	}

	public static NBTTagCompound save(PipeFilterItem upgrade) {
		NBTTagCompound nbt = new NBTTagCompound();
		upgrade.save(nbt);
		return nbt;
	}

	public boolean isEmpty() {
		return list.length == 0 && (mode & 1) == 0;
	}

	public static boolean isNullEq(PipeFilterItem filter) {
		return filter == null || (filter.list.length == 0 && (filter.mode & 1) != 0);
	}

	@Override
	public int insertAmount(ItemStack obj, IItemHandler inv) {
		int m = obj.getCount();
		if (m <= 0) return 0;
		int i = getMatch(obj);
		if ((mode & 32) != 0 && i >= 0) {
			int n = list[i].getCount();
			ItemType filter = new ItemType((mode&4)!=0, (mode&8)!=0, (mode&16)!=0, list[i]);
			for (int s = 0; s < inv.getSlots(); s++) {
				ItemStack item = inv.getStackInSlot(s);
				if (filter.matches(item)) n -= item.getCount();
				if (n <= 0) return 0;
			}
			return n;
		} else return i >= 0 ^ (mode & 1) != 0 ? m : 0;
	}

	@Override
	public ItemStack getExtract(ItemStack obj, IItemHandler inv) {
		int m = obj.getCount();
		if (m > 0) {
			int i = getMatch(obj);
			if ((mode & 32) != 0 && i >= 0) {
				int n = -list[i].getCount();
				for (int s = 0; s < inv.getSlots(); s++) {
					ItemStack item = inv.getStackInSlot(s);
					if (item.getCount() <= 0) continue;
					if (matches(i, item)) n += item.getCount();
					if (n >= m) {
						n = m;
						break;
					}
				}
				return ItemHandlerHelper.copyStackWithSize(obj, n);
			} else if (i >= 0 ^ (mode & 1) != 0) return obj.copy();
			else return ItemStack.EMPTY;
		} else {
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack item = inv.getStackInSlot(i);
				if (item.getCount() > 0 && (item = getExtract(item, inv)).getCount() > 0)
					return item;
			}
			return ItemStack.EMPTY;
		}
	}

	@Override
	public boolean transfer(ItemStack stack) {
		if ((mode & 2) == 0) return true;
		else if (stack.getCount() <= 0) return false;
		return getMatch(stack) >= 0 ^ (mode & 1) == 0;
	}

	public int getMatch(ItemStack item) {
		if (item.getCount() <= 0) return -1;
		for (int i = 0; i < list.length; i++) {
			ItemStack type = list[i];
			if (item.getItem() == type.getItem() && 
				((mode & 4) == 0 || item.getItemDamage() == type.getItemDamage()) &&
				((mode & 8) == 0 || ItemStack.areItemStackTagsEqual(item, type)))
					return i;
		}
		if (ores != null)
			for (int o : OreDictionary.getOreIDs(item))
				for (int i = 0; i < ores.length; i++)
					if (ores[i] == o) return i;
		return -1;
	}

	public boolean matches(int i, ItemStack item) {
		ItemStack type = list[i];
		if (item.getItem() == type.getItem() && 
			((mode & 4) == 0 || item.getItemDamage() == type.getItemDamage()) &&
			((mode & 8) == 0 || ItemStack.areItemStackTagsEqual(item, type))) return true;
		if (ores == null) return false;
		int o = ores[i];
		if (o < 0) return false;
		for (int j : OreDictionary.getOreIDs(item))
			if (j == o) return true;
		return false;
	}

	public boolean matches(ItemStack item) {
		return getMatch(item) >= 0 ^ (mode & 1) != 0;
	}

	@Override
	public boolean blocking() {
		return (mode & 2) != 0;
	}

}
