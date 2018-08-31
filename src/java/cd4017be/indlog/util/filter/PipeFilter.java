package cd4017be.indlog.util.filter;

import cd4017be.lib.util.IFilter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * 
 * @author CD4017BE
 * @param <Obj> Thing to filter
 * @param <Inv> Inventory that provides Obj
 */
public interface PipeFilter<Obj, Inv> extends IFilter<Obj, Inv>, INBTSerializable<NBTTagCompound> {

	/**
	 * @param obj resource to check
	 * @return whether this filter generally accepts the given resource
	 */
	boolean matches(Obj obj);

	/**
	 * @param rs current redstone signal
	 * @return whether external interaction is enabled
	 */
	boolean active(boolean rs);

	/**
	 * @return whether this filter might block on default routing (return false at {@link IFilter#transfer(Obj)})
	 */
	boolean blocking();

	/**
	 * @return whether this filter (when active) would act the same as if there was no filter at all
	 */
	boolean noEffect();

	/**
	 * @return connector priority (for Warp Pipes)
	 */
	byte priority();

	/**
	 * @return the Item providing this filter
	 */
	Item item();

	/**
	 * @return an ItemStack representing this filter
	 */
	default ItemStack getItemStack() {
		ItemStack stack = new ItemStack(item());
		stack.setTagCompound(serializeNBT());
		return stack;
	}

	/**
	 * @return the NBT data representing this filter (must have the string-tag 'id' set to the item registry name)
	 */
	default NBTTagCompound writeNBT() {
		NBTTagCompound nbt = serializeNBT();
		ResourceLocation id = item().getRegistryName();
		if (id != null) nbt.setString("id", id.toString());
		return nbt;
	}

}
