package cd4017be.indlog.item;

import java.io.IOException;
import java.util.List;

import cd4017be.indlog.render.gui.GuiNameFilter;
import cd4017be.indlog.util.filter.FluidFilterProvider;
import cd4017be.indlog.util.filter.ItemFilterProvider;
import cd4017be.indlog.util.filter.NameFilter.FluidFilter;
import cd4017be.indlog.util.filter.NameFilter.ItemFilter;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.IGuiItem;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * @author CD4017BE
 *
 */
public class ItemNameFilter extends BaseItem implements IGuiItem, ClientItemPacketReceiver, ItemFilterProvider, FluidFilterProvider {

	public static int MAX_LENGTH = 20;

	public ItemNameFilter(String id) {
		super(id);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		if (item.hasTagCompound()) {
			String[] states = TooltipUtil.translate("gui.cd4017be.filter.state").split(",");
			FluidFilter filter = getFluidFilter(item);
			String s;
			if (states.length >= 11) {
				s = states[(filter.mode & 1) == 0 ? 0 : 1];
				if ((filter.mode & 8) != 0) s += states[8];
				s += states[(filter.mode & 4) == 0 ? 9 : 10];
				if ((filter.mode & 2) != 0) s += states[5];
				if ((filter.mode & 64) != 0) s += states[(filter.mode & 128) != 0 ? 7 : 6];
			} else s = "<invalid lang entry!>";
			list.add(s);
			if (!filter.regex.isEmpty()) list.add(TooltipUtil.format("gui.cd4017be.pattern", filter.regex));
			if (filter.priority != 0) list.add(TooltipUtil.format("gui.cd4017be.priority", filter.priority));
		}
		super.addInformation(item, player, list, b);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		BlockGuiHandler.openItemGui(player, hand);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new DataContainer(new ItemGuiData(this), player);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiNameFilter(new DataContainer(new ItemGuiData(this), player));
	}

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer player, ItemStack item, int slot) throws IOException {
		NBTTagCompound nbt;
		if (item.hasTagCompound()) nbt = item.getTagCompound();
		else item.setTagCompound(nbt = new NBTTagCompound());
		byte cmd = dis.readByte();
		switch(cmd) {
		case 5: nbt.setByte("mode", dis.readByte()); return;
		case 6: 
			nbt.setString("regex", dis.readString(MAX_LENGTH));
			nbt.setByte("mode", (byte)(nbt.getByte("mode") & ~16));
			return;
		case 7: nbt.setByte("prior", dis.readByte()); return;
		}
	}

	@Override
	public FluidFilter getFluidFilter(ItemStack stack) {
		FluidFilter filter = new FluidFilter();
		if (stack != null && stack.hasTagCompound()) filter.deserializeNBT(stack.getTagCompound());
		return filter;
	}

	@Override
	public ItemFilter getItemFilter(ItemStack stack) {
		ItemFilter filter = new ItemFilter();
		if (stack != null && stack.hasTagCompound()) filter.deserializeNBT(stack.getTagCompound());
		return filter;
	}

}
