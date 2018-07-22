package cd4017be.indlog.util.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cd4017be.indlog.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
/**
 * 
 * @author cd4017be
 * @field mode &4 = use id, &8 = full match, &16 = pattern error
 * @param <Obj>
 * @param <Inv>
 */
public abstract class NameFilter<Obj, Inv> extends FilterBase<Obj, Inv> {

	public String regex;
	Pattern pattern;

	@Override
	public boolean noEffect() {
		return false;
	}

	@Override
	public Item item() {
		return Objects.name_filter;
	}

	@Override
	public boolean transfer(Obj obj) {
		return (mode & 2) == 0 || !matches(obj);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.setString("regex", regex);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		super.deserializeNBT(nbt);
		regex = nbt.getString("regex");
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			pattern = Pattern.compile(regex = Pattern.quote(regex));
			mode |= 16;
		}
	}

	public static class ItemFilter extends NameFilter<ItemStack, IItemHandler> {

		@Override
		public boolean matches(ItemStack obj) {
			String name;
			if ((mode & 4) == 0) {
				name = obj.getDisplayName();
				//remove any color and formatting codes
				for (int i = name.indexOf('\u00A7'); i >= 0; i = name.indexOf('\u00A7', i))
					name = name.substring(0, i) + (i >= name.length() - 2 ? "" : name.substring(i+2));
			} else name = obj.getItem().getRegistryName() + (obj.getHasSubtypes() ? "#" + obj.getMetadata() : "");
			Matcher m = pattern.matcher(name);
			return ((mode & 8) == 0 ? m.find() : m.matches()) ^ (mode & 1) != 0;
		}

		@Override
		public int insertAmount(ItemStack obj, IItemHandler inv) {
			return matches(obj) ? obj.getCount() : 0;
		}

		@Override
		public ItemStack getExtract(ItemStack obj, IItemHandler inv) {
			int n = obj.getCount();
			if (n > 0) return matches(obj) ? obj : ItemStack.EMPTY;
			for (int i = 0, m = inv.getSlots(); i < m; i++)
				if ((obj = inv.getStackInSlot(i)).getCount() > 0 && matches(obj))
					return obj;
			return ItemStack.EMPTY;
		}
	}

	public static class FluidFilter extends NameFilter<FluidStack, IFluidHandler> {

		@Override
		public boolean matches(FluidStack obj) {
			String name = (mode & 4) == 0 ? name = obj.getLocalizedName() : obj.getFluid().getName();
			Matcher m = pattern.matcher(name);
			return ((mode & 8) == 0 ? m.find() : m.matches()) ^ (mode & 1) != 0;
		}

		@Override
		public int insertAmount(FluidStack obj, IFluidHandler inv) {
			return matches(obj) ? obj.amount : 0;
		}

		@Override
		public FluidStack getExtract(FluidStack obj, IFluidHandler inv) {
			if (obj != null) return matches(obj) ? obj : null;
			for (IFluidTankProperties prop : inv.getTankProperties())
				if ((obj = prop.getContents()) != null && prop.canDrainFluidType(obj) && matches(obj))
					return obj;
			return null;
		}
	}
}
