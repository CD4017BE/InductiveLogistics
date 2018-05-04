package cd4017be.indlog.multiblock;

import java.util.List;

import cd4017be.indlog.Objects;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

/**
 * 
 * @author CD4017BE
 *
 */
public abstract class ConComp {

	public final byte side;
	public final WarpPipeNode pipe;

	public ConComp(WarpPipeNode pipe, byte side) {
		this.side = side;
		this.pipe = pipe;
	}

	public void load(NBTTagCompound nbt) {}

	public void save(NBTTagCompound nbt) {}

	/**
	 * @param nbt
	 * @param id 0:N, 1:B, 2:Di, 3:Ei, 4:Df, 5:Ef, 6:Ii, 7:Si, 8:Ai, 9:If, 10:Sf, 11:Af
	 * @return
	 */
	public static ConComp readFromNBT(WarpPipeNode pipe, NBTTagCompound nbt) {
		byte side = nbt.getByte("s");
		byte type = nbt.getByte("t");
		ConComp con;
		switch (type) {
		case 2: con = new ItemDestination(pipe, side); break;
		case 3: con = new ItemExtractor(pipe, side); break;
		case 4: con = new FluidDestination(pipe, side); break;
		case 5: con = new FluidExtractor(pipe, side); break;
		case 6: con = new ItemInjector(pipe, side); break;
		case 7: con = new ItemSource(pipe, side); break;
		case 8: con = new ItemAccess(pipe, side); break;
		case 9: con = new FluidInjector(pipe, side); break;
		case 10:con = new FluidSource(pipe, side); break;
		case 11:con = new FluidAccess(pipe, side); break;
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

	public boolean onClicked(EntityPlayer player, EnumHand hand, ItemStack item) {
		if (item.getCount() == 0 && player.isSneaking()) {
			if (!player.isCreative()) ItemFluidUtil.dropStack(moduleItem(), player);
			pipe.remConnector(side);
			return true;
		}
		return false;
	}

	public static boolean createFromItem(ItemStack item, WarpPipeNode pipe, byte side) {
		if (item.getCount() == 0 || pipe.con[side] >= 2) return false;
		Item type = item.getItem();
		ConComp con;
		if (type == Objects.item_pipe && item.getItemDamage() == 3) {
			con = new ItemDestination(pipe, side);
			pipe.con[side] = 2;
		} else if (type == Objects.item_pipe && item.getItemDamage() == 2) {
			con = new ItemExtractor(pipe, side);
			pipe.con[side] = 3;
		} else if (type == Objects.fluid_pipe && item.getItemDamage() == 3) {
			con = new FluidDestination(pipe, side);
			pipe.con[side] = 4;
		} else if (type == Objects.fluid_pipe && item.getItemDamage() == 2) {
			con = new FluidExtractor(pipe, side);
			pipe.con[side] = 5;
		} else if (type == Objects.item_pipe && item.getItemDamage() == 1) {
			con = new ItemInjector(pipe, side);
			pipe.con[side] = 6;
		} else if (type == Objects.item_pipe && item.getItemDamage() == 4) {
			con = new ItemSource(pipe, side);
			pipe.con[side] = 7;
		} else if (type == Objects.item_pipe && item.getItemDamage() == 0) {
			con = new ItemAccess(pipe, side);
			pipe.con[side] = 8;
		} else if (type == Objects.fluid_pipe && item.getItemDamage() == 1) {
			con = new FluidInjector(pipe, side);
			pipe.con[side] = 9;
		} else if (type == Objects.fluid_pipe && item.getItemDamage() == 4) {
			con = new FluidSource(pipe, side);
			pipe.con[side] = 10;
		} else if (type == Objects.fluid_pipe && item.getItemDamage() == 0) {
			con = new FluidAccess(pipe, side);
			pipe.con[side] = 11;
		} else return false;
		pipe.setConnect(side, false);
		pipe.addConnector(con);
		pipe.isBlocked |= 1 << side;
		return true;
	}

	public void dropContent(List<ItemStack> list) {
		list.add(moduleItem());
	}

	protected abstract ItemStack moduleItem();

}
