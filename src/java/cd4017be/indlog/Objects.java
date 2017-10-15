package cd4017be.indlog;

import cd4017be.api.Capabilities.EmptyCallable;
import cd4017be.api.Capabilities.EmptyStorage;
import cd4017be.indlog.item.*;
import cd4017be.indlog.multiblock.BasicWarpPipe;
import cd4017be.indlog.tileentity.*;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.block.VariantBlock;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.templates.TabMaterials;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 
 * @author CD4017BE
 */
@EventBusSubscriber(modid = Main.ID)
public class Objects {

	public static TabMaterials tabIndLog;

	@CapabilityInject(BasicWarpPipe.class)
	public static Capability<BasicWarpPipe> WARP_PIPE_CAP;

	//Blocks
	public static BlockPipe itemPipe;
	public static BlockPipe fluidPipe;
	public static BlockPipe warpPipe;
	public static VariantBlock tank;
	public static VariantBlock buffer;
	public static AdvancedBlock autoCraft;
	
	//ItemBlocks
	public static ItemItemPipe i_itemPipe;
	public static ItemFluidPipe i_fluidPipe;
	public static BaseItemBlock i_warpPipe;
	public static ItemTank i_tank;
	public static ItemBuffer i_buffer;
	public static BaseItemBlock i_autoCraft;

	//Items
	public static ItemItemFilter itemFilter;
	public static ItemFluidFilter fluidFilter;
	public static ItemPortableCrafter portableCraft;
	public static ItemRemoteInv remoteInv;

	static void init() {
		//Blocks
		i_itemPipe = new ItemItemPipe((itemPipe = BlockPipe.create("item_pipe", Material.WOOD, SoundType.WOOD, ItemPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F));
		i_fluidPipe = new ItemFluidPipe((fluidPipe = BlockPipe.create("fluid_pipe", Material.GLASS, SoundType.GLASS, FluidPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F));
		i_warpPipe = new BaseItemBlock((warpPipe = BlockPipe.create("warp_pipe", Material.IRON, SoundType.METAL, WarpPipe.class, 1).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(1.0F).setResistance(20F));
		i_tank = new ItemTank((tank = VariantBlock.create("tank", Material.GLASS, SoundType.GLASS, 2, 16, Tank.class)).setCreativeTab(tabIndLog).setLightOpacity(0));
		i_buffer = new ItemBuffer((buffer = VariantBlock.create("buffer", Material.WOOD, SoundType.WOOD, 0, 16, Buffer.class)).setCreativeTab(tabIndLog));
		i_autoCraft = new BaseItemBlock((autoCraft = new AdvancedBlock("auto_craft", Material.IRON, SoundType.ANVIL, 0, AutoCrafter.class)).setCreativeTab(tabIndLog));
		//Items
		(fluidFilter = new ItemFluidFilter("fluid_filter")).setCreativeTab(tabIndLog);
		(itemFilter = new ItemItemFilter("item_filter")).setCreativeTab(tabIndLog);
		(portableCraft = new ItemPortableCrafter("portable_craft")).setCreativeTab(tabIndLog);
		(remoteInv = new ItemRemoteInv("remote_inv")).setCreativeTab(tabIndLog);
		//Creative Tab
		tabIndLog = new TabMaterials(Main.ID);
		tabIndLog.item = new ItemStack(i_itemPipe);
	}

	static void registerCapabilities() {
		CapabilityManager.INSTANCE.register(BasicWarpPipe.class, new EmptyStorage<BasicWarpPipe>(), new EmptyCallable<BasicWarpPipe>());
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		ev.getRegistry().registerAll(
			itemPipe, fluidPipe, warpPipe, tank, buffer, autoCraft
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().registerAll(
			i_itemPipe, i_fluidPipe, i_warpPipe, i_tank, i_buffer, i_autoCraft,
			fluidFilter, itemFilter, portableCraft, remoteInv
		);
	}

}
