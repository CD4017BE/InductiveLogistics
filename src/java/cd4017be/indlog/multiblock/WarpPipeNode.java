package cd4017be.indlog.multiblock;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.capabilities.Capability;
import cd4017be.api.IAbstractTile;
import cd4017be.indlog.Objects;
import cd4017be.indlog.multiblock.WarpPipeNetwork.IObjLink;
import cd4017be.lib.templates.MultiblockComp;

/**
 * 
 * @author CD4017BE
 *
 */
public class WarpPipeNode extends MultiblockComp<WarpPipeNode, WarpPipeNetwork> {

	public final byte[] con = new byte[6];
	public byte hasFilters = 0, isBlocked = 0;
	public boolean redstone = false;
	public ConComp[] cons = new ConComp[6];

	public WarpPipeNode(IAbstractTile pipe) {
		super(pipe);
	}

	@Override
	public void setUID(long uid) {
		super.setUID(uid);
		if (network == null) new WarpPipeNetwork(this);
		if (!tile.isClient()) network.enable();
	}

	@Override
	public void updateCons() {
		super.updateCons();
		for (ConComp con : cons)
			if (con instanceof IObjLink)
				((IObjLink)con).updateLink();
	}

	@Override
	public boolean canConnect(byte side) {
		return con[side] == 0;
	}

	@Override
	public void setConnect(byte side, boolean c) {
		byte c0 = con[side];
		if (!c && c0 == 0) {
			con[side] = 1;
			network.onDisconnect(this, side);
		} else if (c && c0 == 1) {
			con[side] = 0;
			markDirty();
		}
	}

	public static WarpPipeNode readFromNBT(IAbstractTile tile, NBTTagCompound nbt) {
		WarpPipeNode pipe = new WarpPipeNode(tile);
		NBTTagList list = nbt.getTagList("connectors", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			ConComp con = ConComp.readFromNBT(pipe, list.getCompoundTagAt(i));
			if (con != null) pipe.cons[con.side] = con;
		}
		pipe.isBlocked = nbt.getByte("block");
		pipe.redstone = nbt.getBoolean("rs");
		new WarpPipeNetwork(pipe);
		return pipe;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		NBTTagList list = new NBTTagList();
		NBTTagCompound tag;
		ConComp comp;
		for (byte i = 0; i < con.length; i++)
			if (con[i] > 0) {
				tag = ConComp.writeToNBT(con[i], i);
				if ((comp = cons[i]) != null) comp.save(tag);
				list.appendTag(tag);
			}
		if (!list.hasNoTags()) nbt.setTag("connectors", list);
		nbt.setByte("block", isBlocked);
		nbt.setBoolean("rs", redstone);
	}

	@Override
	public Capability<WarpPipeNode> getCap() {
		return Objects.WARP_PIPE_CAP;
	}

}
