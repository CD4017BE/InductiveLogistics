package cd4017be.indlog.multiblock;

import net.minecraft.item.ItemStack;
import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IItemDest;

/**
 * 
 * @author CD4017BE
 *
 */
public class ItemDestination extends ItemComp implements IItemDest {

	public ItemDestination(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 3);
	}

}
