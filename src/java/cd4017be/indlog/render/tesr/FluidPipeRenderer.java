package cd4017be.indlog.render.tesr;

import cd4017be.indlog.tileentity.FluidPipe;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class FluidPipeRenderer extends TileEntitySpecialRenderer<FluidPipe> {

	@Override
	public void renderTileEntityAt(FluidPipe te, double x, double y, double z, float partialTicks, int destroyStage) {
		if (te.content != null) FluidRenderer.instance.render(te.content, te, x, y + 0.3755, z, 0.249, 0.249);
	}

}
