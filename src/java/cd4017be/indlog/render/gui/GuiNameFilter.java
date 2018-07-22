package cd4017be.indlog.render.gui;

import cd4017be.indlog.item.ItemNameFilter;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.DataContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiNameFilter extends AdvancedGui {

	private final InventoryPlayer inv;

	public GuiNameFilter(DataContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation("indlog", "textures/gui/fluid_filter.png");
		this.bgTexY = 192;
	}

	@Override
	public void initGui() {
		this.xSize = 138;
		this.ySize = 40;
		super.initGui();
		guiComps.add(new Button(0, 16, 15, 9, 18, 0).texture(185, 0).setTooltip("filter.invertN#"));
		guiComps.add(new Button(1, 7, 15, 9, 18, 0).texture(176, 0).setTooltip("filter.tryN#"));
		guiComps.add(new Button(2, 25, 24, 18, 9, 0).texture(202, 0).setTooltip("filter.textSrc#"));
		guiComps.add(new Button(3, 43, 24, 18, 9, 0).texture(220, 0).setTooltip("filter.fullTxt#"));
		guiComps.add(new Button(4, 123, 15, 8, 18, 0).texture(194, 0).setTooltip("filter.rs#"));
		guiComps.add(new TextField(5, 26, 16, 96, 7, ItemNameFilter.MAX_LENGTH).setTooltip("filter.regex"));
		guiComps.add(new TextField(6, 98, 25, 24, 7, 4).setTooltip("filter.priority"));
		guiComps.add(new InfoTab(7, 7, 6, 7, 8, "filter.infoN"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 5) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			return id < 4 ? mode >> id & 1 : mode >> 6 & 3;
		} else if (id == 5) return item != null && item.hasTagCompound() ? item.getTagCompound().getString("regex") : "";
		else if (id == 6) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getByte("prior") : "0";
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 5) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			if (id < 4) mode ^= 1 << id;
			else {
				if ((mode & 128) == 128) mode ^= 64;
				if ((mode & 64) == 0) mode ^= 128;
				if ((mode & 192) == 64) mode &= 3;
			}
			dos.writeByte(5);
			dos.writeByte(mode);
		} else if (id == 5) { dos.writeByte(6); dos.writeString((String)obj);}
		else if (id == 6) try {dos.writeByte(7); dos.writeByte(Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
