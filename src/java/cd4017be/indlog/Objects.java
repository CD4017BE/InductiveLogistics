package cd4017be.indlog;

import cd4017be.lib.templates.TabMaterials;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class Objects {

	public static TabMaterials tabIndLog;

	static void registerCapabilities() {
	}

	static void createBlocks() {
		tabIndLog.item = new ItemStack(Blocks.HOPPER); //TODO set CreativeTab item
	}

	static void createItems() {
	}

}
