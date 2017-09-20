package cd4017be.indlog.render.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.util.TooltipUtil;

/**
 *
 * @author CD4017BE
 */
public class GuiFluidFilter extends GuiMachine {

	private final InventoryPlayer inv;

	public GuiFluidFilter(TileContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation("indlog", "textures/gui/fluid_filter.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 132;
		super.initGui();
		guiComps.add(new Button(5, 7, 15, 9, 18, 0).texture(176, 0).setTooltip("filter.tryF#"));
		guiComps.add(new Button(6, 16, 15, 9, 18, 0).texture(185, 0).setTooltip("filter.invertF#"));
		guiComps.add(new Button(7, 161, 15, 8, 18, 0).texture(194, 0).setTooltip("filter.rs#"));
		guiComps.add(new TextField(8, 116, 16, 44, 7, 8).setTooltip("filter.targetF"));
		guiComps.add(new TextField(9, 136, 25, 24, 7, 4).setTooltip("filter.priority"));
		guiComps.add(new InfoTab(10, 7, 6, 7, 8, "filter.infoF"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 8) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			return id == 5 ? mode >> 1 & 1 : id == 6 ? mode & 1 : mode >> 2 & 3;
		} else if (id == 8) return item != null && item.hasTagCompound() ? TooltipUtil.formatNumber((double)item.getTagCompound().getInteger("maxAm") / 1000D, 3) : "0";
		else if (id == 9) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getByte("prior") : "0";
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 5) {dos.writeByte(id); dos.writeString(obj == null ? "" : ((Fluid)obj).getName()); send = true;}
		else if (id < 8) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			if (id != 7) mode ^= id == 5 ? 2 : 1;
			else {
				if ((mode & 8) == 8) mode ^= 4;
				if ((mode & 4) == 0) mode ^= 8;
				if ((mode & 12) == 4) mode &= 3;
			}
			dos.writeByte(5);
			dos.writeByte(mode);
		} else if (id == 8) try {dos.writeByte(6); dos.writeInt((int)(Float.parseFloat((String)obj) * 1000D));} catch(NumberFormatException e) {return;}
		else if (id == 9) try {dos.writeByte(7); dos.writeByte(Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
