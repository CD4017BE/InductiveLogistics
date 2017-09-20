package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class ItemExtractor extends ItemComp implements ITickable {

	public static byte TICKS;

	private byte timer = 0;
	private int slotIdx;

	public ItemExtractor(BasicWarpPipe pipe, byte side) {
		super(pipe, side);
		slotIdx = 0;
	}

	@Override
	public void update() {
		if ((++timer & 0xff) < TICKS || !isValid()) return;
		IItemHandler acc = link.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[this.side^1]);
		if (acc == null) return;
		int s, target = -1, m = acc.getSlots();
		ItemStack stack = ItemStack.EMPTY;
		for (int i = slotIdx; i < slotIdx + m; i++) {
			stack = acc.extractItem(s = i % m, 65536, true);
			if (stack.getCount() > 0 && (filter == null || (stack = filter.getExtract(stack, acc)).getCount() > 0)) {
				target = s;
				slotIdx = (i + 1) % m;
				break;
			}
		}
		m = stack.getCount();
		if (target < 0 || m == 0) return;
		m -= pipe.network.insertItem(stack, filter == null || (filter.mode & 2) == 0 ? Byte.MAX_VALUE : filter.priority).getCount();
		if (m > 0) acc.extractItem(target, m, false);
	}

	@Override
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item, long uid) {
		if (super.onClicked(player, hand, item, uid)) return true;
		if (player.isSneaking() && player.getHeldItemMainhand().isEmpty()) {
			ItemFluidUtil.dropStack(new ItemStack(Objects.itemPipe, 1, 2), player);
			pipe.network.remConnector(pipe, side);
			return true;
		}
		return false;
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		list.add(new ItemStack(Objects.itemPipe, 1, 2));
		super.dropContent(list);
	}

}
