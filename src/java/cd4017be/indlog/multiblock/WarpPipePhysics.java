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

	public HashSet<ITickable> activeCon = new HashSet<ITickable>();
	public ArrayList<IItemDest> itemDest = new ArrayList<IItemDest>();
	public ArrayList<IFluidDest> fluidDest = new ArrayList<IFluidDest>();
	public boolean sortItemDest = false;
	public boolean sortFluidDest = false;
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
		if (con instanceof IItemDest) sortItemDest = true;
		if (con instanceof IFluidDest) sortFluidDest = true;
	}

	private void addConnector(ConComp con) {
		if (con == null) return;
		if (con instanceof ITickable) activeCon.add((ITickable)con);
		if (con instanceof IItemDest) {
			itemDest.add((IItemDest)con);
			sortItemDest = true;
		}
		if (con instanceof IFluidDest) {
			fluidDest.add((IFluidDest)con);
			sortFluidDest = true;
		}
	}

	@SuppressWarnings("unlikely-arg-type")
	private void remConnector(ConComp con) {
		if (con instanceof ITickable) activeCon.remove(con);
		if (con instanceof IItemDest) itemDest.remove(con);
		if (con instanceof IFluidDest) fluidDest.remove(con);
	}

	@Override
	public void onMerged(WarpPipePhysics network) {
		super.onMerged(network);
		activeCon.addAll(network.activeCon);
		if (!network.itemDest.isEmpty()) {
			itemDest.addAll(network.itemDest);
			sortItemDest = true;
		}
		if (!network.fluidDest.isEmpty()) {
			fluidDest.addAll(network.fluidDest);
			sortFluidDest = true;
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
		if (sortItemDest) {
			Collections.sort(itemDest, destSort);
			sortItemDest = false;
		}
		if (sortFluidDest) {
			Collections.sort(fluidDest, destSort);
			sortFluidDest = false;
		}
		if (++timer >= TICKS) {
			timer = 0;
			for (ITickable con : activeCon) con.update();
		}
		return true;
	}

	/**
	 * Insert an item stack into valid destinations
	 * @param item
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
	 * Insert an fluid stack into valid destinations
	 * @param fluid
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
		public boolean extractItem(ToIntFunction<ItemStack> acceptor);
		public boolean blockItem();
	}

	public static interface IFluidSrc extends IObjLink, IPrioritySorted {
		public int extractFluid(FluidStack fluid, int max);
		public boolean extractFluid(ToIntFunction<FluidStack> acceptor);
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
