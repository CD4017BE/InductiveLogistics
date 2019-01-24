package cd4017be.indlog.render.tesr;

import cd4017be.indlog.tileentity.Tank;
import cd4017be.lib.render.FluidRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fluids.FluidStack;

/**
 *
 * @author CD4017BE
 */
public class TankRenderer extends TileEntitySpecialRenderer<Tank> {

	@Override
	public void render(Tank te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		FluidStack fluid = te.tank.fluid;
		if (fluid != null) FluidRenderer.instance.render(fluid, te, x, y, z, 0.875D, (double)fluid.amount / (double)te.tank.cap);
	}

}
