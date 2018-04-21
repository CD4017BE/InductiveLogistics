package cd4017be.indlog.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidStack;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.ITickReceiver;
import cd4017be.lib.templates.SharedNetwork;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;

/**
 * 
 * @author CD4017BE
 *
 */
public class WarpPipePhysics extends SharedNetwork<BasicWarpPipe, WarpPipePhysics> implements ITickReceiver {

	public static byte TICKS;
	private static final byte SID = 1, SIS = 2, SFD = 4, SFS = 8;

	public HashSet<ITickable> activeCon = new HashSet<ITickable>();
	public ArrayList<IItemDest> itemDest = new ArrayList<IItemDest>();
	public ArrayList<IFluidDest> fluidDest = new ArrayList<IFluidDest>();
	public ArrayList<IItemSrc> itemSrc = new ArrayList<IItemSrc>();
	public ArrayList<IFluidSrc> fluidSrc = new ArrayList<IFluidSrc>();
	private byte sort = 0;
	public boolean disabled = true;
	public byte timer;

	public WarpPipePhysics(BasicWarpPipe core) {
		super(core);
		for (ConComp c : core.cons)
			if (c != null)
				addConnector(c);
	}

	protected WarpPipePhysics(HashMap<Long, BasicWarpPipe> comps) {
		super(comps);
		enable();
	}

	public void enable() {
		if (disabled) {
			disabled = false;
			TickRegistry.instance.add(this);
		}
	}

	public void disable() {
		disabled = true;
	}

	public void addConnector(BasicWarpPipe pipe, ConComp con) {
		pipe.cons[con.side] = con;
		this.addConnector(con);
		if (con instanceof IObjLink) pipe.markDirty();
		if (!pipe.tile.invalid()) Utils.notifyNeighborTile((TileEntity)pipe.tile, EnumFacing.VALUES[con.side]);
	}

	public ConComp remConnector(BasicWarpPipe pipe, byte side) {
		pipe.con[side] = 0;
		ConComp con = pipe.cons[side];
		remConnector(con);
		pipe.cons[side] = null;
		pipe.markDirty();
		pipe.hasFilters &= ~(1 << side);
		((BaseTileEntity)pipe.tile).markUpdate();
		if (!pipe.tile.invalid()) Utils.notifyNeighborTile((TileEntity)pipe.tile, EnumFacing.VALUES[con.side]);
		return con;
	}

	public void reorder(ConComp con) {
		if (con instanceof IItemDest) sort |= SID;
		if (con instanceof IFluidDest) sort |= SFD;
		if (con instanceof IItemSrc) sort |= SIS;
		if (con instanceof IFluidSrc) sort |= SFS;
	}

	private void addConnector(ConComp con) {
		if (con instanceof ITickable) activeCon.add((ITickable)con);
		if (con instanceof IItemDest) itemDest.add((IItemDest)con);
		if (con instanceof IFluidDest) fluidDest.add((IFluidDest)con);
		if (con instanceof IItemSrc) itemSrc.add((IItemSrc)con);
		if (con instanceof IFluidSrc) fluidSrc.add((IFluidSrc)con);
		reorder(con);
	}

	@SuppressWarnings("unlikely-arg-type")
	private void remConnector(ConComp con) {
		if (con instanceof ITickable) activeCon.remove(con);
		if (con instanceof IItemDest) itemDest.remove(con);
		if (con instanceof IFluidDest) fluidDest.remove(con);
		if (con instanceof IItemSrc) itemSrc.remove(con);
		if (con instanceof IFluidSrc) fluidSrc.remove(con);
	}

	@Override
	public void onMerged(WarpPipePhysics network) {
		super.onMerged(network);
		activeCon.addAll(network.activeCon);
		if (!network.itemDest.isEmpty()) {
			itemDest.addAll(network.itemDest);
			sort |= SID;
		}
		if (!network.fluidDest.isEmpty()) {
			fluidDest.addAll(network.fluidDest);
			sort |= SFD;
		}
		if (!network.itemSrc.isEmpty()) {
			itemSrc.addAll(network.itemSrc);
			sort |= SIS;
		}
		if (!network.fluidSrc.isEmpty()) {
			fluidSrc.addAll(network.fluidSrc);
			sort |= SFS;
		}
	}

	@Override
	public WarpPipePhysics onSplit(HashMap<Long, BasicWarpPipe> comps) {
		WarpPipePhysics physics = new WarpPipePhysics(comps);
		for (Entry<Long, BasicWarpPipe> e : comps.entrySet())
			for (ConComp c : e.getValue().cons)
				if (c != null) {
					remConnector(c);
					physics.addConnector(c);
				}
		return physics;
	}

	@Override
	public void remove(BasicWarpPipe comp) {
		super.remove(comp);
		for (ConComp c : comp.cons)
			if (c != null)
				remConnector(c);
	}

	@Override
	public boolean tick() {
		disabled |= components.isEmpty();
		if (disabled) return false;
		if (sort != 0) {
			if ((sort & SID) != 0) Collections.sort(itemDest, destSort);
			if ((sort & SFD) != 0) Collections.sort(fluidDest, destSort);
			if ((sort & SIS) != 0) Collections.sort(itemSrc, srcSort);
			if ((sort & SFS) != 0) Collections.sort(fluidSrc, srcSort);
			sort = 0;
		}
		if (++timer >= TICKS) {
			timer = 0;
			for (ITickable con : activeCon) con.update();
		}
		return true;
	}

	/**
	 * Insert an item stack into valid destinations
	 * @param item to insert
	 * @param pr node priority to start with
	 * @return the result if not possible
	 */
	public ItemStack insertItem(ItemStack item, byte pr) {
		for (IItemDest dest : itemDest) {
			if (dest.getPriority() <= pr && dest.isValid()) {
				item = dest.insertItem(item);
				if (item == null) return null;
				if (dest.blockItem(item)) return item;
			}
		}
		return item;
	}

	/**
	 * Insert a fluid stack into valid destinations
	 * @param fluid to insert
	 * @param pr node priority to start with
	 * @return the result if not possible
	 */
	public FluidStack insertFluid(FluidStack fluid, byte pr) {
		if (fluid == null) return null;
		for (IFluidDest dest : fluidDest) {
			if (dest.getPriority() <= pr && dest.isValid()) {
				fluid = dest.insertFluid(fluid);
				if (fluid == null) return null;
				if (dest.blockFluid(fluid)) return fluid;
			}
		}
		return fluid;
	}

	/**
	 * Extract a given item stack from valid sources
	 * @param item type to search for
	 * @param am maximum amount to extract
	 * @param pr node priority to start with
	 * @return amount that could be extracted
	 */
	public int extractItem(ItemStack item, int am, byte pr) {
		int n = am;
		for (IItemSrc src : itemSrc) {
			if (src.getPriority() >= pr && src.isValid()) {
				n -= src.extractItem(item, n);
				if (n <= 0 || src.blockItem()) break;
			}
		}
		return am - n;
	}

	/**
	 * Extract a matching item stack from valid sources
	 * @param acceptor filter function that returns the maximum accepted amount for a given item stack
	 * @param pr node priority to start with
	 * @return the item stack that was extracted 
	 */
	public ItemStack extractItem(ToIntFunction<ItemStack> acceptor, byte pr) {
		ItemStack stack = null;
		int n = Integer.MAX_VALUE;
		for (IItemSrc src : itemSrc) {
			if (src.getPriority() >= pr && src.isValid()) {
				if (stack != null) n -= src.extractItem(stack, n);
				else if ((stack = src.findItem(acceptor)) != null) {
					n = stack.getCount();
					n -= src.extractItem(stack, n);
				}
				if (src.blockItem() || n <= 0) break;
			}
		}
		if (stack == null) return null;
		stack.grow(-n);
		return stack;
	}

	/**
	 * Extract a given fluid stack from valid sources
	 * @param fluid type to search for
	 * @param am maximum amount to extract
	 * @param pr node priority to start with
	 * @return amount that could be extracted
	 */
	public int extractFluid(FluidStack fluid, int am, byte pr) {
		int n = am;
		for (IFluidSrc src : fluidSrc) {
			if (src.getPriority() >= pr && src.isValid()) {
				n -= src.extractFluid(fluid, n);
				if (n <= 0 || src.blockFluid()) break;
			}
		}
		return am - n;
	}

	/**
	 * Extract a matching fluid stack from valid sources
	 * @param acceptor filter function that returns the maximum accepted amount for a given fluid stack
	 * @param pr node priority to start with
	 * @return the fluid stack that was extracted 
	 */
	public FluidStack extractFluid(ToIntFunction<FluidStack> acceptor, byte pr) {
		FluidStack stack = null;
		int n = Integer.MAX_VALUE;
		for (IFluidSrc src : fluidSrc) {
			if (src.getPriority() >= pr && src.isValid()) {
				if (stack != null) n -= src.extractFluid(stack, n);
				else if ((stack = src.findFluid(acceptor)) != null) {
					n = stack.amount;
					n -= src.extractFluid(stack, n);
				}
				if (src.blockFluid() || n <= 0) break;
			}
		}
		if (stack == null) return null;
		stack.amount -= n;
		return stack;
	}

	public static interface IObjLink {
		public boolean isValid();
		public void updateLink();
	}

	public static interface IPrioritySorted {
		public byte getPriority();
	}

	public static interface IItemDest extends IObjLink, IPrioritySorted {
		public ItemStack insertItem(ItemStack item);
		public boolean blockItem(ItemStack item);
	}

	public static interface IFluidDest extends IObjLink, IPrioritySorted {
		public FluidStack insertFluid(FluidStack fluid);
		public boolean blockFluid(FluidStack fluid);
	}

	public static interface IItemSrc extends IObjLink, IPrioritySorted {
		public int extractItem(ItemStack item, int max);
		public ItemStack findItem(ToIntFunction<ItemStack> acceptor);
		public boolean blockItem();
	}

	public static interface IFluidSrc extends IObjLink, IPrioritySorted {
		public int extractFluid(FluidStack fluid, int max);
		public FluidStack findFluid(ToIntFunction<FluidStack> acceptor);
		public boolean blockFluid();
	}

	public static final Comparator<IPrioritySorted> destSort = new Comparator<IPrioritySorted>(){
		@Override
		public int compare(IPrioritySorted arg0, IPrioritySorted arg1) {
			return arg1.getPriority() - arg0.getPriority();
		}
	};
	public static final Comparator<IPrioritySorted> srcSort = new Comparator<IPrioritySorted>(){
		@Override
		public int compare(IPrioritySorted arg0, IPrioritySorted arg1) {
			return arg0.getPriority() - arg1.getPriority();
		}
	};

}
