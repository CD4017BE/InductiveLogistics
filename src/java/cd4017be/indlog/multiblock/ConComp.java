package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.indlog.Objects;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

public class ConComp {

	public final byte side;

	public ConComp(byte side) {
		this.side = side;
	}

	public void load(NBTTagCompound nbt) {}

	public void save(NBTTagCompound nbt) {}

	/**
	 * @param nbt
	 * @param id 0:N, 1:B, 2:Ei, 3:Ii, 4:Si, 5:Di, 6:Ci, 7:Ef, 8:If, 9:Df, 10:Sf, 11:Cf
	 * @return
	 */
	public static ConComp readFromNBT(BasicWarpPipe pipe, NBTTagCompound nbt) {
		byte side = nbt.getByte("s");
		byte type = nbt.getByte("t");
		ConComp con;
		switch (type) {
		case 2:
			con = new ItemDestination(pipe, side);
			break;
		case 3:
			con = new ItemExtractor(pipe, side);
			break;
		case 4:
			con = new FluidDestination(pipe, side);
			break;
		case 5:
			con = new FluidExtractor(pipe, side);
			break;
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
		default:
			pipe.con[side] = 1;
			return null;
		}
		pipe.con[side] = type;
		con.load(nbt);
		return con;
	}

	public static NBTTagCompound writeToNBT(byte type, byte side) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("s", side);
		nbt.setByte("t", type);
		return nbt;
	}

	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item, long uid) {
		return false;
	}

	public static boolean createFromItem(ItemStack item, BasicWarpPipe pipe, byte side) {
		if (item.getCount() == 0 || pipe.con[side] >= 2) return false;
		pipe.setConnect(side, false);
		Item type = item.getItem();
		ConComp con;
		if (type == Objects.i_itemPipe && item.getItemDamage() == 1) {
			con = new ItemDestination(pipe, side);
			pipe.con[side] = 2;
		} else if (type == Objects.i_itemPipe && item.getItemDamage() == 2) {
			con = new ItemExtractor(pipe, side);
			pipe.con[side] = 3;
		} else if (type == Objects.i_fluidPipe && item.getItemDamage() == 1) {
			con = new FluidDestination(pipe, side);
			pipe.con[side] = 4;
		} else if (type == Objects.i_fluidPipe && item.getItemDamage() == 2) {
			con = new FluidExtractor(pipe, side);
			pipe.con[side] = 5;
		} else return false;
		pipe.network.addConnector(pipe, con);
		pipe.isBlocked |= 1 << side;
		return true;
	}

	public void dropContent(List<ItemStack> list) {
	}

}
