package cd4017be.indlog;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.api.rs_ctr.sensor.SensorRegistry;
import cd4017be.indlog.item.ItemNameFilter;
import cd4017be.indlog.item.ItemPortableCrafter;
import cd4017be.indlog.item.ItemRemoteInv;
import cd4017be.indlog.modCompat.FilteredFluidSensor;
import cd4017be.indlog.modCompat.FilteredItemSensor;
import cd4017be.indlog.multiblock.FluidExtractor;
import cd4017be.indlog.multiblock.FluidInjector;
import cd4017be.indlog.multiblock.ItemExtractor;
import cd4017be.indlog.multiblock.ItemInjector;
import cd4017be.indlog.tileentity.FluidPipe;
import cd4017be.indlog.tileentity.AutoCrafter;
import cd4017be.indlog.tileentity.BlockPlacer;
import cd4017be.indlog.tileentity.Buffer;
import cd4017be.indlog.tileentity.DropedItemInterface;
import cd4017be.indlog.tileentity.EntityInterface;
import cd4017be.indlog.tileentity.FluidIO;
import cd4017be.indlog.tileentity.ItemPipe;
import cd4017be.indlog.tileentity.Pipe;
import cd4017be.indlog.tileentity.Tank;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

public class CommonProxy {

	public void init() {
		TickRegistry.register();
		setConfig();
		
		BlockGuiHandler.registerContainer(Objects.TANK, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.BUFFER, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.AUTO_CRAFT, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.FLUID_INTAKE, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.FLUID_OUTLET, TileContainer.class);
		BlockGuiHandler.registerContainer(Objects.DROP_INTERFACE, DataContainer.class);
		
		//RedstoneControl compatibility
		if (Loader.isModLoaded("rs_ctr")) {
			SensorRegistry.register(FilteredItemSensor::new,
					new ItemStack(Objects.item_filter),
					new ItemStack(Objects.name_filter),
					new ItemStack(Objects.property_filter)
				);
			SensorRegistry.register(FilteredFluidSensor::new,
					new ItemStack(Objects.fluid_filter)
				);
		}
	}

	private void setConfig() {
		ConfigConstants cfg = new ConfigConstants(RecipeScriptContext.instance.modules.get(Main.ConfigName));
		Pipe.SAVE_PERFORMANCE = !cfg.get("pipe_fancy_content", Boolean.class, false);
		FluidPipe.CAP = (int)cfg.getNumber("fluid_pipe_cap", 1000);
		FluidPipe.TICKS = Math.max((int)cfg.getNumber("fluid_pipe_tick", 1), 1);
		ItemPipe.TICKS = Math.max((int)cfg.getNumber("item_pipe_tick", 1), 1);
		FluidExtractor.INTERVAL = FluidInjector.INTERVAL = (byte)cfg.getNumber("fluid_warp_tick", 4);
		ItemExtractor.INTERVAL = ItemInjector.INTERVAL = (byte)cfg.getNumber("item_warp_tick", 4);
		cfg.getVect("tank_caps", Tank.CAP);
		cfg.getVect("buffer_slots", Buffer.SLOTS);
		cfg.getVect("buffer_stack", Buffer.STACKS);
		ItemPortableCrafter.INTERVAL = (int)cfg.getNumber("portable_craft_tick", 20);
		ItemRemoteInv.INTERVAL = (int)cfg.getNumber("remote_inv_tick", 20);
		ItemRemoteInv.MAX_SLOTS = Math.max(12, (int)cfg.getNumber("remote_max_slots", 96));
		AutoCrafter.INTERVAL = Math.max((int)cfg.getNumber("auto_craft_tick", 20), 1);
		FluidIO.CAP = (int)cfg.getNumber("fluid_io_cap", 8000);
		FluidIO.MAX_SIZE = Math.min((int)cfg.getNumber("fluid_io_range", 127), 127);
		FluidIO.SEARCH_MULT = Math.max((int)cfg.getNumber("fluid_io_path", 3), 1);
		FluidIO.SPEED = (int)cfg.getNumber("fluid_io_speed", 1);
		DropedItemInterface.INTERVAL = (int)cfg.getNumber("drop_interface_tick", 50);
		DropedItemInterface.MAX_RANGE = Math.max((int)cfg.getNumber("drop_interface_range", 15), 1);
		DropedItemInterface.INV_SIZE = (int)cfg.getNumber("drop_interface_slots", 5);
		EntityInterface.INTERVAL = (int)cfg.getNumber("entity_interface_tick", 10);
		BlockPlacer.RANGE = (int)cfg.getNumber("block_placer_range", BlockPlacer.RANGE);
		ItemNameFilter.MAX_LENGTH = Math.max(1, (int)cfg.getNumber("name_filter_chars", ItemNameFilter.MAX_LENGTH));
	}

	public void registerRenderers() {
	}

}
