package cd4017be.indlog.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

import cd4017be.indlog.render.gui.GuiFluidFilter;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.IGuiItem;
import cd4017be.lib.Gui.ITankContainer;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
public class ItemFluidFilter extends BaseItem implements IGuiItem, ClientItemPacketReceiver {

	public ItemFluidFilter(String id) {
		super(id);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		if (item.hasTagCompound()) {
			String[] states = TooltipUtil.translate("gui.cd4017be.filter.state").split(",");
			PipeFilterFluid filter = PipeFilterFluid.load(item.getTagCompound());
			String s;
			if (states.length >= 8) {
				s = states[(filter.mode & 1) == 0 ? 0 : 1];
				if ((filter.mode & 2) != 0) s += states[5];
				if ((filter.mode & 4) != 0) s += states[(filter.mode & 8) != 0 ? 7 : 6];
			} else s = "<invalid lang entry!>";
			list.add(s);
			for (Fluid stack : filter.list) list.add("> " + stack.getLocalizedName(new FluidStack(stack, 0)));
			if (filter.maxAmount != 0) list.add(TooltipUtil.format("gui.cd4017be.filter.stock", TooltipUtil.formatNumber((double)filter.maxAmount / 1000D, 3)));
			if (filter.priority != 0) list.add(TooltipUtil.format("gui.cd4017be.priority", filter.priority));
		}
		super.addInformation(item, player, list, b);
	}

	public Fluid[] getFluids(NBTTagCompound nbt) {
		if (nbt == null || !nbt.hasKey(ItemFluidUtil.Tag_FluidList, 9)) return null;
		NBTTagList list = nbt.getTagList(ItemFluidUtil.Tag_FluidList, 8);
		Fluid[] fluids = new Fluid[list.tagCount()];
		for (int i = 0; i < fluids.length; i++)
			fluids[i] = FluidRegistry.getFluid(list.getStringTagAt(i));
		return fluids;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		BlockGuiHandler.openItemGui(player, hand);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new TileContainer(new GuiData(), player);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiFluidFilter(new TileContainer(new GuiData(), player));
	}

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer player, ItemStack item, int slot) throws IOException {
		NBTTagCompound nbt;
		if (item.hasTagCompound()) nbt = item.getTagCompound();
		else item.setTagCompound(nbt = new NBTTagCompound());
		byte cmd = dis.readByte();
		switch(cmd) {
		case 5: nbt.setByte("mode", dis.readByte()); return;
		case 6: nbt.setInteger("maxAm", dis.readInt()); return;
		case 7: nbt.setByte("prior", dis.readByte()); return;
		default: if (cmd < 0 || cmd >= 5) return;
			String name = dis.readString(32);
			NBTTagList list;
			if (nbt.hasKey(ItemFluidUtil.Tag_FluidList, 9)) list = nbt.getTagList(ItemFluidUtil.Tag_FluidList, 8);
			else nbt.setTag(ItemFluidUtil.Tag_FluidList, list = new NBTTagList());
			if (!name.isEmpty()) {
				if (cmd < list.tagCount()) list.set(cmd, new NBTTagString(name));
				else list.appendTag(new NBTTagString(name));
			} else if (cmd < list.tagCount()) list.removeTag(cmd);
		}
	}

	class GuiData extends ItemGuiData implements ITankContainer {

		private InventoryPlayer player;
		public GuiData() {super(ItemFluidFilter.this);}

		@Override
		public void initContainer(DataContainer container) {
			TileContainer cont = (TileContainer)container;
			for (int i = 0; i < getTanks(); i++)
				cont.addTankSlot(new TankSlot(this, i, 26 + 18 * i, 16, (byte)0x11));
			cont.addPlayerInventory(8, 50, false, true);
			player = cont.player.inventory;
		}

		@Override
		public int getTanks() {return 5;}

		@Override
		public FluidStack getTank(int i) {
			ItemStack item = player.mainInventory.get(player.currentItem);
			Fluid[] fluids = item != null ? getFluids(item.getTagCompound()) : null;
			return fluids != null && i < fluids.length ? new FluidStack(fluids[i], 0) : null;
		}

		@Override
		public int getCapacity(int i) {return 0;}

		@Override
		public void setTank(int i, FluidStack fluid) {}

	}

}
