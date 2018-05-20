package cd4017be.indlog.tileentity;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.SaferFakePlayer;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author CD4017BE
 *
 */
public class BlockPlacer extends BaseTileEntity implements INeighborAwareTile, IItemHandler, ITilePlaceHarvest, IInteractiveTile {

	public static int RANGE = 15;

	public int dx, dy, dz;
	private GameProfile gp = new GameProfile(new UUID(0, 0), "dummyPlayer");
	private float yaw, pitch, rX, rY, rZ;
	private boolean sneaking;
	private FakePlayer player;
	private ItemStack item = ItemStack.EMPTY;

	private ItemStack place(BlockPos pos, ItemStack item) {
		if (player == null) initializePlayer();
		final EnumHand hand = EnumHand.MAIN_HAND;
		player.setPosition(pos.getX() + rX, pos.getY() + rY, pos.getZ() + rZ);
		player.setHeldItem(hand, item);
		IBlockState state = world.getBlockState(pos);
		RayTraceResult res = rayTrace(state, pos);
		do {
			RightClickBlock event = ForgeHooks.onRightClickBlock(player, hand, pos, res.sideHit, res.hitVec);
			if (event.isCanceled()) break;
			if (item.onItemUseFirst(player, world, pos, hand, res.sideHit, (float)res.hitVec.x, (float)res.hitVec.y, (float)res.hitVec.z) != EnumActionResult.PASS) break;
			if ((!sneaking || item.getItem().doesSneakBypassUse(item, world, pos, player) || event.getUseBlock() == Result.ALLOW) && event.getUseBlock() != Result.DENY)
				if (state.getBlock().onBlockActivated(world, pos, state, player, hand, res.sideHit, (float)res.hitVec.x, (float)res.hitVec.y, (float)res.hitVec.z))
					if (event.getUseItem() != Result.ALLOW) break;
			if (!item.isEmpty() && event.getUseItem() != Result.DENY) {
				ItemStack copy = item.copy();
				item.onItemUse(player, world, pos, hand, res.sideHit, (float)res.hitVec.x, (float)res.hitVec.y, (float)res.hitVec.z);
				if (item.isEmpty()) ForgeEventFactory.onPlayerDestroyItem(player, copy, hand);
			}
		} while(false); //break goto
		item = player.getHeldItem(hand);
		player.setHeldItem(hand, ItemStack.EMPTY);
		player.inventory.dropAllItems();
		return item;
	}

	private RayTraceResult rayTrace(IBlockState state, BlockPos pos) {
		Vec3d p = player.getPositionEyes(1), p1 = player.getLook(1);
		RayTraceResult res = state.collisionRayTrace(world, pos, p, p.add(p1.scale(16)));
		if (res != null) return res;
		double t, t1;
		EnumFacing side;
		if (p1.x < 0) {t = -rX / p1.x; side = EnumFacing.EAST;}
		else {t = (1.0 - rX) / p1.x; side = EnumFacing.WEST;}
		if (p1.y < 0) {
			if ((t1 = -rY / p1.y) < t) {t = t1; side = EnumFacing.UP;}
		} else if ((t1 = (1.0 - rY) / p1.y) < t) {t = t1; side = EnumFacing.DOWN;}
		if (p1.z < 0) {
			if ((t1 = -rZ / p1.z) < t) {t = t1; side = EnumFacing.SOUTH;}
		} else if ((t1 = (1.0 - rZ) / p1.z) < t) {t = t1; side = EnumFacing.NORTH;}
		return new RayTraceResult(p.add(p1.scale(t)), side);
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (world.isRemote) return;
		Orientation o = getOrientation();
		if (src.getX() != pos.getX())
			dx = o.front.getFrontOffsetX() + MathHelper.clamp(world.getRedstonePower(pos.offset(EnumFacing.WEST), EnumFacing.WEST) - world.getRedstonePower(pos.offset(EnumFacing.EAST), EnumFacing.EAST), -RANGE, RANGE);
		if (src.getY() != pos.getY())
			dy = o.front.getFrontOffsetY() + MathHelper.clamp(world.getRedstonePower(pos.offset(EnumFacing.DOWN), EnumFacing.DOWN) - world.getRedstonePower(pos.offset(EnumFacing.UP), EnumFacing.UP), -RANGE, RANGE);
		if (src.getZ() != pos.getZ())
			dz = o.front.getFrontOffsetZ() + MathHelper.clamp(world.getRedstonePower(pos.offset(EnumFacing.NORTH), EnumFacing.NORTH) - world.getRedstonePower(pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH), -RANGE, RANGE);
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return item;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (item.getCount() > 0) return stack;
		if (!simulate) item = place(pos.add(dx, dy, dz), ItemHandlerHelper.copyStackWithSize(stack, 1));
		return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - 1);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		int n = item.getCount();
		if (amount > n) amount = n;
		if (amount <= 0) return ItemStack.EMPTY;
		if (!simulate) {
			if ((n -= amount) <= 0) {
				ItemStack stack = item;
				item = ItemStack.EMPTY;
				return stack;
			} else item.setCount(n);
		}
		return ItemHandlerHelper.copyStackWithSize(item, amount);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T)this : null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		gp = new GameProfile(nbt.getUniqueId("FPuuid"), nbt.getString("FPname"));
		item = new ItemStack(nbt.getCompoundTag("item"));
		dx = nbt.getInteger("dx");
		dy = nbt.getInteger("dy");
		dz = nbt.getInteger("dz");
		rX = nbt.getFloat("rx");
		rY = nbt.getFloat("ry");
		rZ = nbt.getFloat("rz");
		yaw = nbt.getFloat("yaw");
		pitch = nbt.getFloat("pitch");
		sneaking = nbt.getBoolean("sneak");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setUniqueId("FPuuid", gp.getId());
		nbt.setString("FPname", gp.getName());
		nbt.setTag("item", item.writeToNBT(new NBTTagCompound()));
		nbt.setInteger("dx", dx);
		nbt.setInteger("dy", dy);
		nbt.setInteger("dz", dz);
		nbt.setFloat("rx", rX);
		nbt.setFloat("ry", rY);
		nbt.setFloat("rz", rZ);
		nbt.setFloat("yaw", yaw);
		nbt.setFloat("pitch", pitch);
		nbt.setBoolean("sneak", sneaking);
		return super.writeToNBT(nbt);
	}

	private void initializePlayer() {
		if (!(world instanceof WorldServer)) return;
		player = new SaferFakePlayer((WorldServer)world, gp);
		player.rotationYaw = yaw;
		player.rotationPitch = pitch;
		player.setSneaking(sneaking);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (entity instanceof EntityPlayer) gp = ((EntityPlayer)entity).getGameProfile();
		Orientation o = getOrientation();
		yaw = (float)(o.ordinal() & 3) * 90F;
		pitch = 90F - 90F * (float)((o.ordinal() >> 2) + 3 & 3);
		sneaking = false;
		dx = o.front.getFrontOffsetX();
		dy = o.front.getFrontOffsetY();
		dz = o.front.getFrontOffsetZ();
		rX = -2.0F * dx + 0.5F;
		rY = -2.0F * dy + 0.5F;
		rZ = -2.0F * dz + 0.5F;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (!item.isEmpty()) return false;
		if (world.isRemote) return true;
		yaw = player.rotationYaw;
		pitch = player.rotationPitch;
		sneaking = player.isSneaking();
		rX = (float)(player.posX - pos.getX());
		rY = (float)(player.posY - pos.getY());
		rZ = (float)(player.posZ - pos.getZ());
		if (this.player == null) initializePlayer();
		else {
			this.player.rotationYaw = yaw;
			this.player.rotationPitch = pitch;
			this.player.setSneaking(sneaking);
		}
		player.sendMessage(new TextComponentString(TooltipUtil.format("cd4017be.placer.cfg", rX, rY, rZ, yaw, pitch, sneaking)));
		return true;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (item.getCount() > 0) list.add(item);
		return list;
	}

}
