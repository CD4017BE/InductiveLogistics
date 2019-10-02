package cd4017be.indlog.filter;

import static cd4017be.lib.util.ItemFluidUtil.listTanks;
import cd4017be.api.indlog.filter.FilterBase;
import cd4017be.indlog.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;

/**
 * 
 * @author cd4017be
 * @field mode &12={0:durability, 4:energy, 8:fluid}, &16:percentage
 */
public class ItemPropertyFilter extends FilterBase<ItemStack, IItemHandler> {

	public float reference;

	@Override
	public boolean matches(ItemStack obj) {
		boolean frac = (mode&16) != 0;
		float val;
		switch(mode&12) {
		case 0:
			if (frac) val = 1F - (float)obj.getItemDamage() / (float)obj.getMaxDamage();
			else val = obj.getMaxDamage() - obj.getItemDamage();
			break;
		case 4: {
			IEnergyStorage acc = obj.getCapability(CapabilityEnergy.ENERGY, null);
			val = acc == null ? 0 : acc.getEnergyStored();
			if (frac) val /= acc == null ? 0 : acc.getMaxEnergyStored();
		} break;
		case 8: {
			IFluidHandlerItem acc = obj.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
			int am = 0, cap = 0;
			if (acc != null)
				for (IFluidTankProperties prop : listTanks(acc)) {
					FluidStack stack = prop.getContents();
					if (stack != null) am += stack.amount;
					cap += prop.getCapacity();
				}
			val = am;
			if (frac) val /= cap;
		} break;
		default: return true;
		}
		return val > reference ^ (mode & 1) != 0;
	}

	@Override
	public boolean noEffect() {
		return false;
	}

	@Override
	public Item item() {
		return Objects.property_filter;
	}

	@Override
	public int insertAmount(ItemStack obj, IItemHandler inv) {
		return matches(obj) ? obj.getCount() : 0;
	}

	@Override
	public ItemStack getExtract(ItemStack obj, IItemHandler inv) {
		if (obj.getCount() > 0)
			return matches(obj) ? obj : ItemStack.EMPTY;
		for (int i = 0, n = inv.getSlots(); i < n; i++)
			if ((obj = inv.getStackInSlot(i)).getCount() > 0 && matches(obj))
				return obj;
		return ItemStack.EMPTY;
	}

	@Override
	public boolean transfer(ItemStack obj) {
		return (mode & 2) == 0 || !matches(obj);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setFloat("ref", reference);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		reference = nbt.getFloat("ref");
		super.deserializeNBT(nbt);
	}

}
