package cd4017be.indlog;

import cd4017be.api.Capabilities.EmptyCallable;
import cd4017be.api.Capabilities.EmptyStorage;
import cd4017be.indlog.item.*;
import cd4017be.indlog.multiblock.WarpPipeNode;
import cd4017be.indlog.tileentity.*;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.block.BlockCoveredPipe;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.block.VariantBlock;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.property.PropertyOrientation;
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

	@CapabilityInject(WarpPipeNode.class)
	public static Capability<WarpPipeNode> WARP_PIPE_CAP;

	//Blocks
	public static final BlockCoveredPipe ITEM_PIPE = null;
	public static final BlockCoveredPipe FLUID_PIPE = null;
	public static final BlockCoveredPipe WARP_PIPE = null;
	public static final VariantBlock TANK = null;
	public static final VariantBlock BUFFER = null;
	public static final AdvancedBlock AUTO_CRAFT = null;
	public static final BlockCoveredPipe INV_CONNECTOR = null;
	public static final OrientedBlock TRASH = null;
	public static final OrientedBlock FLUID_INTAKE = null;
	public static final OrientedBlock FLUID_OUTLET = null;
	public static final OrientedBlock DROP_INTERFACE = null;
	public static final OrientedBlock ENTITY_INTERFACE = null;
	public static final OrientedBlock BLOCK_PLACER = null;

	//ItemBlocks
	public static final ItemItemPipe item_pipe = null;
	public static final ItemFluidPipe fluid_pipe = null;
	public static final BaseItemBlock warp_pipe = null;
	public static final ItemTank tank = null;
	public static final ItemBuffer buffer = null;
	public static final BaseItemBlock auto_craft = null;
	public static final BaseItemBlock inv_connector = null;
	public static final BaseItemBlock trash = null;
	public static final BaseItemBlock fluid_intake = null;
	public static final BaseItemBlock fluid_outlet = null;
	public static final BaseItemBlock drop_interface = null;
	public static final BaseItemBlock entity_interface = null;
	public static final BaseItemBlock block_placer = null;

	//Items
	public static final ItemItemFilter item_filter = null;
	public static final ItemFluidFilter fluid_filter = null;
	public static final ItemAmountFilter amount_filter = null;
	public static final ItemPortableCrafter portable_craft = null;
	public static final ItemRemoteInv remote_inv = null;

	static void init() {
		CapabilityManager.INSTANCE.register(WarpPipeNode.class, new EmptyStorage<WarpPipeNode>(), new EmptyCallable<WarpPipeNode>());
		tabIndLog.item = new ItemStack(ITEM_PIPE);
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		TooltipUtil.CURRENT_DOMAIN = Main.ID;
		ev.getRegistry().registerAll(
			BlockCoveredPipe.create("item_pipe", Material.WOOD, SoundType.WOOD, ItemPipe.class, 5).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			BlockCoveredPipe.create("fluid_pipe", Material.GLASS, SoundType.GLASS, FluidPipe.class, 5).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(0.5F),
			BlockCoveredPipe.create("warp_pipe", Material.IRON, SoundType.METAL, WarpPipe.class, 1).setSize(0.25).setCreativeTab(tabIndLog).setLightOpacity(0).setHardness(1.0F).setResistance(20F),
			VariantBlock.create("tank", Material.GLASS, SoundType.GLASS, 2, 16, Tank.class).setCreativeTab(tabIndLog).setLightOpacity(0),
			VariantBlock.create("buffer", Material.WOOD, SoundType.WOOD, 0, 16, Buffer.class).setCreativeTab(tabIndLog),
			new AdvancedBlock("auto_craft", Material.IRON, SoundType.ANVIL, 0, AutoCrafter.class).setCreativeTab(tabIndLog),
			BlockCoveredPipe.create("inv_connector", Material.GLASS, SoundType.GLASS, InvConnector.class, 1).setSize(0.375).setLightOpacity(0).setCreativeTab(tabIndLog).setHardness(0.5F),
			OrientedBlock.create("trash", Material.ROCK, SoundType.STONE, 0, OverflowTrash.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog),
			OrientedBlock.create("fluid_intake", Material.IRON, SoundType.METAL, 0, FluidIntake.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog),
			OrientedBlock.create("fluid_outlet", Material.IRON, SoundType.METAL, 0, FluidOutlet.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog),
			OrientedBlock.create("drop_interface", Material.WOOD, SoundType.WOOD, 0, DropedItemInterface.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog),
			OrientedBlock.create("entity_interface", Material.WOOD, SoundType.WOOD, 0, EntityInterface.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog),
			OrientedBlock.create("block_placer", Material.IRON, SoundType.METAL, 0, BlockPlacer.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabIndLog)
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
			new BaseItemBlock(INV_CONNECTOR),
			new BaseItemBlock(TRASH),
			new BaseItemBlock(FLUID_INTAKE),
			new BaseItemBlock(FLUID_OUTLET),
			new BaseItemBlock(DROP_INTERFACE),
			new BaseItemBlock(ENTITY_INTERFACE),
			new BaseItemBlock(BLOCK_PLACER),
			new ItemFluidFilter("fluid_filter").setCreativeTab(tabIndLog),
			new ItemItemFilter("item_filter").setCreativeTab(tabIndLog),
			new ItemItemFilter("amount_filter").setCreativeTab(tabIndLog),
			new ItemPortableCrafter("portable_craft").setCreativeTab(tabIndLog),
			new ItemRemoteInv("remote_inv").setCreativeTab(tabIndLog)
		);
	}

}
