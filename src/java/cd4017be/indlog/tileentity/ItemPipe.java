package cd4017be.indlog.tileentity;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.IItemPipeCon;
import cd4017be.indlog.util.PipeFilterItem;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 *
 * @author CD4017BE
 */
public class ItemPipe extends Pipe<ItemPipe, ItemStack, PipeFilterItem, IItemHandler> implements ITilePlaceHarvest {

	public static int TICKS;

	public ItemPipe() {}
	public ItemPipe(IBlockState state) {super(state);}

	@Override
	protected Class<ItemPipe> pipeClass() {return ItemPipe.class;}
	@Override
	protected Capability<IItemHandler> capability() {return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;}
	@Override
	protected int resetTimer() {return TICKS;}
	@Override
	protected IItemHandler createInv() {return new LinkedInventory(1, 64, this::getItem, this::setItem);}

	private ItemStack getItem(int i) {
		return content == null ? ItemStack.EMPTY : content;
	}

	private void setItem(ItemStack item, int i) {
		content = item.isEmpty() ? null : item;
	}

	@Override
	protected byte conDir(TileEntity te, EnumFacing side) {
		return te instanceof IItemPipeCon ? ((IItemPipeCon)te).getItemConnectDir(side) : 0;
	}

	@Override
	protected boolean transferOut(IItemHandler acc) {
		if (PipeFilterItem.isNullEq(filter)) {
			if ((content = ItemHandlerHelper.insertItem(acc, content, false)).getCount() > 0) return false;
		} else {
			int m = filter.insertAmount(content, acc);
			if (m <= 0) return false;
			content.grow(ItemHandlerHelper.insertItem(acc, content.splitStack(m), false).getCount());
			if (content.getCount() > 0) return false;
		}
		content = null;
		return true;
	}

	@Override
	protected boolean transferIn(IItemHandler acc) {
		if (PipeFilterItem.isNullEq(filter)) {
			if (content == null) setItem(ItemFluidUtil.drain(acc, -1), 0);
			else {
				int m = content.getMaxStackSize() - content.getCount();
				if (m <= 0) return true;
				content.grow(ItemFluidUtil.drain(acc, content, m));
			}
		} else {
			int n;
			if (content == null) n = 0;
			else if ((n = content.getCount()) >= content.getMaxStackSize()) return true;
			ItemStack extr = filter.getExtract(getItem(0), acc);
			if (extr.getCount() <= 0) return false;
			int m = Math.min(extr.getMaxStackSize() - n, extr.getCount());
			extr.setCount(ItemFluidUtil.drain(acc, extr, m) + n);
			content = extr;
		}
		return false;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (super.onActivated(player, hand, item, dir, X, Y, Z)) return true;
		if (filter != null && !player.isSneaking() && item.getCount() == 0) {
			item = new ItemStack(Objects.item_filter);
			item.setTagCompound(PipeFilterItem.save(filter));
			filter = null;
			flow |= 0x8000;
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else if (filter == null && type != 0 && item.getItem() == Objects.item_filter && item.getTagCompound() != null) {
			filter = PipeFilterItem.load(item.getTagCompound());
			flow &= 0x7fff;
			item.grow(-1);
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else return false;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (filter != null) nbt.setTag("filter", PipeFilterItem.save(filter));
		if (content != null) nbt.setTag("item", content.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("filter")) filter = PipeFilterItem.load(nbt.getCompoundTag("filter"));
		else filter = null;
		if (nbt.hasKey("item")) content = new ItemStack(nbt.getCompoundTag("item"));
		else content = null;
	}

	@Override
	protected boolean onDataPacket(NBTTagCompound nbt) {
		if (nbt.hasKey("it", 10)) content = new ItemStack(nbt.getCompoundTag("it"));
		else content = null;
		byte f = nbt.getByte("filt");
		if (f == -1 ^ filter != null) return false;
		if (f == -1) filter = null;
		else {
			filter = new PipeFilterItem();
			filter.mode = f;
		}
		return true;
	}

	@Override
	protected void getUpdatePacket(NBTTagCompound nbt) {
		nbt.setByte("filt", filter == null ? -1 : (byte)(filter.mode & 2));
		if (last != null) nbt.setTag("it", last.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (content != null) list.add(content);
		if (filter != null) {
			ItemStack item = new ItemStack(Objects.item_filter);
			item.setTagCompound(PipeFilterItem.save(filter));
			list.add(item);
		}
		return list;
	}

}
