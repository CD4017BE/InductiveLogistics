package cd4017be.indlog;

import cd4017be.api.Capabilities.EmptyCallable;
import cd4017be.api.Capabilities.EmptyStorage;
import cd4017be.indlog.item.*;
import cd4017be.indlog.multiblock.BasicWarpPipe;
import cd4017be.indlog.tileentity.*;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.block.VariantBlock;
import cd4017be.lib.templates.TabMaterials;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class Objects {

	public static TabMaterials tabIndLog;

	@CapabilityInject(BasicWarpPipe.class)
	public static Capability<BasicWarpPipe> WARP_PIPE_CAP;

	static void registerCapabilities() {
		CapabilityManager.INSTANCE.register(BasicWarpPipe.class, new EmptyStorage<BasicWarpPipe>(), new EmptyCallable<BasicWarpPipe>());
	}

	//Blocks
	public static BlockPipe itemPipe;
	public static BlockPipe fluidPipe;
	public static BlockPipe warpPipe;
	public static VariantBlock tank;
	
	//ItemBlocks
	public static ItemItemPipe i_itemPipe;
	public static ItemFluidPipe i_fluidPipe;
	public static DefaultItemBlock i_warpPipe;
	public static ItemTank i_tank;

	static void createBlocks() {
		i_itemPipe = new ItemItemPipe((itemPipe = BlockPipe.create("item_pipe", Material.WOOD, SoundType.WOOD, ItemPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F));
		i_fluidPipe = new ItemFluidPipe((fluidPipe = BlockPipe.create("fluid_pipe", Material.GLASS, SoundType.GLASS, FluidPipe.class, 3).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F));
		i_warpPipe = new DefaultItemBlock((warpPipe = BlockPipe.create("warp_pipe", Material.IRON, SoundType.METAL, WarpPipe.class, 1).setSize(0.25)).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(1.0F).setResistance(20F));
		i_tank = new ItemTank((tank = VariantBlock.create("tank", Material.GLASS, SoundType.GLASS, 2, 16, Tank.class)).setCreativeTab(tabIndLog).setLightOpacity(0));
		
		tabIndLog.item = new ItemStack(Blocks.HOPPER); //TODO set CreativeTab item
	}

	//Items
	public static ItemItemFilter itemFilter;
	public static ItemFluidFilter fluidFilter;

	static void createItems() {
		(fluidFilter = new ItemFluidFilter("fluid_filter")).setCreativeTab(tabIndLog);
		(itemFilter = new ItemItemFilter("item_filter")).setCreativeTab(tabIndLog);
	}

}
