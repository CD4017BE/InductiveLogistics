package cd4017be.indlog.render.tesr;

import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GL11;

import cd4017be.indlog.Main;
import cd4017be.lib.render.IModeledTESR;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.IntArrayModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class FluidRenderer implements IModeledTESR {

	private static final String name = "fluid_block";
	public static final FluidRenderer instance = new FluidRenderer();
	private IntArrayModel baseModel = new IntArrayModel(0);
	private HashMap<Fluid, IntArrayModel> fluidModels = new HashMap<Fluid, IntArrayModel>();
	private HashMap<Fluid, Integer> fluidColors = new HashMap<Fluid, Integer>();

	public IntArrayModel getFor(Fluid fluid) {
		IntArrayModel m = fluidModels.get(fluid);
		if (m != null) return m;
		ResourceLocation res;
		if ((res = fluid.getStill()) == null && (res = fluid.getFlowing()) == null) return null;
		TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(res.toString());
		if (tex == null) return null;
		fluidModels.put(fluid, m = baseModel.withTexture(tex));
		return m;
	}

	public int fluidColor(Fluid fluid) {
		Integer c = fluidColors.get(fluid);
		if (c != null) return c;
		int fc = fluid.getColor();
		ResourceLocation res;
		if ((res = fluid.getStill()) == null && (res = fluid.getFlowing()) == null) return fc;
		TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(res.toString());
		if (tex == null) return fc;
		int r = 0, g = 0, b = 0, n = 0;
		for (int i = 0; i < tex.getFrameCount(); i++)
			for (int[] arr : tex.getFrameTextureData(i)) {
				for (int k : arr) {
					int a = k >> 24 & 0xff;
					n += a;
					r += (k >> 16 & 0xff) * a;
					g += (k >> 8 & 0xff) * a;
					b += (k & 0xff) * a;
				}
			}
		r = r / n * (fc >> 16 & 0xff) / 255 & 0xff;
		g = g / n * (fc >> 8 & 0xff) / 255 & 0xff;
		b = b / n * (fc & 0xff) / 255 & 0xff;
		fc = r << 16 | g << 8 | b;
		fluidColors.put(fluid, fc);
		return fc;
	}

	@Override
	public void bakeModels(IResourceManager manager) {
		try {
			baseModel = SpecialModelLoader.loadTESRModel(Main.ID, name);
			fluidModels.clear();
			fluidColors.clear();
		} catch (Exception e) {
			FMLLog.log("InductiveAutomation", Level.ERROR, e, "failed to load fluid model: ", name);
		}
	}

	public static int RGBtoBGR(int c) {
		return c & 0xff00ff00 | c << 16 & 0xff0000 | c >> 16 & 0xff;
	}

	public void render(FluidStack stack, TileEntity te, double x, double y, double z, double dxz, double dy) {
		Fluid fluid = stack.getFluid();
		IntArrayModel m = getFor(fluid);
		GlStateManager.disableLighting();
		GlStateManager.Profile.TRANSPARENT_MODEL.apply();
		m.setColor(RGBtoBGR(fluid.getColor(stack)));
		m.setBrightness(te.getWorld().getCombinedLight(te.getPos(), fluid.getLuminosity(stack)));
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.scale(dxz, dy, dxz);
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, IntArrayModel.FORMAT);
		t.addVertexData(m.vertexData);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
	}

}
