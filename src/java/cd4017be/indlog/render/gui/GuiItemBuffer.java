package cd4017be.indlog.render.gui;

import cd4017be.indlog.Main;
import cd4017be.indlog.render.RenderItemOverride;
import cd4017be.indlog.tileentity.Buffer;
import cd4017be.indlog.util.VariableInventory.GroupAccess;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GuiItemBuffer extends AdvancedGui {

	private final Buffer tile;
	private final int N;
	private final float StepSlot, StepStack;

	public GuiItemBuffer(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (Buffer)tile;
		this.MAIN_TEX = new ResourceLocation(Main.ID, "textures/gui/buffer.png");
		this.drawBG = 6;
		this.N = this.tile.inventory.items.length;
		this.StepSlot = 1F / (float)Buffer.SLOTS[this.tile.type];
		this.StepStack = 1F / (float)Buffer.STACKS[this.tile.type];
	}

	@Override
	public void initGui() {
		int ofs = (N + 11) / 12 * 18;
		xSize = 230;
		ySize = 114 + ofs;
		super.initGui();
		tabsY = ofs + 31;
		guiComps.add(new InfoTab(0, 7, 6, 7, 8, "buffer.info"));
		guiComps.add(new Slider(1, 194, 42 + ofs, 64, 230, 0, 10, 2, false).scroll(StepSlot));
		guiComps.add(new Slider(2, 212, 42 + ofs, 64, 240, 0, 10, 2, false).scroll(StepStack));
		guiComps.add(new Slider(3, 176, 42 + ofs, 64, 230, 2, 10, 2, false).scroll(StepSlot));
		guiComps.add(new Tooltip<Object[]>(4, 194, 42 + ofs, 10, 64, "buffer.slots"));
		guiComps.add(new Tooltip<Object[]>(5, 212, 42 + ofs, 10, 64, "buffer.stack"));
		guiComps.add(new Tooltip<Integer>(6, 176, 42 + ofs, 10, 64, "buffer.start"));
		guiComps.add(new Button(7, 193, 17 + ofs, 12, 12, 0).texture(242, 29).setTooltip("buffer.side#"));
		guiComps.add(new Button(8, 175, 17 + ofs, 12, 12, 0).texture(230, 17).setTooltip("buffer.reset"));
		setEnabled(3, false);
		setEnabled(6, false);
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 1: return 1F - (float)tile.getEnd() * StepSlot;
		case 2: return 1F - (float)tile.inventory.stackSize * StepStack;
		case 3: return 1F - (float)tile.getStart() * StepSlot;
		case 4: return new Object[] {tile.getEnd(), Buffer.SLOTS[tile.type]};
		case 5: return new Object[] {tile.inventory.stackSize, Buffer.STACKS[tile.type]};
		case 6: return tile.getStart();
		case 7: return (int)tile.selSide;
		case 8: return tile.selSide >= 0 && tile.sideAccs[tile.selSide] != null ? 0 : -1;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
		switch(id) {
		case 1: {
			int v = Math.round((1F - (Float)obj) / StepSlot + 0.4F);
			v = Math.min(Math.max(v, 0), Buffer.SLOTS[tile.type]);
			if (tile.selSide < 0 || tile.selSide >= 6) {
				data.writeByte(0);
				data.writeByte(tile.inventory.slots = v);
			} else {
				data.writeByte(3);
				data.writeByte(tile.selSide);
				data.writeByte(tile.getStart());
				data.writeByte(v);
				GroupAccess acc = tile.sideAccs[tile.selSide];
				if (acc != null) acc.setRange(acc.start, v);
			}
		} break;
		case 2: {
			int v = Math.round((1F - (Float)obj) / StepStack + 0.4F);
			data.writeByte(1);
			data.writeShort(tile.inventory.stackSize = Math.min(Math.max(v, 1), Buffer.STACKS[tile.type]));
		} break;
		case 3: {
			int v = Math.round((1F - (Float)obj) / StepSlot + 0.4F);
			if (tile.selSide < 0 || tile.selSide >= 6) return;
			data.writeByte(3);
			data.writeByte(tile.selSide);
			data.writeByte(v);
			data.writeByte(tile.getEnd());
			GroupAccess acc = tile.sideAccs[tile.selSide];
			if (acc != null) acc.setRange(v, acc.start + acc.size);
		} break;
		case 7:
			if ((Integer)obj == 0) {
				if (++tile.selSide >= 6) tile.selSide = -1;
			} else if (--tile.selSide < -1) tile.selSide = 5;
			setEnabled(3, tile.selSide >= 0);
			setEnabled(6, tile.selSide >= 0);
			return;
		case 8:
			data.writeByte(2);
			data.writeByte(tile.selSide);
			break;
		}
		if (send) BlockGuiHandler.sendPacketToServer(data);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(MAIN_TEX);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 15);
		int s = tile.getStart(), e = tile.getEnd();
		drawSlots(0, s, 132);
		drawSlots(s, e, 114);
		drawSlots(e, N, 132);
		drawSlots(N, (N + 11) / 12 * 12, 150);
		drawTexturedModalRect(guiLeft, guiTop + ySize - 99, 0, 15, xSize, 99);
		if (tile.selSide >= 0)
			drawTexturedModalRect(guiLeft + 175, guiTop + ySize - 85, 230, 29, 12, 78);
		super.drawGuiContainerBackgroundLayer(t, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		super.drawGuiContainerForegroundLayer(mx, my);
		if (tile.selSide >= 0 && tile.selSide < 6)
			drawSideCube(tabsX - 64, tabsY, tile.selSide, (byte) 3);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		setEnabled(8, tile.selSide >= 0 && tile.selSide < 6 && tile.sideAccs[tile.selSide] != null);
	}

	private void drawSlots(int i0, int i1, int ty) {
		if (i1 <= i0) return;
		int w0 = i0 % 12 * 18 + 7, w1 = i1 % 12 * 18 + 7;
		int h0 = i0 / 12, h1 = i1 / 12;
		if (h0 == h1 && w0 > 7) drawTexturedModalRect(guiLeft + w0, guiTop + h0 * 18 + 15, w0, ty, w1 - w0, 18);
		else {
			if (w0 > 7) {drawTexturedModalRect(guiLeft + w0, guiTop + h0 * 18 + 15, w0, ty, xSize - w0, 18); h0++;}
			for (int h = h0; h < h1; h++) drawTexturedModalRect(guiLeft, guiTop + h * 18 + 15, 0, ty, xSize, 18);
			if (w1 > 7) drawTexturedModalRect(guiLeft, guiTop + h1 * 18 + 15, 0, ty, w1, 18);
		}
	}

	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		this.itemRender = RenderItemOverride.instance();
	}

}
