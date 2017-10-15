package cd4017be.indlog.multiblock;

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

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipePhysics.IObjLink;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.util.ItemFluidUtil;

public class FluidComp extends ConComp implements IObjLink {

	public final BasicWarpPipe pipe;
	public ICapabilityProvider link;
	public PipeFilterFluid filter;
	
	public FluidComp(BasicWarpPipe pipe, byte side) {
		super(side);
		this.pipe = pipe;
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey("mode")) {
			filter = PipeFilterFluid.load(nbt);
			pipe.hasFilters |= 1 << side;
		} else pipe.hasFilters &= ~(1 << side);
	}

	@Override
	public void save(NBTTagCompound nbt) {
		if (filter != null) filter.save(nbt);
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
	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item, long uid) {
		if (item.getCount() == 0) {
			if (filter != null) {
				item = new ItemStack(Objects.fluid_filter);
				item.setTagCompound(PipeFilterFluid.save(filter));
				filter = null;
				ItemFluidUtil.dropStack(item, player);
				pipe.network.reorder(this);
				pipe.hasFilters &= ~(1 << side);
				pipe.isBlocked |= 1 << side;
				return true;
			} else if(!player.isSneaking()) {
				pipe.isBlocked ^= 1 << side;
				return true;
			}
		} else if (filter == null && item.getItem() == Objects.fluid_filter && item.getTagCompound() != null) {
			filter = PipeFilterFluid.load(item.getTagCompound());
			item.grow(-1);
			player.setHeldItem(hand, item);
			pipe.network.reorder(this);
			pipe.hasFilters |= 1 << side;
			pipe.isBlocked &= ~(1 << side);
			return true;
		}
		return false;
	}

	@Override
	public void dropContent(List<ItemStack> list) {
		if (filter != null) {
			ItemStack item = new ItemStack(Objects.fluid_filter);
			item.setTagCompound(PipeFilterFluid.save(filter));
			list.add(item);
		}
	}

	/**
	 * Check if that fluid stack is allowed for low priority destinations
	 * @param fluid
	 * @return true if not
	 */
	public boolean blockFluid(FluidStack fluid)
	{
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
		if (PipeFilterFluid.isNullEq(filter)) fluid.amount -= acc.fill(fluid, true);
		else {
			int n = filter.insertAmount(fluid, acc);
			if (n > 0) fluid.amount -= acc.fill(new FluidStack(fluid, n), true);
		}
		return fluid.amount <= 0 ? null : fluid;
	}

	public byte getPriority() {
		return filter == null ? 0 : filter.priority;
	}

}
