package cd4017be.indlog.multiblock;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IItemDest;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
public class ItemAccess extends ItemSource implements IItemDest {

	public ItemAccess(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		return new ItemStack(Objects.item_pipe, 1, 0);
	}

}
