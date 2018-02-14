package cd4017be.indlog;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ClientInputHandler;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.MultipartModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import cd4017be.indlog.render.gui.GuiAutoCrafter;
import cd4017be.indlog.render.gui.GuiDropInterface;
import cd4017be.indlog.render.gui.GuiFluidIO;
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
		
		BlockGuiHandler.registerGui(TANK, GuiTank.class);
		BlockGuiHandler.registerGui(BUFFER, GuiItemBuffer.class);
		BlockGuiHandler.registerGui(AUTO_CRAFT, GuiAutoCrafter.class);
		BlockGuiHandler.registerGui(FLUID_INTAKE, GuiFluidIO.class);
		BlockGuiHandler.registerGui(FLUID_OUTLET, GuiFluidIO.class);
		BlockGuiHandler.registerGui(DROP_INTERFACE, GuiDropInterface.class);
	}

	@Override
	public void registerRenderers() {
		super.registerRenderers();
		ClientRegistry.bindTileEntitySpecialRenderer(ItemPipe.class, new ItemPipeRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(FluidPipe.class, new FluidPipeRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Tank.class, new TankRenderer());
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent ev) {
		SpecialModelLoader.instance.tesrs.add(FluidRenderer.instance);
		SpecialModelLoader.setMod(Main.ID);
		SpecialModelLoader.registerBlockModel(ITEM_PIPE, new MultipartModel(ITEM_PIPE).setPipeVariants(7));
		SpecialModelLoader.registerBlockModel(FLUID_PIPE, new MultipartModel(FLUID_PIPE).setPipeVariants(7));
		SpecialModelLoader.registerBlockModel(WARP_PIPE, new MultipartModel(WARP_PIPE).setPipeVariants(13));
		SpecialModelLoader.registerBlockModel(INV_CONNECTOR, new MultipartModel(INV_CONNECTOR).setPipeVariants(3));
		
		ITEM_PIPE.setBlockLayer(BlockRenderLayer.TRANSLUCENT);
		FLUID_PIPE.setBlockLayer(BlockRenderLayer.TRANSLUCENT);
		WARP_PIPE.setBlockLayer(BlockRenderLayer.TRANSLUCENT);
		TANK.setBlockLayer(BlockRenderLayer.CUTOUT);
		INV_CONNECTOR.setBlockLayer(BlockRenderLayer.TRANSLUCENT);

		BlockItemRegistry.registerRenderBS(ITEM_PIPE, 0, 2);
		BlockItemRegistry.registerRenderBS(FLUID_PIPE, 0, 2);
		BlockItemRegistry.registerRender(WARP_PIPE);
		BlockItemRegistry.registerRenderBS(TANK, 0, 15);
		BlockItemRegistry.registerRenderBS(BUFFER, 0, 15);
		BlockItemRegistry.registerRender(AUTO_CRAFT);
		BlockItemRegistry.registerRender(INV_CONNECTOR);
		BlockItemRegistry.registerRender(TRASH);
		BlockItemRegistry.registerRender(FLUID_INTAKE);
		BlockItemRegistry.registerRender(FLUID_OUTLET);
		BlockItemRegistry.registerRender(DROP_INTERFACE);
		BlockItemRegistry.registerRender(ENTITY_INTERFACE);
		BlockItemRegistry.registerRender(fluid_filter);
		BlockItemRegistry.registerRender(item_filter);
		BlockItemRegistry.registerRender(portable_craft);
		BlockItemRegistry.registerRender(remote_inv);
	}

}
