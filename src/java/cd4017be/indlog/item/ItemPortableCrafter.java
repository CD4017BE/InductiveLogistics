package cd4017be.indlog.item;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cd4017be.indlog.render.gui.GuiPortableCrafting;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.DefaultItem;
import cd4017be.lib.IGuiItem;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.SlotHolo;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.InventoryItem;
import cd4017be.lib.templates.InventoryItem.IItemInventory;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPortableCrafter extends DefaultItem implements IGuiItem, ClientItemPacketReceiver, IItemInventory {

	public static int INTERVAL;
	/**NBT-Tag names */
	public static final String
			TIME = "t",
			ACTIVE = "active",
			AUTO = "auto",
			COUNT = "amount",
			INGRED = "ingreds",
			INGRED_IDX = "idx",
			RESULT = "result",
			DMG = "dmg",
			NBT = "nbt",
			RECIPE = "rcp",
			JEI_DATA = "grid";

	public ItemPortableCrafter(String id) {
		super(id);
		this.setMaxStackSize(1);
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new TileContainer(new GuiData(), player);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiPortableCrafting(new TileContainer(new GuiData(), player));
	}

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer player, ItemStack item, int slot) throws IOException {
		if (item.getItem() != this) return;
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt == null) item.setTagCompound(nbt = new NBTTagCompound());
		switch(dis.readByte()) {
		case 0:
			nbt.setBoolean(ACTIVE, !nbt.getBoolean(ACTIVE));
			break;
		case 1:
			nbt.setBoolean(AUTO, !nbt.getBoolean(AUTO));
			nbt.setBoolean(ACTIVE, false);
			break;
		case 2: {
			byte n = nbt.getByte(COUNT);
			n = dis.readByte();
			if (n < 0) n = 0;
			if (n > 64) n = 64;
			nbt.setByte(COUNT, n);
		} break;
		case 3: {
			byte n = dis.readByte();
			Recipe recipe = new Recipe(nbt);
			recipe.craft(player.inventory, n > 0 ? n : Integer.MAX_VALUE, false);
			if (player.openContainer != null) player.openContainer.detectAndSendChanges();
			nbt.setBoolean(ACTIVE, false);
		} break;
		case 4: try {// set crafting grid (used for JEI recipe transfer)
			NBTTagCompound tag = dis.readCompoundTag();
			ItemStack[] inv = new ItemStack[9];
			ItemFluidUtil.loadInventory(tag.getTagList(JEI_DATA, Constants.NBT.TAG_COMPOUND), inv);
			saveInventory(item, player, inv);
			byte n = tag.getByte(COUNT);
			if (n < 0) n = 0;
			if (n > 64) n = 64;
			nbt.setByte(COUNT, n);
			nbt.setBoolean(ACTIVE, false);
			ItemGuiData.updateInventory(player, player.inventory.currentItem);
		} catch (IOException e) {} break;
		case 5: {
			nbt.setBoolean(DMG, !nbt.getBoolean(DMG));
			saveInventory(item, player, loadInventory(item, player));
			ItemGuiData.updateInventory(player, player.inventory.currentItem);
		} break;
		case 6: {
			nbt.setBoolean(NBT, !nbt.getBoolean(NBT));
			saveInventory(item, player, loadInventory(item, player));
			ItemGuiData.updateInventory(player, player.inventory.currentItem);
		} break;
		}
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		BlockGuiHandler.openItemGui(player, hand);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public void onUpdate(ItemStack item, World world, Entity entity, int s, boolean b) {
		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			NBTTagCompound nbt = item.getTagCompound();
			if (nbt == null) item.setTagCompound(nbt = new NBTTagCompound());
			long t = world.getTotalWorldTime();
			if (nbt.getBoolean(ACTIVE) && (t - (long)nbt.getByte(TIME) & 0xff) >= INTERVAL) {
				InventoryPlayer inv = player.inventory;
				int n = nbt.getByte(COUNT);
				boolean auto = nbt.getBoolean(AUTO);
				Recipe recipe = new Recipe(nbt);
				n -= recipe.craft(inv, n, auto);
				if (!auto) nbt.setByte(COUNT, (byte)n);
				if (n <= 0 || recipe.result.isEmpty()) nbt.setBoolean(ACTIVE, false);
				nbt.setByte(TIME, (byte)t);
			}
		}
	}

	class Recipe {
		int rcpIdx;
		IRecipe recipe;
		ItemStack[] ingreds;
		int[] indices;
		ItemStack result;
		boolean useNBT, useDMG;
		
		Recipe(NBTTagCompound nbt) {
			useDMG = nbt.getBoolean(DMG);
			useNBT = nbt.getBoolean(NBT);
			rcpIdx = nbt.getInteger(RECIPE);
			indices = nbt.getIntArray(INGRED_IDX);
			ingreds = ItemFluidUtil.loadItems(nbt.getTagList(INGRED, Constants.NBT.TAG_COMPOUND));
			result = nbt.hasKey(RESULT) ? new ItemStack(nbt.getCompoundTag(RESULT)) : ItemStack.EMPTY;
			if (ingreds.length != indices.length) {
				indices = new int[0];
				ingreds = new ItemStack[0];
				rcpIdx = -1;
			}
			if (rcpIdx >= 0) {
				List<IRecipe> list = CraftingManager.getInstance().getRecipeList();
				if (rcpIdx < list.size()) recipe = list.get(rcpIdx);
			}
			if (recipe == null)	result = ItemStack.EMPTY;
		}
		
		int craft(InventoryPlayer inv, int n, boolean auto) {
			if (recipe == null) return 0;
			//calculate amount to craft
			if (auto)
				for (ItemStack stack : inv.mainInventory)
					if (itemEqual(stack, result, useNBT, useDMG)) n -= stack.getCount();
			if (n <= 0) return 0;
			n = (n - 1) / result.getCount() + 1;
			//find and count available ingredients
			Slot[] src = new Slot[ingreds.length];
			for (int j = 0; j < ingreds.length; j++) {
				ItemStack ingred = ingreds[j];
				int o = ingred.getCount(), m = 0;
				Slot slot = null;
				ItemStack prev = null;
				for (int i = 0; i < inv.mainInventory.size(); i++) {
					ItemStack stack = inv.mainInventory.get(i);
					if (itemEqual(stack, ingred, useNBT, useDMG)) {
						if (prev != null && n > m / o && !ItemHandlerHelper.canItemStacksStack(prev, stack)) {
							if ((n = m / o) == 0) n = 1;
						}
						m += stack.getCount();
						slot = new Slot(slot, i);
						if (prev == null) src[j] = slot;
						prev = stack;
					}
				}
				m /= o;
				if (m < n && (n = m) <= 0) return 0;
			}
			//fill ingredients into crafting grid
			InventoryCrafting icr = new InventoryCrafting(ItemFluidUtil.CraftContDummy, 3, 3);
			for (int j = 0; j < ingreds.length; j++) {
				Slot slot = src[j];
				for (int k = indices[j] & 0x1ff, i = 0; k != 0; k >>= 1, i++)
					if ((k & 1) != 0) {
						int m = n;
						while (slot != null) {
							ItemStack stack = inv.mainInventory.get(slot.i);
							if (stack.getCount() >= m) {
								icr.setInventorySlotContents(i, ItemHandlerHelper.copyStackWithSize(stack, n));
								stack.grow(-m);
								break;
							}
							m -= stack.getCount();
							stack.setCount(0);
							slot = slot.next;
						}
					}
			}
			//output crafting results
			result = recipe.getCraftingResult(icr);
			boolean invFull = false;
			if (result.isEmpty()) {
				for (int i = 0; i < 9; i++)
					invFull |= addToPlayer(icr.getStackInSlot(i), inv);
			} else {
				if (n > 1) result.setCount(result.getCount() * n);
				invFull |= addToPlayer(result.copy(), inv);
				for (ItemStack stack : recipe.getRemainingItems(icr)) {
					if (n > 1) stack.setCount(stack.getCount() * n);
					invFull |= addToPlayer(stack, inv);
				}
			}
			n = auto ? 0 : result.getCount();
			if (invFull) result = ItemStack.EMPTY;
			return n;
		}
		
	}

	class Slot {
		final int i;
		Slot next;
		Slot(Slot parent, int i) {
			this.i = i;
			if (parent != null) parent.next = this;
		}
	}

	@Override
	public ItemStack[] loadInventory(ItemStack inv, EntityPlayer player) {
		ItemStack[] items = new ItemStack[10];
		Arrays.fill(items, ItemStack.EMPTY);
		if (inv.hasTagCompound()) {
			NBTTagCompound nbt = inv.getTagCompound();
			ItemStack[] ingreds = ItemFluidUtil.loadItems(nbt.getTagList(INGRED, Constants.NBT.TAG_COMPOUND));
			int[] indices = nbt.getIntArray(INGRED_IDX);
			int n = Math.min(indices.length, ingreds.length);
			for (int i = 0; i < n; i++) {
				for (int k = indices[i] & 0x1ff, j = 0; k != 0; k >>= 1, j++)
					if ((k & 1) != 0)
						items[j] = ItemHandlerHelper.copyStackWithSize(ingreds[i], 1);
			}
			if (nbt.hasKey(RESULT)) items[9] = new ItemStack(nbt.getCompoundTag(RESULT));
		}
		return items;
	}

	@Override
	public void saveInventory(ItemStack inv, EntityPlayer player, ItemStack[] items) {
		NBTTagCompound nbt = inv.getTagCompound();
		if (nbt == null) inv.setTagCompound(nbt = new NBTTagCompound());
		boolean useNBT = nbt.getBoolean(NBT), useDMG = nbt.getBoolean(DMG);
		InventoryCrafting icr = ItemFluidUtil.craftingInventory(items, 3);
		ItemStack[] ingreds = new ItemStack[9];
		int[] indices = new int[9];
		int n = 0;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = items[i];
			if (stack.isEmpty()) continue;
			for (int j = 0; j < n; j++) {
				ItemStack ing = ingreds[j];
				if (itemEqual(ing, stack, useNBT, useDMG)) {
					indices[j] |= 1 << i;
					ingreds[j].grow(1);
					stack = null;
					break;
				}
			}
			if (stack != null) {
				indices[n] = 1 << i;
				ingreds[n++] = ItemHandlerHelper.copyStackWithSize(stack, 1);
			}
		}
		nbt.setIntArray(INGRED_IDX, Arrays.copyOf(indices, n));
		nbt.setTag(INGRED, ItemFluidUtil.saveItems(Arrays.copyOf(ingreds, n)));
		int rcpIdx = -1;
		List<IRecipe> list = CraftingManager.getInstance().getRecipeList();
		for (int i = 0; i < list.size(); i++) {
			IRecipe r = list.get(i);
			if (r.matches(icr, player.world)) {
				rcpIdx = i;
				nbt.setTag(RESULT, (items[9] = r.getCraftingResult(icr)).writeToNBT(new NBTTagCompound()));
				break;
			}
		}
		if (rcpIdx < 0) {
			nbt.removeTag(RESULT);
			items[9] = ItemStack.EMPTY;
		}
		nbt.setInteger(RECIPE, rcpIdx);
	}

	public static boolean itemEqual(ItemStack a, ItemStack b, boolean nbt, boolean dmg) {
		return a.getItem() == b.getItem() && (a.getItemDamage() == b.getItemDamage() || (!dmg && a.isItemStackDamageable())) &&
				(!nbt || ItemStack.areItemStackTagsEqual(a, b));
	}

	public static boolean addToPlayer(ItemStack stack, InventoryPlayer inv) {
		if (inv.addItemStackToInventory(stack)) return false;
		inv.player.dropItem(stack, true);
		return true;
	}

	class GuiData extends ItemGuiData {

		public GuiData() {super(ItemPortableCrafter.this);}

		@Override
		public void initContainer(DataContainer container) {
			TileContainer cont = (TileContainer)container;
			this.inv = new InventoryItem(cont.player);
			for (int j = 0; j < 3; j++)
				for (int i = 0; i < 3; i++) 
					cont.addItemSlot(new SlotHolo(inv, i + 3 * j, 17 + i * 18, 16 + j * 18, false, false));
			cont.addItemSlot(new SlotHolo(inv, 9, 89, 34, true, true));
			cont.addPlayerInventory(8, 86, false, true);
		}

	}

}
