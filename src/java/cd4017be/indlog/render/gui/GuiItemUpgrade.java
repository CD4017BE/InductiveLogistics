package cd4017be.indlog.render.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;

/**
 *
 * @author CD4017BE
 */
public class GuiItemUpgrade extends GuiMachine {

	private final InventoryPlayer inv;

	public GuiItemUpgrade(TileContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation("automation", "textures/gui/itemUpgrade.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 150;
		super.initGui();
		guiComps.add(new Button(0, 115, 33, 18, 18, 0).texture(176, 18).setTooltip("filter.invert#"));
		guiComps.add(new Button(1, 124, 15, 9, 18, 0).texture(239, 0).setTooltip("filter.try#"));
		guiComps.add(new Button(2, 133, 24, 18, 9, 0).texture(194, 0).setTooltip("filter.meta#"));
		guiComps.add(new Button(3, 151, 24, 18, 9, 0).texture(212, 0).setTooltip("filter.nbt#"));
		guiComps.add(new Button(4, 133, 33, 18, 18, 0).texture(194, 18).setTooltip("filter.ore#"));
		guiComps.add(new Button(5, 151, 33, 18, 18, 0).texture(212, 18).setTooltip("filter.targetI#"));
		guiComps.add(new Button(6, 115, 15, 9, 18, 0).texture(230, 0).setTooltip("rstCtr"));
		guiComps.add(new TextField(7, 144, 16, 24, 7, 4).setTooltip("filter.priority"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 7) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			return (int)(mode >> id & (id == 6 ? 3 : 1));
		} else if (id == 7) return item != null && item.hasTagCompound() ? "" + item.getTagCompound().getByte("prior") : "0";
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(((TileContainer)inventorySlots).data.pos());
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 7) {
			byte mode = item != null && item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
			if (id != 6) mode ^= 1 << id;
			else {
				if ((mode & 192) != 64) mode ^= 64;
				if ((mode & 64) != 0) mode ^= 128;
			}
			dos.writeByte(0);
			dos.writeByte(mode);
		} else if (id == 7) try {dos.writeByte(1); dos.writeByte(Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
		else if (id == 8) dos.writeByte(2);
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

	@Override
	public void onGuiClosed() {
		this.setDisplVar(8, null, true);
		super.onGuiClosed();
	}

}
