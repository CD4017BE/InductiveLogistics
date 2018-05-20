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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

import java.util.List;

import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Utils;

public class InvConnector extends BaseTileEntity implements INeighborAwareTile, IInteractiveTile, ITilePlaceHarvest, ILinkedInventory, IModularTile, ITickable {

	private boolean linkUpdate = true;
	private BlockPos linkPos = Utils.NOWHERE;
	private TileEntity linkObj;
	private EnumFacing conDir = EnumFacing.DOWN;
	private Cover cover = new Cover();

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		linkUpdate = true;
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
		linkUpdate = true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		conDir = EnumFacing.getFront(nbt.getByte("dir"));
		cover.readNBT(nbt, "cover", null);
		linkUpdate = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("dir", (byte)conDir.ordinal());
		cover.writeNBT(nbt, "cover", false);
		return super.writeToNBT(nbt);
	}

	@Override
	public void update() {
		if (world.isRemote || !linkUpdate) return;
		TileEntity last = linkObj;
		TileEntity te = Utils.neighborTile(this, conDir);
		if (te == null) {
			linkPos = Utils.NOWHERE;
			linkObj = null;
		} else if (te instanceof ILinkedInventory) {
			if (((ILinkedInventory)te).getLinkDir() == conDir.getOpposite()) {
				linkObj = null;
				linkPos = Utils.NOWHERE;
			} else {
				linkPos = ((ILinkedInventory)te).getLinkPos();
				linkObj = world.getTileEntity(linkPos);
				if (linkObj instanceof ILinkedInventory) linkObj = null;
				if (linkObj == null) linkPos = Utils.NOWHERE;
			}
		} else {
			linkObj = te;
			linkPos = te.getPos();
		}
		if (linkObj != last) {
			world.notifyNeighborsOfStateChange(pos, getBlockType(), linkUpdate);
			this.markUpdate();
		}
		linkUpdate = false;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (cover.interact(this, player, hand, item, s, X, Y, Z)) return true;
		if (player.isSneaking() && item.isEmpty()) {
			if (linkObj == null) player.sendMessage(new TextComponentString(TooltipUtil.translate("cd4017be.inv_con.noLink")));
			else player.sendMessage(new TextComponentString(TooltipUtil.format("cd4017be.inv_con.link", world.getBlockState(linkPos).getBlock().getLocalizedName(), linkPos.toString())));
			return true;
		} else if (item.isEmpty()) {
			this.connect();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (!world.isRemote) cover.hit(this, player);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		this.connect();
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (cover.stack != null) list.add(cover.stack);
		return list;
	}

	private void connect() {
		EnumFacing d;
		for (int i = 1; i < 6; i++) {
			d = EnumFacing.VALUES[(conDir.ordinal() + i) % 6];
			if (Utils.neighborTile(this, d) != null) {
				conDir = d;
				this.markUpdate();
				linkUpdate = true;
				markDirty();
				return;
			}
		}
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		conDir = EnumFacing.getFront(nbt.getByte("dir"));
		linkPos = nbt.getBoolean("link") ? new BlockPos(0, 0, 0) : new BlockPos(0, -1, 0);
		cover.readNBT(nbt, "cover", this);
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("dir", (byte)conDir.ordinal());
		nbt.setBoolean("link", linkPos.getY() >= 0);
		cover.writeNBT(nbt, "cover", true);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public BlockPos getLinkPos() {
		return linkPos;
	}

	@Override
	public EnumFacing getLinkDir() {
		return conDir;
	}
	
	@Override
	public TileEntity getLinkObj() {
		return linkObj;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing s) {
		return linkObj == null ? false : linkObj.hasCapability(cap, s);
	}

	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing s) {
		if (linkObj == null) return null;
		if (linkObj.isInvalid()) {
			linkObj = null;
			linkUpdate = true;
			return null;
		}
		return linkObj.getCapability(cap, s);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		if (m == 6) return cover.module();
		EnumFacing d = EnumFacing.VALUES[m];
		return (T) Byte.valueOf(d == conDir ?
				linkPos.getY() >= 0 ? (byte)2 : (byte)1 :
				isModulePresent(m) ? (byte)0 : (byte)-1);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 6) return cover.state != null;
		EnumFacing d = EnumFacing.VALUES[m];
		if (d == conDir) return true;
		TileEntity te = Utils.neighborTile(this, d);
		return te instanceof ILinkedInventory && ((ILinkedInventory)te).getLinkDir() == d.getOpposite();
	}

	@Override
	public boolean isOpaque() {
		return cover.opaque;
	}

}
