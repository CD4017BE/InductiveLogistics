package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.List;

import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotTank;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.capability.LinkedTank;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemFluidUtil.StackedFluidAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.*;
import static net.minecraftforge.items.CapabilityItemHandler.*;

public class Tank extends BaseTileEntity implements INeighborAwareTile, ITilePlaceHarvest, ITickable, IGuiData, ClientPacketReceiver {

	public static final int[] CAP = {8000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public FluidStack fluid;
	private ItemStack item = ItemStack.EMPTY;
	public LinkedTank tank = new LinkedTank(CAP[0], ()-> fluid, this::setFluid);
	private LinkedInventory inventory = new LinkedInventory(1, 64, (i)-> item, this::setItem);
	private TileEntity target;
	private byte type;
	public boolean lockType, fill, updateItem, checkTarget = true;
	private int lastAmount;

	public Tank() {
		super();
	}

	public Tank(IBlockState state) {
		super(state);
		type = (byte)state.getBlock().getMetaFromState(state);
		tank.cap = CAP[type];
	}

	private void setFluid(FluidStack fluid) {
		if (lockType && fluid == null && !world.isRemote) this.fluid.amount = 0;
		else this.fluid = fluid;
	}

	private void setItem(ItemStack item, int s) {
		this.item = item;
		updateItem = item.getCount() > 0;
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (updateItem) useFluidContainer();
		if (checkTarget) {
			checkTarget = false;
			target = world.getTileEntity(pos.down());
			if (target != null && !target.hasCapability(FLUID_HANDLER_CAPABILITY, EnumFacing.UP)) target = null;
		}
		if (target != null && fluid != null && fluid.amount > 0) {
			if (target.isInvalid()) {
				target = null;
				checkTarget = true;
			} else {
				IFluidHandler acc = target.getCapability(FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
				if (acc != null) {
					fluid.amount -= acc.fill(fluid, true);
					if (fluid.amount <= 0 && !lockType) fluid = null;
				}
			}
		}
		int n = fluid == null ? lastAmount : lastAmount - fluid.amount;
		if (n != 0 && (lastAmount == 0 || fluid == null || Math.abs(n) > tank.cap / 64)) {
			lastAmount -= n;
			markUpdate();
		}
	}

	private void useFluidContainer() {
		updateItem = false;
		StackedFluidAccess acc = new StackedFluidAccess(item);
		if (!acc.valid()) return;
		if (fill) {
			if (fluid == null || fluid.amount < item.getCount()) return;
			fluid.amount -= acc.fill(fluid, true);
			updateItem = acc.fill(new FluidStack(fluid, tank.cap), false) > 0;
			if (fluid.amount <= 0 && !lockType) fluid = null;
		} else if (fluid == null) {
			fluid = acc.drain(tank.cap, true);
			updateItem = fluid != null && acc.drain(new FluidStack(fluid, tank.cap), false) != null;
		} else {
			if (fluid.amount > tank.cap - item.getCount()) return; 
			FluidStack res = acc.drain(new FluidStack(fluid, tank.cap - fluid.amount), true);
			if (res != null) {
				fluid.amount += res.amount;
				updateItem = acc.drain(new FluidStack(fluid, tank.cap), false) != null;
			}
		}
		item = acc.result();
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == FLUID_HANDLER_CAPABILITY || cap == ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == FLUID_HANDLER_CAPABILITY) return (T)tank;
		if (cap == ITEM_HANDLER_CAPABILITY) return (T)inventory;
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("fluid", Constants.NBT.TAG_COMPOUND)) fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluid"));
		else fluid = null;
		if (nbt.hasKey("item", Constants.NBT.TAG_COMPOUND)) setItem(new ItemStack(nbt.getCompoundTag("item")), 0);
		else item = ItemStack.EMPTY;
		lockType = fluid != null && nbt.getBoolean("lock");
		lastAmount = fluid != null ? fluid.amount : 0;
		fill = nbt.getBoolean("fill");
		type = nbt.getByte("type");
		tank.cap = CAP[type];
		checkTarget = true;
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (fluid != null) nbt.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
		if (!item.isEmpty()) nbt.setTag("item", item.writeToNBT(new NBTTagCompound()));
		nbt.setBoolean("lock", lockType);
		nbt.setBoolean("fill", fill);
		nbt.setByte("type", type);
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		if (fluid != null) fluid.writeToNBT(nbt);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		fluid = FluidStack.loadFluidStackFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (item.hasTagCompound()) {
			fluid = FluidStack.loadFluidStackFromNBT(item.getTagCompound());
			lastAmount = fluid != null ? fluid.amount : 0;
		}
		lockType = fluid != null && fluid.amount == 0;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(fluid == null ? null : fluid.writeToNBT(new NBTTagCompound()));
		if (!item.isEmpty()) list.add(item);
		return list;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (src.equals(pos.down())) checkTarget = true;
	}

	@Override
	public void neighborTileChange(BlockPos src) {
		if (src.equals(pos.down())) checkTarget = true;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addTankSlot(new TankSlot(tank, 0, 184, 16, (byte)0x23));
		cont.addItemSlot(new SlotTank(inventory, 0, 184, 74));
		cont.addPlayerInventory(8, 16);
	}

	@Override
	public int[] getSyncVariables() {
		return new int[] {(lockType?1:0) | (fill?2:0)};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		lockType = (v & 1) != 0;
		fill = (v & 2) != 0;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		switch(data.readByte()) {
		case 0:
			if (lockType) {
				lockType = false;
				if (fluid.amount == 0) fluid = null;
			} else if (fluid != null) {
				lockType = true;
			} break;
		case 1:
			fill = !fill;
			updateItem = !item.isEmpty();
			break;
		case 2:
			if (lockType) fluid.amount = 0;
			else fluid = null;
			break;
		}
	}

}
