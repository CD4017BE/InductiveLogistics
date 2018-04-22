package cd4017be.indlog.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.item.BaseItemBlock;

	
/**
 *
 * @author CD4017BE
 */
public class ItemFluidPipe extends BaseItemBlock {

	public ItemFluidPipe(Block id) {
		super(id);
		this.setHasSubtypes(true);
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 0), "fluidPipeT");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 1), "fluidPipeI");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 2), "fluidPipeE");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 3), "fluidPipeD");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 4), "fluidPipeS");
	}

	@Override
	public int getMetadata(int dmg) {
		return dmg;
	}

}
