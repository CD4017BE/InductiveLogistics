package cd4017be.indlog.tileentity;

import java.util.ArrayList;
import java.util.List;

import cd4017be.api.automation.IFluidPipeCon;
import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeFilterFluid;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.LinkedTank;
import cd4017be.lib.util.TileAccess;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 *
 * @author CD4017BE
 */
public class FluidPipe extends BaseTileEntity implements ITilePlaceHarvest, INeighborAwareTile, IInteractiveTile, ITickable, IModularTile {

	public static int CAP = 1000;
	public static byte TICKS = 1;

	private final LinkedTank tankcap = new LinkedTank(CAP, this::getFluid, this::setFluid);
	public FluidStack tank, last;
	private PipeFilterFluid filter = null;
	private FluidPipe target = null;
	private ArrayList<TileAccess> invs = null;
	private byte type;
	/** bits[0-13 (6+1)*2]: (side + total) * dir{0:none, 1:in, 2:out, 3:lock/both} */
	private short flow;
	private boolean updateCon = true;
	private byte timer = 0;

	private FluidStack getFluid() {
		return tank;
	}

	private void setFluid(FluidStack fluid) {
		tank = fluid;
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (updateCon) this.updateConnections(type);
		if ((flow & 0x3000) == 0x3000)
			switch(type) {
			case 1:
				if (tank != null && (filter == null || filter.active(world.isBlockPowered(pos)))) transferIn();
				if (tank != null && target != null && (filter == null || filter.transfer(tank))) transfer();
				break;
			case 2:
				timer++;
				if ((filter == null || filter.active(world.isBlockPowered(pos))) && (timer & 0xff) >= TICKS) transferEx();
			default:
				if (tank != null && target != null) transfer();
			}
		if (last != tank) {
			last = tank;
			markUpdate();
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? (T)tankcap : null;
	}

	private void updateConnections(int type) {
		if (invs != null) invs.clear();
		else if (type != 0) invs = new ArrayList<TileAccess>(5);
		EnumFacing dir;
		TileEntity te;
		ArrayList<FluidPipe> updateList = new ArrayList<FluidPipe>();
		if (target != null && ((TileEntity)target).isInvalid()) target = null;
		int lHasIO = getFlowBit(6), nHasIO = 0, lDirIO, nDirIO;
		short lFlow = flow;
		for (int i = 0; i < 6; i++) {
			lDirIO = getFlowBit(i);
			if (lDirIO == 3) continue;
			dir = EnumFacing.VALUES[i];
			te = world.getTileEntity(pos.offset(dir));
			if (te != null && te instanceof FluidPipe) {
				FluidPipe pipe = (FluidPipe)te;
				int pHasIO = pipe.getFlowBit(6);
				int pDirIO = pipe.getFlowBit(i ^ 1);
				if (pDirIO == 3) nDirIO = 3;
				else if ((nDirIO = pHasIO & ~pDirIO) == 3) 
					nDirIO = lHasIO == 1 && (lDirIO & 1) == 0 ? 2 : lHasIO == 2 && (lDirIO & 2) == 0 ? 1 : 0;
				if (nDirIO == 1) {
					if (target == null) target = (FluidPipe)pipe;
					else if (target != pipe) nDirIO = 0;
				}
				setFlowBit(i, nDirIO);
				if (nDirIO != 3) nHasIO |= nDirIO;
				updateList.add(pipe);
			} else if (te instanceof IFluidPipeCon) {
				byte d = ((IFluidPipeCon)te).getFluidConnectType(i^1);
				d = d == 1 ? 2 : d == 2 ? 1 : (byte)0;
				setFlowBit(i, d);
				nHasIO |= d;
			} else if (type != 0 && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
				setFlowBit(i, type);
				nHasIO |= type;
				invs.add(new TileAccess(te, dir.getOpposite()));
			} else setFlowBit(i, 0);
		}
		setFlowBit(6, nHasIO);
		if (flow != lFlow) {
			this.markUpdate();
			for (FluidPipe pipe : updateList) pipe.updateCon = true;
		}
		updateCon = false;
	}

	private void transfer() {
		if (target.tileEntityInvalid) updateCon = true;
		else if (target.tank == null) {
			target.tank = tank;
			tank = null;
		}
	}

	private void transferIn() {
		IFluidHandler acc;
		for (TileAccess inv : invs)
			if (inv.te.isInvalid() || (acc = inv.te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, inv.side)) == null) updateCon = true;
			else if (PipeFilterFluid.isNullEq(filter)) {
				if ((tank.amount -= acc.fill(tank, true)) <= 0) {
					tank = null;
					break;
				}
			} else {
				int m = filter.insertAmount(tank, acc);
				if (m > 0 && (tank.amount -= acc.fill(new FluidStack(tank, m), true)) <= 0) {
					tank = null;
					break;
				}
			}
	}

	private void transferEx() {
		timer = 0;
		IFluidHandler acc;
		for (TileAccess inv : invs)
			if (inv.te.isInvalid() || (acc = inv.te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, inv.side)) == null) updateCon = true;
			else if (PipeFilterFluid.isNullEq(filter)) {
				if (tank == null) tank = acc.drain(tankcap.cap, true);
				else {
					int m = tankcap.cap - tank.amount;
					if (m <= 0) break;
					FluidStack fluid = acc.drain(new FluidStack(tank, m), true);
					if (fluid != null) tank.amount += fluid.amount;
				}
			} else {
				int n;
				if (tank == null) n = 0;
				else if ((n = tank.amount) >= tankcap.cap) break;
				FluidStack fluid = filter.getExtract(tank, acc);
				if (fluid == null || fluid.amount <= 0) continue;
				int m = tankcap.cap - n;
				if (m < fluid.amount) fluid.amount = m;
				fluid.amount = acc.drain(fluid, true).amount + n;
				tank = fluid;
			}
	}

	@Override
	public void neighborTileChange(BlockPos pos) {
		updateCon = true;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		updateCon = true;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		boolean canF = type != 0;
		if (player.isSneaking() && item.getCount() == 0) {
			dir = Utils.hitSide(X, Y, Z);
			int s = dir.getIndex();
			int lock = this.getFlowBit(s) == 3 ? 0 : 3;
			this.setFlowBit(s, lock);
			updateCon = true;
			this.markUpdate();
			ICapabilityProvider te = getTileOnSide(dir);
			if (te != null && te instanceof FluidPipe) {
				FluidPipe pipe = (FluidPipe)te;
				pipe.setFlowBit(s^1, lock);
				pipe.updateCon = true;
				pipe.markUpdate();
			}
			return true;
		} else if (!player.isSneaking() && item.getCount() == 0 && filter != null) {
			item = new ItemStack(Objects.fluidFilter);
			item.setTagCompound(PipeFilterFluid.save(filter));
			filter = null;
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else if (filter == null && canF && item.getItem() == Objects.fluidFilter && item.getTagCompound() != null) {
			filter = PipeUpgradeFluid.load(item.getTagCompound());
			item.grow(-1);
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	private int getFlowBit(int b) {
		return flow >> (b * 2) & 3;
	}

	private void setFlowBit(int b, int v) {
		b *= 2;
		flow = (short)(flow & ~(3 << b) | (v & 3) << b);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("flow", flow);
		if (filter != null) nbt.setTag("filter", PipeFilterFluid.save(filter));
		if (tank != null) nbt.setTag("fluid", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		flow = nbt.getShort("flow");
		if (nbt.hasKey("filter")) filter = PipeFilterFluid.load(nbt.getCompoundTag("filter"));
		if (nbt.hasKey("fluid")) tank = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluid"));
		updateCon = true;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		short nf = nbt.getShort("flow");
		byte f = nbt.getByte("filt");
		if (nbt.hasKey("fl", 10)) tank = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fl"));
		else tank = null;
		if (nf != flow || (f != -1 ^ filter != null)) {
			if (f == -1) filter = null;
			else {
				filter = new PipeFilterFluid();
				filter.mode = f;
			}
			flow = nf;
			this.markUpdate();
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("flow", flow);
		nbt.setByte("filt", filter == null ? -1 : (byte)(filter.mode & 2));
		if (last != null) nbt.setTag("fl", last.writeToNBT(new NBTTagCompound()));
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		int b = getFlowBit(m);
		EnumFacing f = EnumFacing.VALUES[m];
		ICapabilityProvider p = getTileOnSide(f);
		if (b == 3 || p == null || !p.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite())) return (T)Byte.valueOf((byte)-1);
		if (filter != null && b != 0 && !(b == 2 && p instanceof FluidPipe)) b += 2;
		return (T)Byte.valueOf((byte)b);
	}

	@Override
	public boolean isModulePresent(int m) {
		int b = getFlowBit(m);
		EnumFacing f = EnumFacing.VALUES[m];
		ICapabilityProvider p = getTileOnSide(f);
		return b != 3 && p != null && p.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite());
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (filter != null) {
			ItemStack item = new ItemStack(Objects.fluidFilter);
			item.setTagCompound(PipeFilterFluid.save(filter));
			list.add(item);
		}
		return list;
	}

}
