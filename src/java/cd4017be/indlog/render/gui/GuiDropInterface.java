package cd4017be.indlog.render.gui;

import cd4017be.indlog.tileentity.DropedItemInterface;
import static cd4017be.indlog.tileentity.DropedItemInterface.MAX_RANGE;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GuiDropInterface extends AdvancedGui {

	private DropedItemInterface tile;

	public GuiDropInterface(IGuiData tile, EntityPlayer player) {
		super(new DataContainer(tile, player));
		this.tile = (DropedItemInterface)tile;
		this.MAIN_TEX = new ResourceLocation("indlog:textures/gui/drop_interface.png");
	}

	@Override
	public void initGui() {
		xSize = 104;
		ySize = 76;
		super.initGui();
		guiComps.add(new NumberSel(0, 79, 33, 18, 18, "%d", 1, MAX_RANGE, 8).setTooltip("dropInterf.drop"));
		guiComps.add(new NumberSel(1, 25, 33, 18, 18, "%d", 1, MAX_RANGE, 8).setTooltip("dropInterf.front"));
		guiComps.add(new NumberSel(2, 43, 33, 18, 18, "%d", 0, MAX_RANGE, 8).setTooltip("dropInterf.right"));
		guiComps.add(new NumberSel(3, 7, 33, 18, 18, "%d", 0, MAX_RANGE, 8).setTooltip("dropInterf.left"));
		guiComps.add(new NumberSel(4, 25, 51, 18, 18, "%d", 0, MAX_RANGE, 8).setTooltip("dropInterf.down"));
		guiComps.add(new NumberSel(5, 25, 15, 18, 18, "%d", 0, MAX_RANGE, 8).setTooltip("dropInterf.up"));
		guiComps.add(new Text<Object[]>(6, 43, 15, 54, 8, "dropInterf.size", ()-> new Object[] {
				tile.settings[2] + tile.settings[3] + 1,
				tile.settings[4] + tile.settings[5] + 1,
				tile.settings[1]
		}).center().setTooltip("dropInterf.sizeI"));
	}

	@Override
	protected Object getDisplVar(int id) {
		return tile.settings[id];
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
		int v = (Integer)obj;
		if (id == 0 && v > tile.settings[1]) v = tile.settings[1];
		data.writeByte(id).writeByte(tile.settings[id] = v);
		if (send) BlockGuiHandler.sendPacketToServer(data);
	}

}
