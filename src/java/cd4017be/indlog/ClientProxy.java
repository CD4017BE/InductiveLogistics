package cd4017be.indlog;

import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ClientInputHandler;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.MultipartModel;
import net.minecraft.util.BlockRenderLayer;

import static cd4017be.indlog.Objects.*;

public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		super.init();
		ClientInputHandler.init();
		SpecialModelLoader.setMod(Main.ID);
		SpecialModelLoader.registerBlockModel(itemPipe, new MultipartModel(itemPipe).setPipeVariants(5));
		SpecialModelLoader.registerBlockModel(fluidPipe, new MultipartModel(fluidPipe).setPipeVariants(5));
		SpecialModelLoader.registerBlockModel(warpPipe, new MultipartModel(warpPipe).setPipeVariants(9));
		
		Objects.itemPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
		Objects.fluidPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
		Objects.warpPipe.setBlockLayer(BlockRenderLayer.CUTOUT);
	}

	@Override
	public void registerRenderers() {
		super.registerRenderers();
		BlockItemRegistry.registerRenderBS(itemPipe, 0, 2);
		BlockItemRegistry.registerRenderBS(fluidPipe, 0, 2);
		BlockItemRegistry.registerRender(warpPipe);
		
		BlockItemRegistry.registerRender(fluidFilter);
		BlockItemRegistry.registerRender(itemFilter);
		
	}

}
