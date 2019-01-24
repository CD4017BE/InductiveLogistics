package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.api.indlog.filter.FluidFilterProvider;
import cd4017be.api.indlog.filter.PipeFilter;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IObjLink;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * 
 * @author CD4017BE
 *
 */
public abstract class FluidComp extends ConComp implements IObjLink {

	public ICapabilityProvider link;
	public PipeFilter<FluidStack, IFluidHandler> filter;

	public FluidComp(WarpPipeNode pipe, byte side) {
		super(pipe, side);
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("id") || nbt.hasKey("mode")) {
			filter = FluidFilterProvider.load(nbt);
			pipe.hasFilters |= 1 << side;
		} else pipe.hasFilters &= ~(1 << side);
	}

	@Override
	public void save(NBTTagCompound nbt) {
		if (filter != null) nbt.merge(filter.writeNBT());
	}

	/**
	 * Check if this destination still exists
	 * @return
	 */
	@Override
	public boolean isValid() {
		if (link == null) return false;
		if (((TileEntity)link).isInvalid()) this.updateLink();
		return link != null;
	}

	@Override
	public void updateLink() {
		link = pipe.tile.getTileOnSide(EnumFacing.VALUES[side]);
	}

	@Override
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item) {
		if (item.getCount() == 0) {
			if (filter != null) {
				ItemFluidUtil.dropStack(filter.getItemStack(), player);
				filter = null;
				pipe.network.reorder(this);
				pipe.hasFilters &= ~(1 << side);
				pipe.isBlocked |= 1 << side;
				return true;
			} else if(!player.isSneaking()) {
				pipe.isBlocked ^= 1 << side;
				return true;
			}
		} else if (filter == null && item.getItem() instanceof FluidFilterProvider
				&& (filter = ((FluidFilterProvider)item.getItem()).getFluidFilter(item)) != null) {
			item.grow(-1);
			player.setHeldItem(hand, item);
			pipe.network.reorder(this);
			pipe.hasFilters |= 1 << side;
			pipe.isBlocked &= ~(1 << side);
			return true;
		}
		return super.onClicked(player, hand, item);
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		if (filter != null) list.add(filter.getItemStack());
		super.dropContent(list);
	}

	/**
	 * Check if that fluid stack is allowed for low priority destinations
	 * @param fluid
	 * @return true if not
	 */
	public boolean blockFluid(FluidStack fluid) {
		return filter != null && !filter.transfer(fluid);
	}

	/**
	 * Insert a fluid stack into this destination tank
	 * @param fluid
	 * @return the result if not possible
	 */
	public FluidStack insertFluid(FluidStack fluid) {
		if ((filter != null && !filter.active(pipe.redstone)) || (pipe.isBlocked & 1 << side) != 0) return fluid;
		IFluidHandler acc = link.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side^1]);
		if (acc == null) return fluid;
		if (filter == null || filter.noEffect()) fluid.amount -= acc.fill(fluid, true);
		else {
			int n = filter.insertAmount(fluid, acc);
			if (n > 0) fluid.amount -= acc.fill(new FluidStack(fluid, n), true);
		}
		return fluid.amount <= 0 ? null : fluid;
	}

	public byte getPriority() {
		return filter == null ? 0 : filter.priority();
	}

}
