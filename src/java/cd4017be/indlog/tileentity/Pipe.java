package cd4017be.indlog.tileentity;

import static cd4017be.lib.property.PropertyByte.cast;

import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.TileAccess;
import cd4017be.lib.util.Utils;
import cd4017be.indlog.util.PipeFilter;
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
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public abstract class Pipe<T extends Pipe<T, O, F, I>, O, F extends PipeFilter<O, I>, I> extends BaseTileEntity implements INeighborAwareTile, IInteractiveTile, IModularTile, ITickable, ITilePlaceHarvest {

	public static boolean SAVE_PERFORMANCE;

	public O content, last;
	protected T target;
	protected F filter;
	protected ArrayList<TileAccess> invs = null;
	protected byte type, dest;
	/** bits[0-13 (6+1)*2]: (side + total) * dir{0:none, 1:out, 2:in, 3:lock/both}, bit[14]: update, bit[15]: blocked */
	protected short flow;
	private byte time;
	protected boolean updateCon = true;
	protected Cover cover = new Cover();
	private boolean onChunkBorder;

	public Pipe() {}

	public Pipe(IBlockState state) {
		super(state);
		type = (byte)blockType.getMetaFromState(state);
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (world.getTotalWorldTime() % resetTimer() == time) {
			if (updateCon) updateConnections();
			switch(type) {
			case 1:
				if ((flow & 0x8000) != 0) break;
				if (content != null && (filter == null || filter.active(world.isBlockPowered(pos)))) {
					I acc;
					for (TileAccess inv : invs)
						if (inv.te.isInvalid() || (acc = inv.te.getCapability(capability(), inv.side)) == null) updateCon = true;
						else if (transferOut(acc)) break;
				}
			case 3:
				if ((flow & 0x3000) == 0x3000 && content != null && target != null && target.content == null && (filter == null || filter.transfer(content))) {
					if (target.tileEntityInvalid) target = null;
					else {
						target.content = content;
						content = null;
						markDirty();
						if (onChunkBorder) target.markDirty();
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
				if ((flow & 0x3000) == 0x3000 && content != null && target != null && target.content == null) {
					if (target.tileEntityInvalid) target = null;
					else {
						target.content = content;
						content = null;
						markDirty();
						if (onChunkBorder) target.markDirty();
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
		else if (type > 0 && type < 3) invs = new ArrayList<TileAccess>(5);
		if (target != null && target.invalid()) {
			target = null;
			dest = -1;
		}
		TileEntity te;
		if (onChunkBorder && unloadedNeighbor()) {
			//only refresh cached tiles
			for (EnumFacing s : EnumFacing.values()) {
				int io = getFlowBit(s.ordinal());
				if (io == 0 || io == 3) continue;
				te = Utils.neighborTile(this, s);
				if (te == null) continue;
				if (pipeClass().isInstance(te)) {
					if (io == 1 && target == null) target = pipeClass().cast(te);
				} else if(te.hasCapability(capability(), s.getOpposite()) && io == type)
					invs.add(new TileAccess(te, s.getOpposite()));
			}
			return;
		}
		
		EnumFacing dir;
		ArrayList<T> updateList = new ArrayList<T>();
		/** -1: fine, 0: best match, 1: any match, 2: no match */
		int newDest = target == null || target.getFlowBit(dest^1) != 2 || (target.getFlowBit(6) & 1) == 0 ? 2 : -1;
		int lHasIO = getFlowBit(6), nHasIO = type > 2 ? type - 2 : 0, lDirIO, nDirIO;
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
				int pDirIO = pipe.getFlowBit(i ^ 1);
				if (pDirIO != 3) {
					int pHasIO = pipe.getFlowBit(6);
					nDirIO = (~lHasIO | lDirIO) & ~pDirIO & pHasIO;
					if (newDest <= 0 || !(newDest > 1 || pDirIO == 2 || (pHasIO & 2) == 0)) nDirIO &= 2;
					else nDirIO &= 3;
					if (nDirIO == 3) nDirIO = 0;
					if (nDirIO == 0) {
						if (pDirIO != 1 && pHasIO == 1 || pDirIO == 2 && pHasIO == 3 && newDest == 2 && type == 1) nDirIO = 1;
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
				} else if (type > 0 && type < 3) {
					setFlowBit(i, type);
					nHasIO |= type;
					invs.add(new TileAccess(te, dir.getOpposite()));
				} else setFlowBit(i, 3);
			} else if (type > 0 && type < 3 && te.hasCapability(capability(), dir.getOpposite())) {
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
			this.markDirty();
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
		if (cover.interact(this, player, hand, item, dir, X, Y, Z)) return true;
		if (item.getCount() > 0) return false;
		if (player.isSneaking()) {
			dir = Utils.hitSide(X, Y, Z);
			int s = dir.getIndex();
			lockCon(s, getFlowBit(s) == 3);
			return true;
		} else if (filter == null) {
			flow ^= 0x8000;
			markUpdate();
			markDirty();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (world.isRemote) return;
		if (cover.state == null) {
			RayTraceResult hit = Utils.getHit(player, getBlockState(), pos);
			if (hit != null) {
				int i = hit.subHit - 1;
				if (i >= 0 && getFlowBit(i) != 3)
					lockCon(i, false);
			}
		} else cover.hit(this, player);
	}

	protected void lockCon(int s, boolean unlock) {
		int lock = unlock ? 0 : 3;
		setFlowBit(s, lock);
		if (lock != 0) flow |= 0x4000;
		updateCon = true;
		this.markUpdate();
		this.markDirty();
		ICapabilityProvider te = getTileOnSide(EnumFacing.VALUES[s]);
		if (pipeClass().isInstance(te)) {
			T pipe = pipeClass().cast(te);
			if (unlock || pipe.type == 1 || pipe.type == 2) pipe.setFlowBit(s^1, 0);
			else {
				pipe.setFlowBit(s^1, 3);
				pipe.flow |= 0x4000;
			}
			pipe.updateCon = true;
			pipe.markUpdate();
			if (onChunkBorder) pipe.markDirty();
		}
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		updateCon = true;
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
		updateCon = true;
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		if ((posIn.getX() + posIn.getY() + posIn.getZ() & 1) == 0) time = 0;
		else time = (byte) Math.min(127, resetTimer() / 2);
		onChunkBorder = (posIn.getX() + 1 & 15) <= 1 || (posIn.getZ() + 1 & 15) <= 1;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("type", type);
		nbt.setShort("flow", flow);
		cover.writeNBT(nbt, "cover", false);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		type = nbt.getByte("type");
		flow = nbt.getShort("flow");
		cover.readNBT(nbt, "cover", null);
		updateCon = true;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		IBlockState c = cover.state;
		cover.readNBT(nbt, "cv", this);
		short nf = nbt.getShort("flow");
		if (onDataPacket(nbt) || nf != flow || cover.state != c) {
			flow = nf;
			this.markUpdate();
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("flow", flow);
		cover.writeNBT(nbt, "cv", true);
		getUpdatePacket(nbt);
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> M getModuleState(int m) {
		if (m == 6) return cover.module();
		int b = getFlowBit(m);
		if (b == 3) return cast(-1);
		EnumFacing f = EnumFacing.VALUES[m];
		TileEntity p = Utils.neighborTile(this, f);
		boolean isPipe = pipeClass().isInstance(p) && ((T)p).getFlowBit(m^1) != 3;
		if (b == 0) {
			if (!isPipe) b = -1;
		} else if ((flow & 0x8000) != 0) {
			if (b == type && !(b == 2 && isPipe)) b += 4;
		} else if (filter != null && b == type && !(isPipe && (b == 2 || !filter.blocking()))) b += 2;
		return cast(b);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 6) return cover.state != null;
		int b = getFlowBit(m);
		if (b == 3) return false;
		else if (b != 0) return true;
		EnumFacing f = EnumFacing.VALUES[m];
		TileEntity p = Utils.neighborTile(this, f);
		return pipeClass().isInstance(p);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (cover.stack != null) list.add(cover.stack);
		return list;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == capability();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> C getCapability(Capability<C> cap, EnumFacing facing) {
		return cap == capability() ? (C) getInv(type != 0 && facing != null && getFlowBit(facing.ordinal()) != 3) : null;
	}

	protected abstract boolean transferOut(I acc);
	protected abstract boolean transferIn(I acc);
	protected abstract boolean onDataPacket(NBTTagCompound nbt);
	protected abstract void getUpdatePacket(NBTTagCompound nbt);
	protected abstract byte conDir(TileEntity te, EnumFacing side);
	protected abstract int resetTimer();
	protected abstract I getInv(boolean filtered);
	protected abstract Class<T> pipeClass();
	protected abstract Capability<I> capability();

}
