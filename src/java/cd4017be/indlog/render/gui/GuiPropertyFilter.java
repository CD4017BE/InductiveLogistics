package cd4017be.indlog.render.gui;

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
public class GuiPropertyFilter extends AdvancedGui {

	private final InventoryPlayer inv;

	/**
	 * @param container
	 */
	public GuiPropertyFilter(DataContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation("indlog", "textures/gui/item_filter.png");
		this.bgTexY = 160;
	}

	@Override
	public void initGui() {
		this.xSize = 113;
		this.ySize = 40;
		super.initGui();
		guiComps.add(new Button(0, 43, 15, 9, 18, 0).texture(239, 36).setTooltip("filter.comp"));
		guiComps.add(new Button(1, 16, 15, 9, 18, 0).texture(239, 0).setTooltip("filter.try#"));
		guiComps.add(new Button(2, 25, 15, 18, 18, 0).texture(176, 54).setTooltip("filter.type#"));
		guiComps.add(new Button(3, 7, 15, 9, 18, 0).texture(230, 0).setTooltip("filter.rs#"));
		guiComps.add(new Button(4, 52, 24, 18, 9, 0).texture(176, 0).setTooltip("filter.abs#"));
		guiComps.add(new TextField(5, 53, 16, 52, 7, 10).setTooltip("filter.ref"));
		guiComps.add(new TextField(6, 81, 25, 24, 7, 4).setTooltip("filter.priority"));
		guiComps.add(new InfoTab(7, 7, 6, 7, 8, "filter.infoP"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 5) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			return id == 3 ? mode >> 6 & 3 : id == 2 ? mode >> 2 & 3 : mode >> id & 1;
		} else if (id == 5) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getFloat("ref") : "0.0";
		else if (id == 6) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getByte("prior") : "0";
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 5) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			if (id == 2)
				mode = (byte)(mode & ~12 | ((mode >> 2 & 3) + ((Integer)obj == 0 ? 1:2)) % 3 << 2);
			else if (id == 3) {
				if ((mode & 128) == 128) mode ^= 64;
				if ((mode & 64) == 0) mode ^= 128;
				if ((mode & 192) == 64) mode &= 3;
			} else mode ^= 1 << id;
			dos.writeByte(5);
			dos.writeByte(mode);
		} else if (id == 5) try { dos.writeByte(6); dos.writeFloat(Float.parseFloat((String)obj));} catch(NumberFormatException e) {return;}
		else if (id == 6) try {dos.writeByte(7); dos.writeByte(Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
