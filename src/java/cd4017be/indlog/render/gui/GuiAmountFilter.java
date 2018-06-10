package cd4017be.indlog.render.gui;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.DataContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author cd4017be
 */
public class GuiAmountFilter extends AdvancedGui {

	private final InventoryPlayer inv;

	public GuiAmountFilter(DataContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation("indlog", "textures/gui/fluid_filter.png");//TODO use diff tex
	}

	@Override
	public void initGui() {
		this.xSize = 176; //TODO diff dims
		this.ySize = 132;
		super.initGui();
		guiComps.add(new Button(0, 7, 15, 9, 18, 0).texture(176, 0).setTooltip("filter.tryA#"));
		guiComps.add(new Button(1, 161, 15, 8, 18, 0).texture(194, 0).setTooltip("filter.rs#"));
		guiComps.add(new TextField(2, 116, 16, 44, 7, 8).setTooltip("filter.amount"));
		guiComps.add(new TextField(3, 136, 25, 24, 7, 4).setTooltip("filter.priority"));
		guiComps.add(new InfoTab(4, 7, 6, 7, 8, "filter.infoA"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 2) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			return id == 0 ? mode >> 1 & 1 : mode >> 6 & 3;
		} else if (id == 2) return item != null && item.hasTagCompound() ? "" + (double)item.getTagCompound().getInteger("amount") : "0";
		else if (id == 3) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getByte("prior") : "0";
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 2) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			if (id == 0) mode ^= 2;
			else {
				if ((mode & 128) == 128) mode ^= 64;
				if ((mode & 64) == 0) mode ^= 128;
				if ((mode & 192) == 64) mode &= 3;
			}
			dos.writeByte(5);
			dos.writeByte(mode);
		} else if (id == 2) try {dos.writeByte(6); dos.writeInt((int)Double.parseDouble((String)obj));} catch(NumberFormatException e) {return;}
		else if (id == 3) try {dos.writeByte(7); dos.writeByte(Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
