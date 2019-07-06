package cd4017be.indlog.modCompat;

import cd4017be.api.indlog.filter.ItemFilterProvider;
import cd4017be.api.indlog.filter.PipeFilter;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.indlog.filter.DummyFilter;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * @author CD4017BE
 *
 */
public class FilteredItemSensor implements IBlockSensor {

	final PipeFilter<ItemStack, IItemHandler> filter;

	public FilteredItemSensor(ItemStack stack) {
		if (stack.getItem() instanceof ItemFilterProvider)
			this.filter = ((ItemFilterProvider)stack.getItem()).getItemFilter(stack);
		else this.filter = new DummyFilter<>((byte)0);
	}

	@Override
	public int readValue(BlockReference block) {
		IItemHandler inv = block.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		if (inv == null) return 0;
		int n = 0;
		for (int i = inv.getSlots() - 1; i >= 0; i--) {
			ItemStack stack = inv.getStackInSlot(i);
			int m = stack.getCount();
			if (m > 0 && filter.matches(stack))
				n += m;
		}
		return n;
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.indlog.item_filter");
	}

	@Override
	public ResourceLocation getModel() {
		return new ResourceLocation("rs_ctr:block/_sensor.item()");
	}

}
