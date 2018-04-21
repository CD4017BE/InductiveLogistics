package cd4017be.indlog.multiblock;

import cd4017be.indlog.multiblock.WarpPipePhysics.IItemDest;
import net.minecraft.item.ItemStack;


/**
 * @author CD4017BE
 *
 */
public class ItemAccess extends ItemSource implements IItemDest {

	public ItemAccess(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	protected ItemStack moduleItem() {
		// TODO Auto-generated method stub
		return super.moduleItem();
	}

}
