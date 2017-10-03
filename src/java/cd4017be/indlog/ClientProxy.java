package cd4017be.indlog;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ClientInputHandler;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.MultipartModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import cd4017be.indlog.render.gui.GuiItemBuffer;
import cd4017be.indlog.render.gui.GuiTank;
import cd4017be.indlog.render.tesr.FluidPipeRenderer;
import cd4017be.indlog.render.tesr.FluidRenderer;
import cd4017be.indlog.render.tesr.ItemPipeRenderer;
import cd4017be.indlog.render.tesr.TankRenderer;
import cd4017be.indlog.tileentity.FluidPipe;
import cd4017be.indlog.tileentity.ItemPipe;
import cd4017be.indlog.tileentity.Tank;

import static cd4017be.indlog.Objects.*;

public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		super.init();
		ClientInputHandler.init();
		SpecialModelLoader.setMod(Main.ID);
		SpecialModelLoader.registerBlockModel(itemPipe, new MultipartModel(itemPipe).setPipeVariants(7));
		SpecialModelLoader.registerBlockModel(fluidPipe, new MultipartModel(fluidPipe).setPipeVariants(7));
		SpecialModelLoader.registerBlockModel(warpPipe, new MultipartModel(warpPipe).setPipeVariants(13));
		
		Objects.itemPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
		Objects.fluidPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
		Objects.warpPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
		Objects.tank.setBlockLayer(BlockRenderLayer.CUTOUT);
		
		BlockGuiHandler.registerGui(tank, GuiTank.class);
		BlockGuiHandler.registerGui(buffer, GuiItemBuffer.class);
	}

	@Override
	public void registerRenderers() {
		super.registerRenderers();
		BlockItemRegistry.registerRenderBS(itemPipe, 0, 2);
		BlockItemRegistry.registerRenderBS(fluidPipe, 0, 2);
		BlockItemRegistry.registerRender(warpPipe);
		BlockItemRegistry.registerRenderBS(tank, 0, 15);
		BlockItemRegistry.registerRenderBS(buffer, 0, 15);
		
		BlockItemRegistry.registerRender(fluidFilter);
		BlockItemRegistry.registerRender(itemFilter);
		
		ClientRegistry.bindTileEntitySpecialRenderer(ItemPipe.class, new ItemPipeRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(FluidPipe.class, new FluidPipeRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Tank.class, new TankRenderer());
		SpecialModelLoader.instance.tesrs.add(FluidRenderer.instance);
	}

}
