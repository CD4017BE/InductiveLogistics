package cd4017be.indlog.render.tesr;

import cd4017be.indlog.tileentity.ItemPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;

public class ItemPipeRenderer extends TileEntitySpecialRenderer<ItemPipe> {

	private static final double scale = 0.501D;
	private final RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();

	@Override
	public void render(ItemPipe te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te.content != null) renderItem(te.content, x, y, z);
	}

	private void renderItem(ItemStack item, double x, double y, double z) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
		GlStateManager.scale(scale, scale, scale);
		renderItem.renderItem(item, ItemCameraTransforms.TransformType.FIXED);
		GlStateManager.popMatrix();
	}

}
