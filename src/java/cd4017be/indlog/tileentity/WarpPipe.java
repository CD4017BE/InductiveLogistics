package cd4017be.indlog.tileentity;

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

import java.util.List;

import static cd4017be.lib.util.PropertyByte.cast;
import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.BasicWarpPipe;
import cd4017be.indlog.multiblock.ConComp;
import cd4017be.indlog.multiblock.WarpPipePhysics;
import cd4017be.indlog.util.IFluidPipeCon;
import cd4017be.indlog.util.IItemPipeCon;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.MultiblockTile;
import cd4017be.lib.util.Utils;

public class WarpPipe extends MultiblockTile<BasicWarpPipe, WarpPipePhysics> implements ITilePlaceHarvest, INeighborAwareTile, IInteractiveTile, ITickable, IModularTile, IItemPipeCon, IFluidPipeCon {

	public WarpPipe() {
		comp = new BasicWarpPipe(this);
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		super.update();
		comp.redstone = world.isBlockPowered(pos);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		dir = Utils.hitSide(X, Y, Z);
		byte s = (byte)dir.getIndex();
		byte t = comp.con[s];
		if (t >= 2) {
			long uid = WarpPipePhysics.SidedPosUID(comp.getUID(), s);
			ConComp con = comp.network.connectors.get(uid);
			if (con != null && con.onClicked(player, hand, item, uid)) {
				markUpdate();
				return true;
			}
		} else if (player.isSneaking() && item.getCount() == 0) {
			comp.setConnect(s, t != 0);
			this.markUpdate();
			TileEntity te = Utils.neighborTile(this, dir);
			if (te instanceof WarpPipe) {
				((WarpPipe)te).comp.setConnect((byte)(s^1), t != 0);
				((WarpPipe)te).markUpdate();
			}
			return true;
		} 
		if (player.isSneaking()) return false;
		else if (t < 2 && ConComp.createFromItem(item, comp, s)) {
			item.grow(-1);
			player.setHeldItem(hand, item);
			this.markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		comp.writeToNBT(nbt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		comp = BasicWarpPipe.readFromNBT(this, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		byte[] data = nbt.getByteArray("con");
		if (data.length == 6) System.arraycopy(data, 0, comp.con, 0, 6);
		comp.hasFilters = nbt.getByte("filt");
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		byte[] data = new byte[comp.con.length];
		System.arraycopy(comp.con, 0, data, 0, data.length);
		nbt.setByteArray("con", data);
		nbt.setByte("filt", comp.hasFilters);
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@Override
	public <T> T getModuleState(int m) {
		byte t = comp.con[m];
		if (t == 1) return cast(-1);
		else if (t > 1) return cast(t - 1 + (comp.hasFilters >> m & 1) * 4);
		TileEntity te = Utils.neighborTile(this, EnumFacing.VALUES[m]);
		return cast(te != null && te.hasCapability(Objects.WARP_PIPE_CAP, EnumFacing.VALUES[m^1]) ? 0 : -1);
	}

	@Override
	public boolean isModulePresent(int m) {
		byte t = comp.con[m];
		if (t == 1) return false;
		else if (t > 1) return true;
		TileEntity te = Utils.neighborTile(this, EnumFacing.VALUES[m]);
		return te != null && te.hasCapability(Objects.WARP_PIPE_CAP, EnumFacing.VALUES[m^1]);
	}

	@Override
	public byte getFluidConnectDir(EnumFacing s) {
		byte t = comp.con[s.ordinal()];
		return (byte)(t == 4 ? 2 : t == 5 ? 1 : 0);
	}

	@Override
	public byte getItemConnectDir(EnumFacing s) {
		byte t = comp.con[s.ordinal()];
		return (byte)(t == 2 ? 2 : t == 3 ? 1 : 0);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		for (int i = 0; i < 6; i++) {
			ConComp con = comp.network.getConnector(comp, (byte)i);
			if (con != null) con.dropContent(list);
		}
		return list;
	}

}
