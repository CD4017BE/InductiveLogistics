package cd4017be.indlog.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItemBlock;

	
/**
 *
 * @author CD4017BE
 */
public class ItemFluidPipe extends DefaultItemBlock {

	public ItemFluidPipe(Block id) {
		super(id);
		this.setHasSubtypes(true);
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 0), "fluidPipeT");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 2), "fluidPipeE");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 1), "fluidPipeI");
	}

	@Override
	public int getMetadata(int dmg) {
		return dmg;
	}

}
