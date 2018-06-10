package cd4017be.indlog.util.filter;

import cd4017be.indlog.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author cd4017be
 * @param <Obj>
 * @param <Inv>
 */
public abstract class AmountFilter<Obj, Inv> extends FilterBase<Obj, Inv> {

	public int amount;
	boolean inserted;

	AmountFilter() {}

	@Override
	public boolean matches(Obj obj) {
		return true;
	}

	@Override
	public boolean noEffect() {
		return amount <= 0;
	}

	@Override
	public Item item() {
		return Objects.amount_filter;
	}

	@Override
	public boolean transfer(Obj obj) {
		return inserted || (mode & 2) == 0;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setInteger("amount", amount);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		amount = nbt.getInteger("amount");
		super.deserializeNBT(nbt);
	}

	public static class FluidFilter extends AmountFilter<FluidStack, IFluidHandler> {

		@Override
		public int insertAmount(FluidStack obj, IFluidHandler inv) {
			if (obj == null) return 0;
			int n = obj.amount;
			if (amount < n) n = amount;
			if ((mode & 2) == 0) return n;
			if (n < amount) obj = new FluidStack(obj, n);
			if (inv.fill(obj, false) >= n) {
				inserted = true;
				return n;
			}
			inserted = false;
			return 0;
		}

		@Override
		public FluidStack getExtract(FluidStack obj, IFluidHandler inv) {
			int n = obj == null ? 0 : obj.amount;
			if (n <= 0) {
				FluidStack fallback = obj;
				int max = 0;
				for (IFluidTankProperties prop : inv.getTankProperties())
					if (prop.canDrain() && (obj = prop.getContents()) != null && (obj = inv.drain(obj, false)) != null) {
						if ((n = obj.amount) >= amount)
							return new FluidStack(obj, amount);
						else if (n > max) {
							fallback = obj;
							max = n;
						}
					}
				if ((mode & 2) == 0 && max > 0)
					return fallback;
			} else if (n < amount)
				return new FluidStack(obj, amount - n);
			return null;
		}

	}

	public static class ItemFilter extends AmountFilter<ItemStack, IItemHandler> {

		@Override
		public int insertAmount(ItemStack obj, IItemHandler inv) {
			int n = obj.getCount();
			if (amount < n) n = amount;
			if ((mode & 2) == 0) return n;
			if (n < amount) obj = ItemHandlerHelper.copyStackWithSize(obj, n);
			inserted = true;
			for (int i = 0, m = inv.getSlots(); i < m; i++)
				if ((obj = inv.insertItem(i, obj, true)).getCount() <= 0)
					return n;
			inserted = false;
			return 0;
		}

		@Override
		public ItemStack getExtract(ItemStack obj, IItemHandler inv) {
			int n = obj.getCount();
			if (n <= 0) {
				ItemStack fallback = obj;
				int max = 0;
				for (int i = 0, m = inv.getSlots(); i < m; i++)
					if ((n = (obj = inv.extractItem(i, amount, true)).getCount()) == amount)
						return obj;
					else if (n > max) {
						fallback = obj;
						max = n;
					}
				if ((mode & 2) == 0 && max > 0) {
					fallback.setCount(amount);
					return fallback;
				}
			} else if (n < amount)
				return ItemHandlerHelper.copyStackWithSize(obj, amount - n);
			return ItemStack.EMPTY;
		}

	}

}
