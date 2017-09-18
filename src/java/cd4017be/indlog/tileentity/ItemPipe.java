package cd4017be.indlog.tileentity;

import java.util.ArrayList;
import java.util.List;

import cd4017be.api.automation.IItemPipeCon;
import cd4017be.indlog.Objects;
import cd4017be.indlog.util.PipeUpgradeItem;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.LinkedInventory;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.TileAccess;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 *
 * @author CD4017BE
 */
public class ItemPipe extends BaseTileEntity implements ITilePlaceHarvest, INeighborAwareTile, IInteractiveTile, ITickable, IItemPipeCon, IModularTile {

	public static byte ticks = 1;
	public final LinkedInventory invcap = new LinkedInventory(1, 64, this::getItem, this::setItem);
	public ItemStack inventory, last;
	private PipeUpgradeItem filter = null;
	private IItemPipeCon target;
	private EnumFacing targetSide;
	private ArrayList<TileAccess> invs = null;
	private byte type;
	/** bits[0-13 (6+1)*2]: (side + total) * dir{0:none, 1:in, 2:out, 3:lock/both} */
	private short flow;
	private boolean updateCon = true;
	private byte timer = 0;

	private ItemStack getItem(int i) {
		return inventory == null ? ItemStack.EMPTY : inventory;
	}

	private void setItem(ItemStack item, int i) {
		inventory = item.isEmpty() ? null : item;
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (updateCon) this.updateConnections(type);
		if ((flow & 0x3000) == 0x3000) {
			switch(type) {
			case 1:
				if (inventory != null && (filter == null || filter.active(world.isBlockPowered(pos)))) transferIn();
				if (inventory != null && target != null && (filter == null || filter.transfer(inventory)))
					inventory = target.insert(inventory, targetSide);
				break;
			case 2:
				timer++;
				if ((filter == null || filter.active(world.isBlockPowered(pos))) && (timer & 0xff) >= ticks) transferEx();
			default:
				if (inventory != null && target != null) inventory = target.insert(inventory, targetSide);
			}
		}
		if (inventory != last) {
			last = inventory;
			markUpdate();
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T)invcap : null;
	}

	private void updateConnections(int type) {
		if (invs != null) invs.clear();
		else if (type != 0) invs = new ArrayList<TileAccess>(5);
		EnumFacing dir;
		TileEntity te;
		ArrayList<ItemPipe> updateList = new ArrayList<ItemPipe>();
		if (target != null && ((TileEntity)target).isInvalid()) target = null;
		int lHasIO = getFlowBit(6), nHasIO = 0, lDirIO, nDirIO;
		short lFlow = flow;
		for (int i = 0; i < 6; i++) {
			lDirIO = getFlowBit(i);
			if (lDirIO == 3) continue;
			dir = EnumFacing.VALUES[i];
			te = world.getTileEntity(pos.offset(dir));
			if (te == null) setFlowBit(i, 0);
			else if (te instanceof ItemPipe) {
				ItemPipe pipe = (ItemPipe)te;
				int pHasIO = pipe.getFlowBit(6);
				int pDirIO = pipe.getFlowBit(i ^ 1);
				if (pDirIO == 3) nDirIO = 3;
				else if ((nDirIO = pHasIO & ~pDirIO) == 3) 
					nDirIO = lHasIO == 1 && (lDirIO & 1) == 0 ? 2 : lHasIO == 2 && (lDirIO & 2) == 0 ? 1 : 0;
				if (nDirIO == 1) {
					if (target == null) {
						target = pipe;
						targetSide = dir;
					} else if (target != pipe) nDirIO = 0;
				}
				setFlowBit(i, nDirIO);
				if (nDirIO != 3) nHasIO |= nDirIO;
				updateList.add(pipe);
			} else if (te instanceof IItemPipeCon) {
				byte d = ((IItemPipeCon)te).getItemConnectType(i^1);
				d = d == 1 ? 2 : d == 2 ? 1 : (byte)0;
				if (d == 1) {
					if (target == null) {
						target = (IItemPipeCon)te;
						targetSide = dir.getOpposite();
					} else if (target != te) nDirIO = 0;
				}
				setFlowBit(i, d);
				nHasIO |= d;
			} else if (type != 0 && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
				setFlowBit(i, type);
				nHasIO |= type;
				invs.add(new TileAccess(te, dir.getOpposite()));
			} else setFlowBit(i, 0);
		}
		setFlowBit(6, nHasIO);
		if (flow != lFlow) {
			this.markUpdate();
			for (ItemPipe pipe : updateList) pipe.updateCon = true;
		}
		updateCon = false;
	}

	private void transferIn() {
		IItemHandler acc;
		for (TileAccess inv : invs)
			if (inv.te.isInvalid() || (acc = inv.te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inv.side)) == null) updateCon = true;
			else if (PipeUpgradeItem.isNullEq(filter)) {
				inventory = ItemHandlerHelper.insertItem(acc, inventory, false);
				if (inventory.getCount() <= 0) {
					inventory = null;
					break;
				}
			} else {
				int m = filter.insertAmount(inventory, acc);
				if (m > 0) {
					inventory.grow(ItemHandlerHelper.insertItem(acc, inventory.splitStack(m), false).getCount());
					if (inventory.getCount() <= 0) {
						inventory = null;
						break;
					}
				}
			}
	}

	private void transferEx() {
		timer = 0;
		IItemHandler acc;
		for (TileAccess inv : invs)
			if (inv.te.isInvalid() || (acc = inv.te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inv.side)) == null) updateCon = true;
			else if (PipeUpgradeItem.isNullEq(filter)) {
				if (inventory == null) setItem(ItemFluidUtil.drain(acc, -1), 0);
				else {
					int m = inventory.getMaxStackSize() - inventory.getCount();
					if (m <= 0) break;
					inventory.grow(ItemFluidUtil.drain(acc, ItemHandlerHelper.copyStackWithSize(inventory, m)));
				}
			} else {
				int n;
				if (inventory == null) n = 0;
				else if ((n = inventory.getCount()) >= inventory.getMaxStackSize()) break;
				ItemStack extr = filter.getExtract(getItem(0), acc);
				if (extr == null) continue;
				int m = extr.getMaxStackSize() - n;
				if (m < extr.getCount()) extr.setCount(m);
				extr.setCount(ItemFluidUtil.drain(acc, extr) + n);
				inventory = extr;
			}
	}

	@Override
	public void neighborTileChange(BlockPos pos) {
		updateCon = true;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		updateCon = true;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		boolean canF = type != 0;
		if (player.isSneaking() && item.getCount() == 0) {
			dir = Utils.hitSide(X, Y, Z);
			int s = dir.getIndex();
			int lock = this.getFlowBit(s) == 3 ? 0 : 3;
			this.setFlowBit(s, lock);
			updateCon = true;
			this.markUpdate();
			ICapabilityProvider te = getTileOnSide(dir);
			if (te != null && te instanceof ItemPipe) {
				ItemPipe pipe = (ItemPipe)te;
				pipe.setFlowBit(s^1, lock);
				pipe.updateCon = true;
				pipe.markUpdate();
			}
			return true;
		} else if (!player.isSneaking() && item.getCount() == 0 && filter != null) {
			item = new ItemStack(Objects.itemFilter);
			item.setTagCompound(PipeUpgradeItem.save(filter));
			filter = null;
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else if (filter == null && canF && item.getItem() == Objects.itemFilter && item.getTagCompound() != null) {
			filter = PipeUpgradeItem.load(item.getTagCompound());
			item.grow(-1);
			player.setHeldItem(hand, item);
			markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	private int getFlowBit(int b) {
		return flow >> (b * 2) & 3;
	}

	private void setFlowBit(int b, int v) {
		b *= 2;
		flow = (short)(flow & ~(3 << b) | (v & 3) << b);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("flow", flow);
		if (filter != null) nbt.setTag("filter", PipeUpgradeItem.save(filter));
		if (inventory != null) nbt.setTag("item", inventory.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		flow = nbt.getShort("flow");
		if (nbt.hasKey("filter")) filter = PipeUpgradeItem.load(nbt.getCompoundTag("filter"));
		if (nbt.hasKey("item")) inventory = new ItemStack(nbt.getCompoundTag("item"));
		updateCon = true;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		short nf = nbt.getShort("flow");
		boolean f = nbt.getBoolean("filt");
		if (nbt.hasKey("it", 10)) inventory = new ItemStack(nbt.getCompoundTag("it"));
		else inventory = null;
		if (nf != flow || (f ^ filter != null)) {
			if (f) filter = new PipeUpgradeItem();
			else filter = null;
			flow = nf;
			this.markUpdate();
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("flow", flow);
		nbt.setBoolean("filt", filter != null);
		if (last != null) nbt.setTag("it", last.writeToNBT(new NBTTagCompound()));
		return new SPacketUpdateTileEntity(getPos(), -1, nbt);
	}

	@Override
	public byte getItemConnectType(int s) {return 0;}

	@Override
	public ItemStack insert(ItemStack item, EnumFacing side) {
		if (inventory == null) {
			inventory = item;
			return null;
		}
		return item;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		int b = getFlowBit(m);
		EnumFacing f = EnumFacing.VALUES[m];
		ICapabilityProvider p = getTileOnSide(f);
		if (b == 3 || p == null || !p.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite())) return (T)Byte.valueOf((byte)-1);
		if (filter != null && b != 0 && !(b == 2 && p instanceof ItemPipe)) b += 2;
		return (T)Byte.valueOf((byte)b);
	}

	@Override
	public boolean isModulePresent(int m) {
		int b = getFlowBit(m);
		EnumFacing f = EnumFacing.VALUES[m];
		ICapabilityProvider p = getTileOnSide(f);
		return b != 3 && p != null && p.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f.getOpposite());
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (inventory != null) list.add(inventory);
		if (filter != null) {
			ItemStack item = new ItemStack(Objects.itemFilter);
			item.setTagCompound(PipeUpgradeItem.save(filter));
			list.add(item);
		}
		return list;
	}

}
