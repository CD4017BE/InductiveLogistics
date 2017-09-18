package cd4017be.indlog.multiblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipePhysics.IItemDest;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.util.ItemFluidUtil;

public class ItemDestination extends ItemComp implements IItemDest {

	public ItemDestination(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item, long uid) {
		if (super.onClicked(player, hand, item, uid)) return true;
		if (player.getHeldItemMainhand() == null && player.isSneaking()) {
			ItemFluidUtil.dropStack(new ItemStack(Objects.itemPipe, 1, 1), player);
			pipe.con[side] = 0;
			pipe.network.remConnector(pipe, side);
			pipe.updateCon = true;
			pipe.hasFilters &= ~(1 << side);
			((BaseTileEntity)pipe.tile).markUpdate();
			return true;
		}
		return false;
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		list.add(new ItemStack(Objects.itemPipe, 1, 1));
		super.dropContent(list);
	}

}
