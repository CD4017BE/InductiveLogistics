package cd4017be.indlog.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author cd4017be
 */
@SideOnly(Side.CLIENT)
public class RenderItemOverride extends RenderItem {

	public int stacksizeWidth = 16;
	private RenderItem original;
	private static RenderItemOverride instance;
	
	public static RenderItemOverride instance() {
		if (instance == null) instance = new RenderItemOverride(Minecraft.getMinecraft());
		instance.original = Minecraft.getMinecraft().getRenderItem();
		return instance;
	}

	private RenderItemOverride(Minecraft mc) {
		super(mc.renderEngine, mc.getRenderItem().getItemModelMesher().getModelManager(), mc.getItemColors());
	}

	@Override
	public void renderItemOverlayIntoGUI(FontRenderer fr, ItemStack stack, int x, int y, String text) {
		int n = stack.getCount();
		if (n > stack.getMaxStackSize() || text != null) {
			String s = text != null ? text : n == Integer.MAX_VALUE ? "§90" : String.valueOf(n);
			int l = fr.getStringWidth(s);
			if (l > stacksizeWidth) {
				float scale = (float)stacksizeWidth / (float)l;
				GlStateManager.pushMatrix();
				GlStateManager.translate(x + 17, y + 6 + fr.FONT_HEIGHT, 0);
				GlStateManager.scale(scale, scale, 1);
				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				GlStateManager.disableBlend();
				fr.drawStringWithShadow(s, (float)-l, (float)(3 - fr.FONT_HEIGHT), 16777215);
				GlStateManager.enableLighting();
				GlStateManager.enableDepth();
				GlStateManager.enableBlend();
				GlStateManager.popMatrix();
				stack = ItemHandlerHelper.copyStackWithSize(stack, 1);
				text = null;
			} else text = s;
		}
		super.renderItemOverlayIntoGUI(fr, stack, x, y, text);
	}

	@Override
	public IBakedModel getItemModelWithOverrides(ItemStack stack, World worldIn, EntityLivingBase entitylivingbaseIn) {
		return original.getItemModelWithOverrides(stack, worldIn, entitylivingbaseIn);
	}

}
