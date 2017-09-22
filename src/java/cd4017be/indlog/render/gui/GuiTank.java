package cd4017be.indlog.render.gui;

import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.indlog.tileentity.Tank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiTank extends GuiMachine {
	private static final long interval = 5000;
	private final Tank tile;
	private long clickTime = 0;

	public GuiTank(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (Tank)tile;
		this.MAIN_TEX = new ResourceLocation("indlog:textures/gui/tank.png");
	}

	@Override
	public void initGui() {
		this.xSize = 226;
		this.ySize = 98;
		super.initGui();
		this.titleX = xSize * 3 / 4;
		guiComps.add(new Button(1, 176, 62, 7, 7, 0, ()-> tile.lockType ? 1:0, (t)-> sendCommand(0)).texture(242, 0).setTooltip("tank.lock#"));
		guiComps.add(new Button(2, 202, 74, 16, 16, 0, ()-> tile.fill ? 1:0, (t)-> sendCommand(1)).texture(226, 0).setTooltip("tank.dir#"));
		guiComps.add(new Button(3, 176, 15, 7, 7, 0, ()-> confirm() ? 1:0, (t)-> {
			if(confirm()) sendCommand(2);
			else clickTime = System.currentTimeMillis();
		}).texture(249, 0).setTooltip("tank.del#"));
	}

	private boolean confirm() {
		return System.currentTimeMillis() - clickTime < interval;
	}

}
