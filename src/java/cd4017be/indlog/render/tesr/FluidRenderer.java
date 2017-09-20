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
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLLog;

public class FluidRenderer implements IModeledTESR {

	private static final String name = "fluid_block";
	public static final FluidRenderer instance = new FluidRenderer();
	private IntArrayModel baseModel; 
	private HashMap<Fluid, IntArrayModel> fluidModels;

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

	@Override
	public void bakeModels(IResourceManager manager) {
		try {
			baseModel = SpecialModelLoader.loadTESRModel(Main.ID, name);
			fluidModels.clear();
		} catch (Exception e) {
			FMLLog.log("InductiveAutomation", Level.ERROR, e, "failed to load fluid model: ", name);
		}
	}

	public void render(FluidStack stack, TileEntity te, double x, double y, double z, double dxz, double dy) {
		Fluid fluid = stack.getFluid();
		IntArrayModel m = getFor(fluid);
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.depthMask(false);
		GlStateManager.color(1, 1, 1, 1);
		m.setColor(fluid.getColor(stack));
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
