package cd4017be.indlog.multiblock;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.capabilities.Capability;
import cd4017be.api.IAbstractTile;
import cd4017be.indlog.Objects;
import cd4017be.lib.templates.MultiblockComp;

/**
 * 
 * @author CD4017BE
 *
 */
public class BasicWarpPipe extends MultiblockComp<BasicWarpPipe, WarpPipePhysics> {

	public final byte[] con = new byte[6];
	public byte hasFilters = 0, isBlocked = 0;
	public boolean redstone = false;
	public ConComp[] cons = new ConComp[6];

	public BasicWarpPipe(IAbstractTile pipe) {
		super(pipe);
	}

	@Override
	public void setUID(long uid) {
		super.setUID(uid);
		if (network == null) new WarpPipePhysics(this);
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
			updateCon = true;
		}
	}

	public static BasicWarpPipe readFromNBT(IAbstractTile tile, NBTTagCompound nbt) {
		BasicWarpPipe pipe = new BasicWarpPipe(tile);
		NBTTagList list = nbt.getTagList("connectors", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			ConComp con = ConComp.readFromNBT(pipe, list.getCompoundTagAt(i));
			if (con != null) pipe.cons[con.side] = con;
		}
		pipe.isBlocked = nbt.getByte("block");
		new WarpPipePhysics(pipe);
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
	}

	@Override
	public Capability<BasicWarpPipe> getCap() {
		return Objects.WARP_PIPE_CAP;
	}

}
