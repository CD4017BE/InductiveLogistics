package cd4017be.indlog.tileentity;

import static cd4017be.lib.property.PropertyByte.cast;

import java.util.ArrayList;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.TileAccess;
import cd4017be.lib.util.Utils;
import cd4017be.indlog.util.PipeFilter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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

/**
 *
 * @author CD4017BE
 */
public abstract class Pipe<T extends Pipe<T, O, F, I>, O, F extends PipeFilter<O, I>, I> extends BaseTileEntity implements INeighborAwareTile, IInteractiveTile, IModularTile, ITickable {

	public static boolean SAVE_PERFORMANCE;

	protected final I inventory = createInv();
	public O content, last;
	protected T target;
	protected F filter;
	protected ArrayList<TileAccess> invs = null;
	protected byte type, dest;
	/** bits[0-13 (6+1)*2]: (side + total) * dir{0:none, 1:out, 2:in, 3:lock/both}, bit[14]: update, bit[15]: blocked */
	protected short flow;
	private byte time;
	protected boolean updateCon = true;

	public Pipe() {}

	public Pipe(IBlockState state) {
		super(state);
		type = (byte)blockType.getMetaFromState(state);
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (updateCon) updateConnections();
		if (world.getTotalWorldTime() % resetTimer() == time) {
			if ((flow & 0x3000) == 0x3000) {
				switch(type) {
				case 1:
					if ((flow & 0x8000) != 0) break;
					if (content != null && (filter == null || filter.active(world.isBlockPowered(pos)))) {
						I acc;
						for (TileAccess inv : invs)
							if (inv.te.isInvalid() || (acc = inv.te.getCapability(capability(), inv.side)) == null) updateCon = true;
							else if (transferOut(acc)) break;
					}
					if (content != null && target != null && target.content == null && (filter == null || filter.transfer(content))) {
						if (target.tileEntityInvalid) target = null;
						else {
							target.content = content;
							content = null;
						}
					}
					break;
				case 2:
					if ((flow & 0x8000) == 0 && (filter == null || filter.active(world.isBlockPowered(pos)))) {
						I acc;
						for (TileAccess inv : invs)
							if (inv.te.isInvalid() || (acc = inv.te.getCapability(capability(), inv.side)) == null) updateCon = true;
							else if (transferIn(acc)) break;
					}
				default:
					if (content != null && target != null && target.content == null) {
						if (target.tileEntityInvalid) target = null;
						else {
							target.content = content;
							content = null;
						}
					}
				}
			}
		} else if (SAVE_PERFORMANCE) return;
		if (content != last) {
			last = content;
			markUpdate();
		}
	}

	private boolean unloadedNeighbor() {
		int i = pos.getX() & 15, j = pos.getZ() & 15;
		return (i == 0 && !world.isBlockLoaded(pos.west())) ||
			(i == 15 && !world.isBlockLoaded(pos.east())) ||
			(j == 0 && !world.isBlockLoaded(pos.north())) ||
			(j == 15 && !world.isBlockLoaded(pos.south()));
	}

	protected void updateConnections() {
		updateCon = false;
		if (invs != null) invs.clear();
		else if (type != 0) invs = new ArrayList<TileAccess>(5);
		if (target != null && ((TileEntity)target).isInvalid()) {
			target = null;
			dest = -1;
		}
		if (unloadedNeighbor()) return;
		
		EnumFacing dir;
		TileEntity te;
		ArrayList<T> updateList = new ArrayList<T>();
		/** -1: fine, 0: best match, 1: any match, 2: no match */
		int newDest = target == null || target.getFlowBit(dest^1) != 2 || (target.getFlowBit(6) & 1) == 0 ? 2 : -1;
		int lHasIO = getFlowBit(6), nHasIO = 0, lDirIO, nDirIO;
		short lFlow = flow;
		for (int i = 0; i < 6; i++) {
			lDirIO = getFlowBit(i);
			if (lDirIO == 3) continue;
			dir = EnumFacing.VALUES[i];
			te = world.getTileEntity(pos.offset(dir));
			if (te == null) setFlowBit(i, 0);
			else if (pipeClass().isInstance(te)) {
				T pipe = pipeClass().cast(te);
				if (newDest < 0 && lDirIO == 1 && dest == i) {
					nHasIO |= 1;
					updateList.add(pipe);
					continue;
				}
				int pHasIO = pipe.getFlowBit(6);
				int pDirIO = pipe.getFlowBit(i ^ 1);
				if (pDirIO == 3) setFlowBit(i, 3);
				else {
					nDirIO = (~lHasIO | lDirIO) & ~pDirIO & pHasIO;
					if (newDest <= 0 || !(newDest > 1 || pDirIO == 2 || (pHasIO & 2) == 0)) nDirIO &= 2;
					else nDirIO &= 3;
					if (nDirIO == 3) nDirIO = 0;
					if (nDirIO == 0) {
						if (pDirIO != 1 && pHasIO == 1) nDirIO = 1;							
						else if (pDirIO != 2 && pHasIO == 2) nDirIO = 2;
					}
					if (nDirIO == 1) {
						target = pipe;
						if (dest >= 0 && getFlowBit(dest) == 1) setFlowBit(dest, 0);
						dest = (byte)i;
						newDest = pDirIO == 2 ? 0 : 1;
					} else if (dest == i) {
						target = null;
						dest = -1;
						updateCon = true;
					}
					setFlowBit(i, nDirIO);
					nHasIO |= nDirIO;
					updateList.add(pipe);
				}
			} else if (type != 0 && te.hasCapability(capability(), dir.getOpposite())) {
				setFlowBit(i, type);
				nHasIO |= type;
				invs.add(new TileAccess(te, dir.getOpposite()));
			} else {
				byte d = conDir(te, dir.getOpposite());
				if (d == 1 && newDest >= 0) {
					if (dest >= 0 && getFlowBit(dest) == 1) setFlowBit(dest, 0);
					dest = -1;
					target = null;
					newDest = -1;
				} else if (d == 3) d = 0;
				setFlowBit(i, d);
				nHasIO |= d;
			}
		}
		setFlowBit(6, nHasIO);
		flow &= 0xbfff;
		if (flow != lFlow) {
			this.markUpdate();
			for (T pipe : updateList) pipe.updateCon = true;
		}
	}

	protected int getFlowBit(int b) {
		return flow >> (b * 2) & 3;
	}

	protected void setFlowBit(int b, int v) {
		b *= 2;
		flow = (short)(flow & ~(3 << b) | (v & 3) << b);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (item.getCount() > 0) return false;
		if (player.isSneaking()) {
			dir = Utils.hitSide(X, Y, Z);
			int s = dir.getIndex();
			int lock = getFlowBit(s) == 3 ? 0 : 3;
			setFlowBit(s, lock);
			if (lock != 0) flow |= 0x4000;
			updateCon = true;
			this.markUpdate();
			ICapabilityProvider te = getTileOnSide(dir);
			if (pipeClass().isInstance(te)) {
				T pipe = pipeClass().cast(te);
				pipe.setFlowBit(s^1, lock);
				if (lock != 0) pipe.flow |= 0x4000;
				pipe.updateCon = true;
				pipe.markUpdate();
			}
			return true;
		} else if (filter == null) {
			flow ^= 0x8000;
			markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		updateCon = true;
	}

	@Override
	public void neighborTileChange(BlockPos src) {
		updateCon = true;
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		if ((posIn.getX() + posIn.getY() + posIn.getZ() & 1) == 0) time = 0;
		else time = (byte) Math.min(127, resetTimer() / 2);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("type", type);
		nbt.setShort("flow", flow);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		type = nbt.getByte("type");
		flow = nbt.getShort("flow");
		updateCon = true;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		short nf = nbt.getShort("flow");
		if (onDataPacket(nbt) || nf != flow) {
			flow = nf;
			this.markUpdate();
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("flow", flow);
		getUpdatePacket(nbt);
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@Override
	public <M> M getModuleState(int m) {
		int b = getFlowBit(m);
		if (b == 3) return cast(-1);
		EnumFacing f = EnumFacing.VALUES[m];
		TileEntity p = Utils.neighborTile(this, f);
		if (b == 0) {
			if (!pipeClass().isInstance(p)) b = -1;
		} else if ((flow & 0x8000) != 0) {
			if (b == type && !(b == 2 && pipeClass().isInstance(p))) b += 4;
		} else if (filter != null && b == type && !(pipeClass().isInstance(p) && (b == 2 || !filter.blocking()))) b += 2;
		return cast(b);
	}

	@Override
	public boolean isModulePresent(int m) {
		int b = getFlowBit(m);
		if (b == 3) return false;
		else if (b != 0) return true;
		EnumFacing f = EnumFacing.VALUES[m];
		TileEntity p = Utils.neighborTile(this, f);
		return pipeClass().isInstance(p);
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == capability();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> C getCapability(Capability<C> cap, EnumFacing facing) {
		return cap == capability() ? (C) inventory : null;
	}

	protected abstract boolean transferOut(I acc);
	protected abstract boolean transferIn(I acc);
	protected abstract boolean onDataPacket(NBTTagCompound nbt);
	protected abstract void getUpdatePacket(NBTTagCompound nbt);
	protected abstract byte conDir(TileEntity te, EnumFacing side);
	protected abstract int resetTimer();
	protected abstract I createInv();
	protected abstract Class<T> pipeClass();
	protected abstract Capability<I> capability();

}
