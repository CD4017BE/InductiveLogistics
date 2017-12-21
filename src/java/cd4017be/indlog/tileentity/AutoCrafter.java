package cd4017be.indlog.tileentity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

import static net.minecraftforge.items.CapabilityItemHandler.*;

public class AutoCrafter extends BaseTileEntity implements ITickable, IRedstoneTile, ITilePlaceHarvest, IGuiData, ClientPacketReceiver {

	public static int INTERVAL = 10;

	private final InventoryCrafting icr = new InventoryCrafting(ItemFluidUtil.CraftContDummy, 3, 3);
	private IRecipe lastRecipe;
	private ItemStack output = ItemStack.EMPTY;
	private final LinkedInventory inventory = new LinkedInventory(1, 64, (i)-> output, (o, i)-> output = o);
	private final ItemStack[] results = new ItemStack[7]; {Arrays.fill(results, ItemStack.EMPTY);}
	private final byte[] inputs = new byte[6];
	public final byte[] grid = new byte[10]; {Arrays.fill(grid, (byte)-1);}
	/**bit[0]: check rs, bit[1]: emit rs */
	public byte rsMode;
	private byte rsOut;
	public int amount;
	private int lastAm = 1;
	private boolean outEmpty;
	private byte time;

	public AutoCrafter() {
	}

	public AutoCrafter(IBlockState state) {
		super(state);
	}

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % INTERVAL != time) return;
		if (outEmpty && output.getCount() < amount * lastAm && ((rsMode & 1) == 0 || hasRSInput())) {
			byte nrs = 0;
			//search for ingredients
			Ingred[] ingreds = new Ingred[6];
			for (int i = 0; i < 6; i++)  {
				int j = inputs[i];
				if (j > 0) {
					Ingred ing = findIngred(Utils.neighborCapability(this, EnumFacing.VALUES[i], ITEM_HANDLER_CAPABILITY), amount * j);
					if (ing == null) nrs |= 1 << i;
					else ingreds[i] = ing;
				}
			}
			if ((rsMode & 2) != 0 && nrs != rsOut) updateRS(nrs);
			if (nrs != 0) return;
			//populate crafting grid
			for (int i = 0; i < 9; i++) {
				int j = grid[i];
				if (j < 0 || j >= 6) icr.setInventorySlotContents(i, ItemStack.EMPTY);
				else icr.setInventorySlotContents(i, ItemHandlerHelper.copyStackWithSize(ingreds[j].stack, 1));
			}
			//find recipe
			if (lastRecipe == null || !lastRecipe.matches(icr, world)) {
				lastRecipe = null;
				for (IRecipe r : CraftingManager.REGISTRY)
					if(r.matches(icr, world)) {
						lastRecipe = r;
						break;
					}
				if (lastRecipe == null) return;
			}
			//extract ingredients
			for (int i = 0; i < 6; i++)
				if (ingreds[i] != null) {
					int n = inputs[i] * amount;
					n -= ingreds[i].extract(n);
					if (n > 0) {//this shouldn't happen but in case it does: ABORT!
						results[i] = ItemHandlerHelper.copyStackWithSize(ingreds[i].stack, inputs[i] * amount - n);
						for (i--; i >= 0; i--) results[i] = ItemHandlerHelper.copyStackWithSize(ingreds[i].stack, inputs[i] * amount);
						markDirty();
						return;
					}
				}
			//output results
			ItemStack stack = lastRecipe.getCraftingResult(icr);
			lastAm = stack.getCount();
			if (amount > 1) stack.setCount(lastAm * amount);
			results[6] = stack;
			outEmpty = false;
			NonNullList<ItemStack> out = lastRecipe.getRemainingItems(icr);
			for (int i = 0; i < out.size(); i++) {
				stack = out.get(i);
				int n = stack.getCount();
				if (n <= 0) continue;
				if (amount > 1) stack.setCount(n * amount);
				int j = grid[i];
				if (j < 0 || j >= 6) //previously empty slots have left overs?
					ItemFluidUtil.dropStack(stack, world, pos); //just throw them away!
				else {
					ItemStack item = results[j];
					int m = item.getCount();
					if (m <= 0) results[j] = stack;
					else if (ItemHandlerHelper.canItemStacksStack(stack, item)) item.setCount(m + n);
					else //previously identical items have different left overs?
						ItemFluidUtil.dropStack(stack, world, pos); //just throw them away!
				}
			}
		} else if (rsOut != 0) updateRS((byte) 0);
		if (!outEmpty) output();
	}

	private Ingred findIngred(IItemHandler acc, int am) {
		if (acc == null) return null;
		ArrayList<Ingred> found = new ArrayList<Ingred>();
		int s = acc.getSlots();
		for (int j = 0; j < s; j++) {
			ItemStack item = acc.extractItem(j, am, true);
			int m = item.getCount();
			if (m <= 0) continue;
			for (Ingred i : found)
				if (ItemHandlerHelper.canItemStacksStack(item, i.stack)) { 
					if (i.add(j, m) >= am) return i;
					m = 0;
					break;
				}
			if (m != 0) {
				Ingred i = new Ingred(acc, item, j);
				if (m >= am) return i;
				found.add(i);
			}
		}
		return null;
	}

	private void output() {
		ItemStack stack = results[6];
		if (stack.getCount() <= 0 || (results[6] = inventory.insertItem(0, stack, false)).getCount() <= 0)
			outEmpty = true;
		int s = grid[9];
		if (s < 0 || s >= 6) {
			for (int i = 0; i < 6; i++) {
				stack = results[i];
				if (stack.getCount() <= 0) continue;
				IItemHandler acc = Utils.neighborCapability(this, EnumFacing.VALUES[i], ITEM_HANDLER_CAPABILITY);
				results[i] = stack = ItemHandlerHelper.insertItemStacked(acc, stack, false);
				outEmpty &= stack.getCount() <= 0;
			}
		} else {
			IItemHandler acc = Utils.neighborCapability(this, EnumFacing.VALUES[s], ITEM_HANDLER_CAPABILITY);
			for (int i = 0; i < 6; i++) {
				stack = results[i];
				if (stack.getCount() <= 0) continue;
				results[i] = stack = ItemHandlerHelper.insertItemStacked(acc, stack, false);
				outEmpty &= stack.getCount() <= 0;
			}
		}
		markDirty();
	}

	private boolean hasRSInput() {
		for (EnumFacing dir : EnumFacing.values())
			if (inputs[dir.ordinal()] == 0 && world.getRedstonePower(pos.offset(dir), dir) > 0)
				return true;
		return false;
	}

	private void updateRS(byte nrs) {
		nrs ^= rsOut;
		rsOut ^= nrs;
		for (EnumFacing dir : EnumFacing.values()) {
			if ((nrs & 1) != 0) {
				BlockPos bp = pos.offset(dir);
				IBlockState state = world.getBlockState(bp);
				state.neighborChanged(world, bp, blockType, pos);
				if (state.getBlock().shouldCheckWeakPower(state, world, bp, dir))
					world.notifyNeighborsOfStateExcept(bp, blockType, dir.getOpposite());
			}
			nrs >>= 1;
		}
	}

	private void updateGrid() {
		Arrays.fill(inputs, (byte)0);
		for (int i = 0; i < 9; i++) {
			byte b = grid[i];
			if (b >= 0 && b < 6) inputs[b]++;
		}
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return (rsOut >> side.ordinal() & 1) * 15;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return true;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == ITEM_HANDLER_CAPABILITY ? (T) inventory : null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		output = new ItemStack(nbt.getCompoundTag("result"));
		outEmpty = true;
		for (int i = 0; i < results.length; i++)
			outEmpty &= (results[i] = ItemFluidUtil.loadItemHighRes(nbt.getCompoundTag("out" + i))).isEmpty();
		amount = nbt.getByte("am");
		rsMode = nbt.getByte("mode");
		rsOut = nbt.getByte("rs");
		byte[] ba = nbt.getByteArray("grid");
		System.arraycopy(ba, 0, grid, 0, Math.min(grid.length, ba.length));
		updateGrid();
		lastAm = nbt.getByte("lam");
		if (lastAm < 1) lastAm = 1;
		lastRecipe = null;
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (!output.isEmpty()) nbt.setTag("result", output.writeToNBT(new NBTTagCompound()));
		for (int i = 0; i < results.length; i++)
			if (!results[i].isEmpty()) nbt.setTag("out" + i, ItemFluidUtil.saveItemHighRes(results[i]));
		nbt.setByte("am", (byte) amount);
		nbt.setByte("mode", rsMode);
		nbt.setByte("rs", rsOut);
		nbt.setByteArray("grid", grid);
		nbt.setByte("lam", (byte) lastAm);
		return super.writeToNBT(nbt);
	}

	class Ingred {
		final IItemHandler acc;
		final ItemStack stack;
		final int[] idx;
		int l;
		Ingred(IItemHandler acc, ItemStack stack, int i) {
			this.acc = acc;
			this.stack = stack.copy();
			this.idx = new int[acc.getSlots() - i];
			this.idx[0] = i;
			this.l = 1;
		}
		int add(int i, int n) {
			idx[l++] = i;
			n += stack.getCount();
			stack.setCount(n);
			return n;
		}
		int extract(int n) {
			int m = 0;
			for (int i = 0; i < l && m < n; i++)
				m += acc.extractItem(idx[i], n - m, false).getCount();
			return m;
		}
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		if ((posIn.getX() + posIn.getY() + posIn.getZ() & 1) == 0) time = 0;
		else time = (byte) Math.min(127, INTERVAL / 2);
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		int cmd = data.readByte() & 0xff;
		if (cmd < 10) {
			grid[cmd] = data.readByte();
			updateGrid();
		} else if (cmd == 10) {
			amount = data.readByte() & 0xff;
			if (amount > 64) amount = 64;
		} else if (cmd == 11) rsMode ^= 1;
		else if (cmd == 12) {
			rsMode ^= 2;
			if ((rsMode & 2) == 0) updateRS((byte) 0);
		}
		markDirty();
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addItemSlot(new SlotItemHandler(inventory, 0, 107, 34));
		cont.addPlayerInventory(8, 86);
	}

	@Override
	public int[] getSyncVariables() {
		int pgrid = 0;
		for (int i = 0; i < grid.length; i++)
			pgrid |= (grid[i] & 7) << (i * 3);
		return new int[] {amount, rsMode, pgrid};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0: amount = v; break;
		case 1: rsMode = (byte)v; break;
		case 2: for (int j = 0; j < grid.length; j++) {
				int k = v >> (j * 3) & 7;
				grid[j] = (byte)(k >= 6 ? -1 : k);
			}
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
		if (!output.isEmpty()) list.add(output);
		for (ItemStack stack : results)
			if (!stack.isEmpty()) list.add(stack);
		return list;
	}

}
