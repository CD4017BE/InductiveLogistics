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
import cd4017be.lib.util.TooltipUtil;
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

	public static TabMaterials tabIndLog = new TabMaterials(Main.ID);

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

	static void registerCapabilities() {
		CapabilityManager.INSTANCE.register(BasicWarpPipe.class, new EmptyStorage<BasicWarpPipe>(), new EmptyCallable<BasicWarpPipe>());
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			(itemPipe = BlockPipe.create("item_pipe", Material.WOOD, SoundType.WOOD, ItemPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			(fluidPipe = BlockPipe.create("fluid_pipe", Material.GLASS, SoundType.GLASS, FluidPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			(warpPipe = BlockPipe.create("warp_pipe", Material.IRON, SoundType.METAL, WarpPipe.class, 1).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(1.0F).setResistance(20F),
			(tank = VariantBlock.create("tank", Material.GLASS, SoundType.GLASS, 2, 16, Tank.class)).setCreativeTab(tabIndLog).setLightOpacity(0),
			(buffer = VariantBlock.create("buffer", Material.WOOD, SoundType.WOOD, 0, 16, Buffer.class)).setCreativeTab(tabIndLog),
			(autoCraft = new AdvancedBlock("auto_craft", Material.IRON, SoundType.ANVIL, 0, AutoCrafter.class)).setCreativeTab(tabIndLog)
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			i_itemPipe = new ItemItemPipe(itemPipe),
			i_fluidPipe = new ItemFluidPipe(fluidPipe),
			i_warpPipe = new BaseItemBlock(warpPipe),
			i_tank = new ItemTank(tank),
			i_buffer = new ItemBuffer(buffer),
			i_autoCraft = new BaseItemBlock(autoCraft),
			(fluidFilter = new ItemFluidFilter("fluid_filter")).setCreativeTab(tabIndLog),
			(itemFilter = new ItemItemFilter("item_filter")).setCreativeTab(tabIndLog),
			(portableCraft = new ItemPortableCrafter("portable_craft")).setCreativeTab(tabIndLog),
			(remoteInv = new ItemRemoteInv("remote_inv")).setCreativeTab(tabIndLog)
		);
		tabIndLog.item = new ItemStack(i_itemPipe);
	}

}
