package cd4017be.indlog.render.gui;

import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.indlog.tileentity.Tank;
import cd4017be.indlog.util.AdvancedTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiTank extends AdvancedGui {
	private static final long interval = 5000;
	private final Tank tile;
	private static long clickTime = 0;

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
		guiComps.add(new Button(1, 201, 73, 18, 18, 0, ()-> tile.tank.output ? 1:0, (t)-> sendCommand(2)).texture(226, 0).setTooltip("tank.dir#"));
		guiComps.add(new Button(2, 176, 84, 7, 7, 0, ()-> tile.auto ? 1:0, (t)-> sendCommand(3)).texture(249, 28).setTooltip("tank.auto#"));
		addTankButtons(this, 0);
	}

	private static boolean confirm() {
		return System.currentTimeMillis() - clickTime < interval;
	}

	public static void addTankButtons(AdvancedGui gui, int cmd) {
		int i = gui.guiComps.size();
		TileContainer cont = (TileContainer)gui.inventorySlots;
		for (TankSlot tank : cont.tankSlots) {
			gui.guiComps.add(gui.new Button(i++, 176, 15, 7, 7, 0, ()-> confirm() ? 1:0, (t)-> {
			if(confirm()) {
				gui.sendCommand(cmd + tank.tankNumber * 2);
				clickTime = 0;
			} else clickTime = System.currentTimeMillis();
		}).texture(249, 14).setTooltip("tank.del#"));
			if (tank.inventory instanceof AdvancedTank) {
				AdvancedTank at = (AdvancedTank)tank.inventory;
				gui.guiComps.add(gui.new Button(i++, tank.xPos, tank.yPos, 7, 7, 0, ()-> at.lock ? 1:0, (t)-> gui.sendCommand(cmd + tank.tankNumber * 2 + 1)).texture(249, 0).setTooltip("tank.lock#"));
			}
		}
	}

}
