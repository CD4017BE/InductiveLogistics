package cd4017be.indlog.render.gui;

import cd4017be.indlog.Main;
import cd4017be.indlog.item.ItemFilteredSubInventory;
import cd4017be.indlog.item.ItemRemoteInv.GuiData;
import cd4017be.indlog.render.RenderItemOverride;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author CD4017BE
 */
public class GuiRemoteInventory extends AdvancedGui {

	private final GuiData data;
	private final InventoryPlayer inv;
	private Slider scroll;

	public GuiRemoteInventory(TileContainer cont) {
		super(cont);
		this.data = (GuiData)cont.data;
		this.inv = cont.player.inventory;
		this.MAIN_TEX = new ResourceLocation(Main.ID, "textures/gui/portable_remote_inv.png");
		this.drawBG = 6;
	}

	@Override
	public void initGui() {
		this.xSize = 230;
		this.ySize = 150 + data.ofsY;
		super.initGui();
		guiComps.add(new Button(0, 11, ySize - 83, 10, 18, 0).texture(230, 0).setTooltip("inputFilter"));
		guiComps.add(new Button(1, 29, ySize - 83, 10, 18, 0).texture(240, 0).setTooltip("outputFilter"));
		guiComps.add(new InfoTab(2, 7, 6, 7, 8, "remote.info"));
		int size = data.slots <= data.size ? 160 : 160 * data.size / data.slots;
		if (size < 1) size = 1;
		guiComps.add(scroll = new Slider(3, 62 + size / 2, ySize - 99, 160 - size, 1, 154, size, 4, true).scroll(12F / (data.slots - data.size)));
		guiComps.add(new Tooltip<Object[]>(4, 62, ySize - 99, 160, 4, "remote.scroll"));
		guiComps.add(new ScrollWrapper(5, 7, 16, 216, (data.size + 11) / 12 * 18, 3));
		if (data.size == 0) guiComps.add(new Text<>(6, 0, 20, xSize, 8, "remote.notFound").center());
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (data.slots <= data.size) {
			setEnabled(scroll.id, false);
		} else {
			setEnabled(scroll.id, true);
			int size = 160 * data.size / data.slots;
			if (size < 1) size = 1;
			if (scroll.tw != size)
				guiComps.set(scroll.id, scroll = new Slider(3, 62 + size / 2, ySize - 99, 160 - size, 1, 154, size, 4, true).scroll(12F / (data.slots - data.size)));
		}
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		if (id < 2) return ItemFilteredSubInventory.isFilterOn(item, id == 0) ? 1 : 0;
		else if (id == 3) return (float)data.offset / (float)(data.slots - data.size);
		else if (id == 4) return new Object[] {data.offset, data.offset + data.size, data.slots};
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		dos.writeByte(id);
		if (id == 3) dos.writeInt(data.offset = (int)((Float)obj * (float)(data.slots - data.size)));
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
		mc.renderEngine.bindTexture(MAIN_TEX);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, 15);
		int n = Math.min(data.size, data.slots);
		drawSlots(0, n, 114);
		drawSlots(n, (data.size + 11) / 12 * 12, 132);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop + 51 + data.ofsY, 0, 15, this.xSize, 99);
		if (scroll.enabled) this.drawTexturedModalRect(this.guiLeft + 61, this.guiTop + 51 + data.ofsY, 0, 150, 162, 4);
		super.drawGuiContainerBackgroundLayer(var1, var2, var3);
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

	class ScrollWrapper extends GuiComp<Object> {

		final int tid;

		public ScrollWrapper(int id, int px, int py, int w, int h, int tid) {
			super(id, px, py, w, h);
			this.tid = tid;
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 3) return guiComps.get(tid).mouseIn(x, y, b, d);
			return false;
		}

	}

}
