package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.List;

import cd4017be.indlog.util.AdvancedTank;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotTank;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.AbstractInventory;
import cd4017be.lib.tileentity.BaseTileEntity;
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
import net.minecraftforge.items.IItemHandler;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.*;
import static net.minecraftforge.items.CapabilityItemHandler.*;

/**
 * 
 * @author cd4017be
 */
public class Tank extends BaseTileEntity implements INeighborAwareTile, ITilePlaceHarvest, ITickable, IGuiData, ClientPacketReceiver {

	public static final int[] CAP = {8000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	public AdvancedTank tank = new AdvancedTank(this, CAP[0], false);
	private IItemHandler inventory = new Inventory();
	private TileEntity target;
	private byte type;
	public boolean auto, checkTarget = true;
	private int lastAmount;

	public Tank() {
		super();
	}

	public Tank(IBlockState state) {
		super(state);
		type = (byte)state.getBlock().getMetaFromState(state);
		tank.cap = CAP[type];
	}

	@Override
	public void update() {
		if (world.isRemote || (world.getTotalWorldTime() & 7) != 0) return;
		if (checkTarget || target != null && target.isInvalid()) {
			checkTarget = false;
			target = world.getTileEntity(pos.down());
			if (target != null && !target.hasCapability(FLUID_HANDLER_CAPABILITY, EnumFacing.UP)) target = null;
		}
		if (target != null && tank.amount() > 0) {
			IFluidHandler acc = target.getCapability(FLUID_HANDLER_CAPABILITY, EnumFacing.UP);
			if (acc != null) tank.decrement(acc.fill(tank.fluid, true));
		}
		int n = lastAmount - tank.amount();
		if (n != 0 && (lastAmount == 0 || tank.fluid == null || Math.abs(n) > tank.cap / 64)) {
			lastAmount -= n;
			markUpdate();
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == FLUID_HANDLER_CAPABILITY || cap == ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == FLUID_HANDLER_CAPABILITY) return (T)tank;
		if (cap == ITEM_HANDLER_CAPABILITY) return (T)(auto ? inventory : tank);
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		type = (byte)(nbt.getByte("type") & 0xf);
		tank.cap = CAP[type];
		tank.output = nbt.getBoolean("fill");
		tank.readNBT(nbt.getCompoundTag("tank"));
		auto = nbt.getBoolean("auto");
		
		//backward compatibility
		if (nbt.hasKey("fluid", Constants.NBT.TAG_COMPOUND)) tank.fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluid"));
		if (nbt.hasKey("item", Constants.NBT.TAG_COMPOUND)) tank.setStackInSlot(0, new ItemStack(nbt.getCompoundTag("item")));
		tank.lock |= nbt.getBoolean("lock");
		
		//init
		lastAmount = tank.amount();
		checkTarget = true;
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("tank", tank.writeNBT(new NBTTagCompound()));
		nbt.setBoolean("fill", tank.output);
		nbt.setBoolean("auto", auto);
		nbt.setByte("type", type);
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		if (tank.fluid != null) tank.fluid.writeToNBT(nbt);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		tank.fluid = FluidStack.loadFluidStackFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (item.hasTagCompound()) {
			tank.fluid = FluidStack.loadFluidStackFromNBT(item.getTagCompound());
			lastAmount = tank.amount();
		}
		tank.lock = tank.fluid != null && tank.fluid.amount == 0;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(tank.fluid == null ? null : tank.fluid.writeToNBT(new NBTTagCompound()));
		tank.addToList(list);
		return list;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (src.equals(pos.down())) checkTarget = true;
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
		if (side == EnumFacing.DOWN) {
			target = te;
			checkTarget = false;
		}
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addTankSlot(new TankSlot(tank, 0, 184, 16, (byte)0x23));
		cont.addItemSlot(new SlotTank(tank, 0, 184, 74));
		cont.addPlayerInventory(8, 16);
	}

	@Override
	public int[] getSyncVariables() {
		return new int[] {(tank.lock?1:0) | (tank.output?2:0) | (auto?4:0)};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		tank.lock = (v & 1) != 0;
		tank.output = (v & 2) != 0;
		auto = (v & 4) != 0;
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
		case 0: if (tank.fluid != null) tank.decrement(tank.fluid.amount); break;
		case 1: tank.setLock(!tank.lock); break;
		case 2: tank.setOut(!tank.output); break;
		case 3:	auto = !auto; break;
		}
		markDirty();
	}

	private class Inventory extends AbstractInventory {

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			tank.setStackInSlot(slot, stack);
		}

		@Override
		public int getSlots() {
			return tank.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return tank.getStackInSlot(slot);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (tank.transposing()) return ItemStack.EMPTY;
			return super.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

	}

}
