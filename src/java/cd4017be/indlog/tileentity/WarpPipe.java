package cd4017be.indlog.tileentity;

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
import net.minecraft.util.math.BlockPos;

import static cd4017be.lib.property.PropertyByte.cast;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNode;
import cd4017be.indlog.multiblock.ConComp;
import cd4017be.indlog.multiblock.WarpPipeNetwork;
import cd4017be.indlog.util.IFluidPipeCon;
import cd4017be.indlog.util.IItemPipeCon;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.tileentity.PassiveMultiblockTile;
import cd4017be.lib.util.Utils;

/**
 * 
 * @author CD4017BE
 *
 */
public class WarpPipe extends PassiveMultiblockTile<WarpPipeNode, WarpPipeNetwork> implements ITilePlaceHarvest, INeighborAwareTile, IInteractiveTile, IModularTile, IItemPipeCon, IFluidPipeCon {

	private Cover cover = new Cover();

	public WarpPipe() {
		comp = new WarpPipeNode(this);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (cover.interact(this, player, hand, item, dir, X, Y, Z)) return true;
		dir = Utils.hitSide(X, Y, Z);
		byte s = (byte)dir.getIndex();
		byte t = comp.con[s];
		if (t >= 2) {
			ConComp con = comp.cons[s];
			if (con != null && con.onClicked(player, hand, item)) {
				markUpdate();
				markDirty();
				return true;
			}
		} else if (player.isSneaking() && item.getCount() == 0) {
			comp.setConnect(s, t != 0);
			this.markUpdate();
			markDirty();
			TileEntity te = Utils.neighborTile(this, dir);
			if (te instanceof WarpPipe) {
				WarpPipe wp = (WarpPipe)te;
				wp.comp.setConnect((byte)(s^1), t != 0);
				wp.markUpdate();
				wp.markDirty();
			}
			return true;
		} 
		if (player.isSneaking()) return false;
		else if (t < 2 && ConComp.createFromItem(item, comp, s)) {
			if (!player.isCreative()) {
				item.grow(-1);
				player.setHeldItem(hand, item);
			}
			this.markUpdate();
			markDirty();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (!world.isRemote) cover.hit(this, player);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		comp.writeToNBT(nbt);
		cover.writeNBT(nbt, "cover", false);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (comp.network != null) comp.network.remove(comp);
		comp = WarpPipeNode.readFromNBT(this, nbt);
		cover.readNBT(nbt, "cover", null);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		byte[] data = nbt.getByteArray("con");
		if (data.length == 6) System.arraycopy(data, 0, comp.con, 0, 6);
		comp.hasFilters = nbt.getByte("filt");
		comp.isBlocked = nbt.getByte("block");
		cover.readNBT(nbt, "cv", this);
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		byte[] data = new byte[comp.con.length];
		System.arraycopy(comp.con, 0, data, 0, data.length);
		nbt.setByteArray("con", data);
		nbt.setByte("filt", comp.hasFilters);
		nbt.setByte("block", comp.isBlocked);
		cover.writeNBT(nbt, "cv", true);
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@Override
	public <T> T getModuleState(int m) {
		if (m == 6) return cover.module();
		byte t = comp.con[m];
		if (t == 1) return cast(-1);
		else if (t > 1) return cast(t - 1 + (comp.hasFilters >> m & 1) * 10 + (comp.isBlocked >> m & 1) * 20);
		TileEntity te = Utils.neighborTile(this, EnumFacing.VALUES[m]);
		return cast(te != null && te.hasCapability(Objects.WARP_PIPE_CAP, EnumFacing.VALUES[m^1]) ? 0 : -1);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 6) return cover.state != null;
		byte t = comp.con[m];
		if (t == 1) return false;
		else if (t > 1) return true;
		TileEntity te = Utils.neighborTile(this, EnumFacing.VALUES[m]);
		return te != null && te.hasCapability(Objects.WARP_PIPE_CAP, EnumFacing.VALUES[m^1]);
	}

	@Override
	public boolean isOpaque() {
		return cover.opaque;
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
			ConComp con = comp.cons[i];
			if (con != null) con.dropContent(list);
		}
		if (cover.stack != null) list.add(cover.stack);
		return list;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		comp.redstone = world.isBlockPowered(pos);
		super.neighborBlockChange(b, src);
	}

}
