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
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

/**
 * 
 * @author CD4017BE
 */
@EventBusSubscriber(modid = Main.ID)
@ObjectHolder(value = Main.ID)
public class Objects {

	public static TabMaterials tabIndLog = new TabMaterials(Main.ID);

	@CapabilityInject(BasicWarpPipe.class)
	public static Capability<BasicWarpPipe> WARP_PIPE_CAP;

	//Blocks
	public static final BlockPipe ITEM_PIPE = null;
	public static final BlockPipe FLUID_PIPE = null;
	public static final BlockPipe WARP_PIPE = null;
	public static final VariantBlock TANK = null;
	public static final VariantBlock BUFFER = null;
	public static final AdvancedBlock AUTO_CRAFT = null;

	//ItemBlocks
	public static final ItemItemPipe item_pipe = null;
	public static final ItemFluidPipe fluid_pipe = null;
	public static final BaseItemBlock warp_pipe = null;
	public static final ItemTank tank = null;
	public static final ItemBuffer buffer = null;
	public static final BaseItemBlock auto_craft = null;

	//Items
	public static final ItemItemFilter item_filter = null;
	public static final ItemFluidFilter fluid_filter = null;
	public static final ItemPortableCrafter portable_craft = null;
	public static final ItemRemoteInv remote_inv = null;

	static void init() {
		CapabilityManager.INSTANCE.register(BasicWarpPipe.class, new EmptyStorage<BasicWarpPipe>(), new EmptyCallable<BasicWarpPipe>());
		tabIndLog.item = new ItemStack(ITEM_PIPE);
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			BlockPipe.create("item_pipe", Material.WOOD, SoundType.WOOD, ItemPipe.class, 3).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			BlockPipe.create("fluid_pipe", Material.GLASS, SoundType.GLASS, FluidPipe.class, 3).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			BlockPipe.create("warp_pipe", Material.IRON, SoundType.METAL, WarpPipe.class, 1).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(1.0F).setResistance(20F),
			VariantBlock.create("tank", Material.GLASS, SoundType.GLASS, 2, 16, Tank.class).setCreativeTab(tabIndLog).setLightOpacity(0),
			VariantBlock.create("buffer", Material.WOOD, SoundType.WOOD, 0, 16, Buffer.class).setCreativeTab(tabIndLog),
			new AdvancedBlock("auto_craft", Material.IRON, SoundType.ANVIL, 0, AutoCrafter.class).setCreativeTab(tabIndLog)
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			new ItemItemPipe(ITEM_PIPE),
			new ItemFluidPipe(FLUID_PIPE),
			new BaseItemBlock(WARP_PIPE),
			new ItemTank(TANK),
			new ItemBuffer(BUFFER),
			new BaseItemBlock(AUTO_CRAFT),
			new ItemFluidFilter("fluid_filter").setCreativeTab(tabIndLog),
			new ItemItemFilter("item_filter").setCreativeTab(tabIndLog),
			new ItemPortableCrafter("portable_craft").setCreativeTab(tabIndLog),
			new ItemRemoteInv("remote_inv").setCreativeTab(tabIndLog)
		);
	}

}
