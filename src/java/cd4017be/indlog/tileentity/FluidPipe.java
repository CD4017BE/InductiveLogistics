package cd4017be.indlog.tileentity;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.util.IFluidPipeCon;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.capability.LinkedTank;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 *
 * @author CD4017BE
 */
public class FluidPipe extends Pipe<FluidPipe, FluidStack, PipeFilterFluid, IFluidHandler> {

	public static int CAP;
	public static int TICKS;

	public FluidPipe() {}
	public FluidPipe(IBlockState state) {super(state);}

	@Override
	protected Class<FluidPipe> pipeClass() {return FluidPipe.class;}
	@Override
	protected Capability<IFluidHandler> capability() {return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;}
	@Override
	protected int resetTimer() {return TICKS;}
	@Override
	protected IFluidHandler createInv() {return new LinkedTank(CAP, this::getFluid, this::setFluid);}

	private FluidStack getFluid() {
		return content;
	}

	private void setFluid(FluidStack fluid) {
		content = fluid;
	}

	@Override
	protected byte conDir(TileEntity te, EnumFacing side) {
		return te instanceof IFluidPipeCon ? ((IFluidPipeCon)te).getFluidConnectDir(side) : 0;
	}

	@Override
	protected boolean transferOut(IFluidHandler acc) {
		if (PipeFilterFluid.isNullEq(filter)) {
			if ((content.amount -= acc.fill(content, true)) > 0) return false;
		} else {
			int m = filter.insertAmount(content, acc);
			if (m <= 0 || (content.amount -= acc.fill(new FluidStack(content, m), true)) > 0) return false;
		}
		content = null;
		return true;
	}

	@Override
	protected boolean transferIn(IFluidHandler acc) {
		if (PipeFilterFluid.isNullEq(filter)) {
			if (content == null) content = acc.drain(CAP, true);
			else {
				int m = CAP - content.amount;
				if (m <= 0) return true;
				FluidStack fluid = acc.drain(new FluidStack(content, m), true);
				if (fluid != null) content.amount += fluid.amount;
			}
		} else {
			int n;
			if (content == null) n = 0;
			else if ((n = content.amount) >= CAP) return true;
			FluidStack fluid = filter.getExtract(content, acc);
			if (fluid == null || fluid.amount <= 0) return false;
			int m = CAP - n;
			if (m < fluid.amount) fluid.amount = m;
			fluid.amount = acc.drain(fluid, true).amount + n;
			content = fluid;
		}
		return false;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (super.onActivated(player, hand, item, dir, X, Y, Z)) return true;
		if (filter != null && !player.isSneaking() && item.getCount() == 0) {
			item = new ItemStack(Objects.fluid_filter);
			item.setTagCompound(PipeFilterFluid.save(filter));
			filter = null;
			flow |= 0x8000;
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else if (filter == null && type != 0 && item.getItem() == Objects.fluid_filter && item.getTagCompound() != null) {
			filter = PipeFilterFluid.load(item.getTagCompound());
			flow &= 0x7fff;
			item.grow(-1);
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else return false;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (filter != null) nbt.setTag("filter", PipeFilterFluid.save(filter));
		if (content != null) nbt.setTag("fluid", content.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("filter")) filter = PipeFilterFluid.load(nbt.getCompoundTag("filter"));
		else filter = null;
		if (nbt.hasKey("fluid")) content = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluid"));
		else content = null;
	}

	@Override
	protected boolean onDataPacket(NBTTagCompound nbt) {
		if (nbt.hasKey("fl", 10)) content = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fl"));
		else content = null;
		byte f = nbt.getByte("filt");
		if (f == -1 ^ filter != null) return false;
		if (f == -1) filter = null;
		else {
			filter = new PipeFilterFluid();
			filter.mode = f;
		}
		return true;
	}

	@Override
	protected void getUpdatePacket(NBTTagCompound nbt) {
		nbt.setByte("filt", filter == null ? -1 : (byte)(filter.mode & 2));
		if (last != null) nbt.setTag("fl", last.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = super.dropItem(state, fortune);
		if (filter != null) {
			ItemStack item = new ItemStack(Objects.fluid_filter);
			item.setTagCompound(PipeFilterFluid.save(filter));
			list.add(item);
		}
		return list;
	}

}
