package cd4017be.indlog.render.gui;

import cd4017be.indlog.Main;
import cd4017be.indlog.tileentity.AutoCrafter;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class GuiAutoCrafter extends GuiMachine {

	private final AutoCrafter tile;

	public GuiAutoCrafter(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (AutoCrafter)tile;
		this.MAIN_TEX = new ResourceLocation(Main.ID, "textures/gui/auto_craft.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 168;
		super.initGui();
		this.tabsY = -52;
		for (int j = 0; j < 3; j++)
			for (int i = 0; i < 3; i++)
				guiComps.add(new Button(i + j * 3, i * 18 + 19, j * 18 + 18, 12, 12, 1).texture(176, 12).setTooltip("craft.in"));
		guiComps.add(new Button(9, 82, 54, 12, 12, 2).texture(176, 12).setTooltip("craft.out"));
		guiComps.add(new NumberSel(10, 124, 33, 18, 18, "%d", 0, 64, 8).setTooltip("craftAm"));
		guiComps.add(new Button(11, 142, 33, 18, 18, 0, ()-> tile.rsMode & 1, (b)-> sendCommand(11)).texture(188, 0).setTooltip("craft.rsIn#"));
		guiComps.add(new Button(12, 79, 15, 18, 18, 0, ()-> tile.rsMode >> 1 & 1, (b)-> sendCommand(12)).texture(206, 0).setTooltip("craft.rsOut#"));
		guiComps.add(new InfoTab(13, 7, 8, 7, 9, "craft.infoB"));
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 10) {
			int b = tile.grid[id];
			return b < 0 || b >= 6 ? b : EnumFacing.VALUES[b];
		} else if (id == 10) return tile.amount;
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id < 10) {
			data.writeByte(id);
			int b = tile.grid[id];
			b = (b + ((Integer)obj == 0 ? 2 : 7)) % 7 - 1;
			tile.grid[id] = (byte)b;
			data.writeByte(b);
		} else if (id == 10) {
			data.writeByte(10);
			data.writeByte(tile.amount = (Integer)obj);
		} else return;
		if (send) BlockGuiHandler.sendPacketToServer(data);
	}

}
