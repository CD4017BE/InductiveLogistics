package cd4017be.indlog.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.item.BaseItemBlock;

/**
 *
 * @author CD4017BE
 */
public class ItemItemPipe extends BaseItemBlock {

	public ItemItemPipe(Block id) {
		super(id);
		this.setHasSubtypes(true);
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 0), "itemPipeT");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 1), "itemPipeI");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 2), "itemPipeE");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 3), "itemPipeD");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 4), "itemPipeS");
	}

	@Override
	public int getMetadata(int dmg) {
		return dmg;
	}

}
