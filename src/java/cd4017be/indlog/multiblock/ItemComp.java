package cd4017be.indlog.multiblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

import cd4017be.indlog.multiblock.WarpPipeNetwork.IObjLink;
import cd4017be.indlog.util.filter.ItemFilterProvider;
import cd4017be.indlog.util.filter.PipeFilter;
import cd4017be.lib.util.ItemFluidUtil;

/**
 * 
 * @author CD4017BE
 *
 */
public abstract class ItemComp extends ConComp implements IObjLink {

	public ICapabilityProvider link;
	public PipeFilter<ItemStack, IItemHandler> filter;

	public ItemComp(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("id") || nbt.hasKey("mode")) {
			filter = ItemFilterProvider.load(nbt);
			pipe.hasFilters |= 1 << side;
		} else pipe.hasFilters &= ~(1 << side);
	}

	@Override
	public void save(NBTTagCompound nbt) {
		if (filter != null) nbt.merge(filter.writeNBT());
	}

	/**
	 * Check if this destination still exists
	 * @return
	 */
	@Override
	public boolean isValid() {
		if (link == null) return false;
		if (((TileEntity)link).isInvalid()) this.updateLink();
		return link != null;
	}

	@Override
	public void updateLink() {
		link = pipe.tile.getTileOnSide(EnumFacing.VALUES[side]);
	}

	@Override
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item) {
		if (item.getCount() == 0) {
			if (filter != null) {
				ItemFluidUtil.dropStack(filter.getItemStack(), player);
				filter = null;
				pipe.network.reorder(this);
				pipe.hasFilters &= ~(1 << side);
				pipe.isBlocked |= 1 << side;
				return true;
			} else if(!player.isSneaking()) {
				pipe.isBlocked ^= 1 << side;
				return true;
			}
		} else if (filter == null && item.getItem() instanceof ItemFilterProvider
				&& (filter = ((ItemFilterProvider)item.getItem()).getItemFilter(item)) != null) {
			item.grow(-1);
			player.setHeldItem(hand, item);
			pipe.network.reorder(this);
			pipe.hasFilters |= 1 << side;
			pipe.isBlocked &= ~(1 << side);
			return true;
		}
		return super.onClicked(player, hand, item);
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		if (filter != null) list.add(filter.getItemStack());
		super.dropContent(list);
	}

	/**
	 * Check if that item stack is allowed for low priority destinations
	 * @param item
	 * @return true if not
	 */
	public boolean blockItem(ItemStack item) {
		return filter != null && !filter.transfer(item);
	}

	/**
	 * Insert an item stack into this destination inventory
	 * @param item
	 * @return the result if not possible
	 */
	public ItemStack insertItem(ItemStack item) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return item;
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return item;
		int n = item.getCount();
		if (filter == null || filter.noEffect()) return ItemHandlerHelper.insertItemStacked(acc, item, false);
		n = filter.insertAmount(item, acc);
		if (n == 0) return item;
		if (n > item.getCount()) n = item.getCount();
		item.grow(ItemHandlerHelper.insertItemStacked(acc, item.splitStack(n), false).getCount());
		return item;
	}

	public byte getPriority() {
		return filter == null ? 0 : filter.priority();
	}

}
