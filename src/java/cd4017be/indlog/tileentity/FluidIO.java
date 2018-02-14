package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.List;

import com.mojang.authlib.GameProfile;

import cd4017be.api.protect.PermissionUtil;
import cd4017be.indlog.util.AdvancedTank;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.SlotTank;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

/**
 * 
 * @author cd4017be
 */
public abstract class FluidIO extends BaseTileEntity implements ITickable, IGuiData, ClientPacketReceiver, ITilePlaceHarvest {

	public static int CAP = 8000, MAX_SIZE = 127, SEARCH_MULT = 3, SPEED = 1;

	protected GameProfile lastUser = PermissionUtil.DEFAULT_PLAYER;
	public AdvancedTank tank = new AdvancedTank(this, CAP, this instanceof FluidIntake);
	/**bits[0-7]: x, bits[8-15]: y, bits[16-23]: z, bits[24-26]: ld0, bits[27-29]: ld1, bit[30]: can back */
	protected int[] blocks = new int[MAX_SIZE * SEARCH_MULT];
	protected int dist = -1;
	protected boolean goUp;
	public boolean blockNotify;
	public int range, debugI;

	@Override
	public void update() {
		int target = blocks[dist];
		byte dx = (byte)target, dy = (byte)(target >> 8), dz = (byte)(target >> 16);
		if (dist >= blocks.length - 1) {
			moveBack(dx, dy, dz);
			return;
		}
		int ld0 = target >> 24 & 7, ld1 = target >> 27 & 7;
		boolean canBack = (target & 0x40000000) != 0;
		EnumFacing dir = findNextDir(dx, dy, dz, ld0, ld1, canBack);
		if (dir != null) {
			int s = dir.ordinal();
			if (s < 2) {
				target = 0;
			} else if (s != ld0) {
				target = s << 24 | ld0 << 27;
				if (dist == 0) target |= 0x40000000;
			} else {
				target &= 0x7f000000;
				if (!canBack && ld1 >= 2) {
					EnumFacing ld = EnumFacing.VALUES[ld1];
					if (!isValidPos(dx - ld.getFrontOffsetX(), dy, dz - ld.getFrontOffsetZ()))
						target |= 0x40000000;
				}
			}
			blocks[++dist] = (dx + dir.getFrontOffsetX() & 0xff) | (dy + dir.getFrontOffsetY() & 0xff) << 8 | (dz + dir.getFrontOffsetZ() & 0xff) << 16 | target;
		} else moveBack(dx, dy, dz);
	}

	protected EnumFacing findNextDir(int x, int y, int z, int ld0, int ld1, boolean canBack) {
		if (isValidPos(x, y + (goUp ? 1 : -1), z))
			return goUp ? EnumFacing.UP : EnumFacing.DOWN;
		EnumFacing ld = EnumFacing.VALUES[ld0];
		if (ld0 >= 2 && isValidPos(x + ld.getFrontOffsetX(), y, z + ld.getFrontOffsetZ()))
			return ld;
		if (ld0 < 2 || ld1 < 2) {
			for (EnumFacing dir : EnumFacing.HORIZONTALS)
				if (isValidPos(x + dir.getFrontOffsetX(), y, z + dir.getFrontOffsetZ()))
					return dir;
			return null;
		}
		ld = EnumFacing.VALUES[ld1];
		if (isValidPos(x + ld.getFrontOffsetX(), y, z + ld.getFrontOffsetZ()))
			return ld;
		if (canBack && isValidPos(x - ld.getFrontOffsetX(), y, z - ld.getFrontOffsetZ()))
			return ld.getOpposite();
		return null;
	}

	protected boolean isValidPos(int x, int y, int z) {
		int l = range;
		if (x > l || -x > l || y > l || -y > l || z > l || -z > l || !canUse(pos.add(x, y, z))) return false;
		int p = (x & 0xff) | (y & 0xff) << 8 | (z & 0xff) << 16;
		for (int i = dist - 1; i >= 0; i -= 2) {
			int b = blocks[i] ^ p;
			if ((b & 0xffffff) == 0) return false;
			if ((b & 0x00ff00) != 0) return true;
		}
		return true;
	}

	protected abstract boolean canUse(BlockPos pos);
	protected abstract void moveBack(int x, int y, int z);

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? (T) tank : null;
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		switch(data.readByte()) {
		case 0:
			blockNotify = !blockNotify;
			break;
		case 1:
			lastUser = sender.getGameProfile();
			range = data.readByte();
			if (range < 0) range = 0;
			else if (range > MAX_SIZE) range = MAX_SIZE;
			setDist();
			break;
		case 2: if (tank.fluid != null) tank.decrement(tank.fluid.amount); break;
		case 3: tank.setLock(!tank.lock); break;
		}
		markDirty();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("tank", tank.writeNBT(new NBTTagCompound()));
		nbt.setInteger("mode", range & 0xff | (blockNotify ? 0x100 : 0));
		PermissionUtil.writeOwner(nbt, lastUser);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		tank.readNBT(nbt.getCompoundTag("tank"));
		range = nbt.getInteger("mode");
		blockNotify = (range & 0x100) != 0;
		range &= 0xff;
		lastUser = PermissionUtil.readOwner(nbt);
		setDist();
	}

	protected void setDist() {
		int l = range;
		if (!PermissionUtil.handler.canEdit(world, pos.add(-l, -l, -l), pos.add(l, l, l), lastUser))
			range = 0;
		dist = -1;
	}

	@Override
	public void initContainer(DataContainer cont) {
		TileContainer container = (TileContainer)cont;
		container.addTankSlot(new TankSlot(tank, 0, 184, 16, (byte)0x23));
		container.addItemSlot(new SlotTank(tank, 0, 202, 34));
		container.addPlayerInventory(8, 16);
	}

	@Override
	public int[] getSyncVariables() {
		return new int[]{range & 0xff | (blockNotify ? 0x100 : 0) | (tank.lock ? 0x200 : 0), dist >= 0 ? blocks[dist] : 0};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0:
			range = v & 0xff;
			blockNotify = (v & 0x100) != 0;
			tank.lock = (v & 0x200) != 0;
			break;
		case 1: debugI = v; break;
		}
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		tank.addToList(list);
		return list;
	}

}
