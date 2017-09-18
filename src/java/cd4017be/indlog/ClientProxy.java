package cd4017be.indlog;

import cd4017be.lib.ClientInputHandler;
import cd4017be.lib.render.SpecialModelLoader;

public class ClientProxy extends CommonProxy {

	@Override
	public void init() {
		super.init();
		SpecialModelLoader.setMod(Main.ID);
		ClientInputHandler.init();
	}

	@Override
	public void registerRenderers() {
		super.registerRenderers();
	}

}
