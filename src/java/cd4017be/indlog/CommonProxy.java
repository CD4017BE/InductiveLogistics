package cd4017be.indlog;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.indlog.multiblock.FluidExtractor;
import cd4017be.indlog.multiblock.ItemExtractor;
import cd4017be.indlog.tileentity.FluidPipe;
import cd4017be.indlog.tileentity.Buffer;
import cd4017be.indlog.tileentity.ItemPipe;
import cd4017be.indlog.tileentity.Pipe;
import cd4017be.indlog.tileentity.Tank;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.Gui.TileContainer;

public class CommonProxy {

	public void init() {
		TickRegistry.register();
		setConfig();
		
		BlockGuiHandler.registerContainer(Objects.tank, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.buffer, TileContainer.class);
	}

	private void setConfig() {
		ConfigConstants cfg = new ConfigConstants(RecipeScriptContext.instance.modules.get(Main.ConfigName));
		Pipe.SAVE_PERFORMANCE = !cfg.get("pipe_fancy_content", Boolean.class, false);
		FluidPipe.CAP = (int)cfg.getNumber("fluid_pipe_cap", 1000);
		FluidPipe.TICKS = Math.max((int)cfg.getNumber("fluid_pipe_tick", 1), 1);
		ItemPipe.TICKS = Math.max((int)cfg.getNumber("item_pipe_tick", 1), 1);
		FluidExtractor.TICKS = (byte)cfg.getNumber("fluid_warp_tick", 1);
		ItemExtractor.TICKS = (byte)cfg.getNumber("item_warp_tick", 1);
		cfg.getVect("tank_caps", Tank.CAP);
		cfg.getVect("buffer_slots", Buffer.SLOTS);
		cfg.getVect("buffer_stack", Buffer.STACKS);
	}

	public void registerRenderers() {
	}

}
