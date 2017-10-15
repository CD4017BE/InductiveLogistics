package cd4017be.indlog.render.gui;

import org.lwjgl.input.Keyboard;

import cd4017be.indlog.Main;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import static cd4017be.indlog.item.ItemPortableCrafter.*;

public class GuiPortableCrafting extends AdvancedGui {

	private final InventoryPlayer inv;

	public GuiPortableCrafting(TileContainer container) {
		super(container);
		this.inv = container.player.inventory;
		this.MAIN_TEX = new ResourceLocation(Main.ID, "textures/gui/portable_crafting.png");
		this.drawBG = 3;
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 168;
		super.initGui();
		guiComps.add(new Button(0, 89, 34, 16, 16, (b)-> send(3, ((Integer)b==0 ? 1:8) * (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 64:1))));
		guiComps.add(new NumberSel(1, 106, 33, 18, 18, "%d", 0, 64, 8, ()-> (int)nbt().getByte(COUNT), (n)-> send(2, n)).setTooltip("craftAm"));
		guiComps.add(new Button(2, 124, 33, 18, 18, 0, ()-> nbt().getBoolean(ACTIVE) ? 1:0, (b)-> send(0)).texture(176, 0).setTooltip("startCraft"));
		guiComps.add(new Button(3, 142, 33, 18, 18, 0, ()-> nbt().getBoolean(AUTO) ? 1:0, (b)-> send(1)).texture(194, 0).setTooltip("autoCraft#"));
		guiComps.add(new Button(4, 88, 15, 18, 9, 0, ()-> nbt().getBoolean(DMG) ? 1:0, (b)-> send(5)).texture(212, 0).setTooltip("craftDmg#"));
		guiComps.add(new Button(5, 106, 15, 18, 9, 0, ()-> nbt().getBoolean(NBT) ? 1:0, (b)-> send(6)).texture(230, 0).setTooltip("craftNBT#"));
		guiComps.add(new InfoTab(6, 7, 6, 7, 8, "craft.info"));
		guiComps.add(new Button(7, 7, 21, 7, 7, (b)-> multiClick(b, 0, 1, 2)));
		guiComps.add(new Button(8, 7, 39, 7, 7, (b)-> multiClick(b, 3, 4, 5)));
		guiComps.add(new Button(9, 7, 56, 7, 7, (b)-> multiClick(b, 6, 7, 8)));
		guiComps.add(new Button(10, 7, 71, 7, 7, (b)-> multiClick(b, 6, 4, 2)));
		guiComps.add(new Button(11, 22, 71, 7, 7, (b)-> multiClick(b, 6, 3, 0)));
		guiComps.add(new Button(12, 39, 71, 7, 7, (b)-> multiClick(b, 7, 4, 1)));
		guiComps.add(new Button(13, 57, 71, 7, 7, (b)-> multiClick(b, 8, 5, 2)));
		guiComps.add(new Button(14, 72, 71, 7, 7, (b)-> multiClick(b, 8, 4, 0)));
		guiComps.add(new Button(15, 72, 56, 7, 7, (b)-> multiClick(b, 1, 3, 5, 7)));
		guiComps.add(new Button(16, 82, 56, 7, 7, (b)-> multiClick(b, 0, 2, 6, 8)));
		guiComps.add(new Button(17, 92, 56, 7, 7, (b)-> multiClick(b, 0, 1, 2, 3, 4, 5, 6, 7, 8)));
		guiComps.add(new Text<>(18, 88, 72, 81, 9, "\\" + TooltipUtil.translate("container.inventory")).center());
	}

	private NBTTagCompound nbt() {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		return item.hasTagCompound() ? item.getTagCompound() : new NBTTagCompound();
	}

	private void multiClick(Object click, int... slots) {
		boolean override = (Integer)click == 0;
		for (int i : slots)
			 if (override || !inventorySlots.getSlot(i).getHasStack())
				 mc.playerController.windowClick(inventorySlots.windowId, i, 0, ClickType.PICKUP, mc.player);
	}

	private void send(int... c) {
		PacketBuffer data = BlockGuiHandler.getPacketForItem(inv.currentItem);
		for (int i : c) data.writeByte(i);
		BlockGuiHandler.sendPacketToServer(data);
	}

}
