package cd4017be.indlog.render.gui;

import cd4017be.indlog.Main;
import cd4017be.indlog.render.RenderItemOverride;
import cd4017be.indlog.tileentity.Buffer;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GuiItemBuffer extends GuiMachine {

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
		guiComps.add(new InfoTab(0, 7, 6, 7, 8, "buffer.info"));
		guiComps.add(new Slider(1, 194, 42 + ofs, 64, 230, 0, 10, 2, false).scroll(StepSlot));
		guiComps.add(new Slider(2, 212, 42 + ofs, 64, 240, 0, 10, 2, false).scroll(StepStack));
		guiComps.add(new Tooltip<Object[]>(3, 194, 42 + ofs, 10, 64, "buffer.slots"));
		guiComps.add(new Tooltip<Object[]>(4, 212, 42 + ofs, 10, 64, "buffer.stack"));
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 1: return 1F - (float)tile.inventory.slots * StepSlot;
		case 2: return 1F - (float)tile.inventory.stackSize * StepStack;
		case 3: return new Object[] {tile.inventory.slots, Buffer.SLOTS[tile.type]};
		case 4: return new Object[] {tile.inventory.stackSize, Buffer.STACKS[tile.type]};
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
		switch(id) {
		case 1: {
			int v = Math.round((1F - (Float)obj) / StepSlot + 0.5F);
			data.writeByte(0);
			data.writeByte(tile.inventory.slots = Math.min(Math.max(v, 1), Buffer.SLOTS[tile.type]));
		} break;
		case 2: {
			int v = Math.round((1F - (Float)obj) / StepStack + 0.5F);
			data.writeByte(1);
			data.writeShort(tile.inventory.stackSize = Math.min(Math.max(v, 1), Buffer.STACKS[tile.type]));
		} break;
		}
		if (send) BlockGuiHandler.sendPacketToServer(data);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(MAIN_TEX);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 15);
		drawSlots(0, tile.inventory.slots, 114);
		drawSlots(tile.inventory.slots, N, 132);
		drawSlots(N, (N + 11) / 12 * 12, 150);
		drawTexturedModalRect(guiLeft, guiTop + ySize - 99, 0, 15, xSize, 99);
		super.drawGuiContainerBackgroundLayer(t, mouseX, mouseY);
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
