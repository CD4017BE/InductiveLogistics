package cd4017be.indlog.render.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.indlog.tileentity.FluidIO;
import cd4017be.indlog.tileentity.FluidOutlet;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;

/**
 *
 * @author CD4017BE
 */
public class GuiFluidIO extends AdvancedGui {

	private static final char[] dirs = {'B', 'T', 'N', 'S', 'W', 'E', '!', '?'};

	private final FluidIO tile;

	public GuiFluidIO(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (FluidIO)tile;
		this.MAIN_TEX = new ResourceLocation("indlog:textures/gui/tank.png");
	}

	@Override
	public void initGui() {
		this.xSize = 226;
		this.ySize = 98;
		super.initGui();
		this.titleX = xSize * 3 / 4;
		boolean in = tile.tank.output;
		guiComps.add(new Button(1, 183, 73, 18, 18, 0, ()-> tile.mode >> 8 & 1, (b)-> sendCommand(0)).texture(226, in ? 72:36).setTooltip(in ? "intake.update":"outlet.update"));
		guiComps.add(new NumberSel(2, 201, 73, 18, 18, "%d", 0, FluidOutlet.MAX_SIZE, 8, ()-> tile.mode & 0xff, (n)-> {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			tile.mode = (tile.mode & 0x100) | (n & 0xff);
			dos.writeByte(1).writeByte(n);
			BlockGuiHandler.sendPacketToServer(dos);
		}).setTooltip("fluidIO.range"));
		guiComps.add(new Text<Object[]>(3, 0, ySize, xSize, 8, "fluidIO.pos", ()-> new Object[]{
				dirs[tile.debugI >> 24 & 7],
				dirs[tile.debugI >> 27 & 7],
				dirs[tile.debugI >> 30 & 1 | 6],
				(byte)tile.debugI,
				(byte)(tile.debugI >> 8),
				(byte)(tile.debugI >> 16)
			}).font(0xffffffff, 8).center());
		GuiTank.addTankButtons(this, 2);
	}

}
