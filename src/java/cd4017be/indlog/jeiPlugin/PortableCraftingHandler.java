package cd4017be.indlog.jeiPlugin;

import java.util.List;
import java.util.Map.Entry;

import static cd4017be.indlog.item.ItemPortableCrafter.*;

import cd4017be.indlog.Objects;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.transfer.RecipeTransferErrorInternal;

/**
 * 
 * @author CD4017BE
 */
public class PortableCraftingHandler implements IRecipeTransferHandler<TileContainer> {

	@Override
	public Class<TileContainer> getContainerClass() {
		return TileContainer.class;
	}

	@Override
	public IRecipeTransferError transferRecipe(TileContainer container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
		if (!(container.data instanceof ItemGuiData) || player.getHeldItemMainhand().getItem() != Objects.portable_craft) return RecipeTransferErrorInternal.INSTANCE;
		if (!doTransfer) return null;
		IGuiItemStackGroup items = recipeLayout.getItemStacks();
		ItemStack type, stack;
		List<ItemStack> var;
		NBTTagList grid = new NBTTagList();
		NBTTagCompound entry;
		for (Entry<Integer, ? extends IGuiIngredient<ItemStack>> ingr : items.getGuiIngredients().entrySet()) {
			if (!ingr.getValue().isInput() || (var = ingr.getValue().getAllIngredients()).isEmpty()) continue;
			entry = new NBTTagCompound();
			type = null;
			for (int i = 0; type == null && i < player.inventory.mainInventory.size(); i++) 
				if ((stack = player.inventory.mainInventory.get(i)) != null) 
					for (ItemStack item : var) 
						if (stack.isItemEqual(item)) {
							type = stack;
							break;
						}
			if (type == null) type = var.get(0);
			type.writeToNBT(entry);
			entry.setByte("Count", (byte)1);
			entry.setByte("slot", (byte)(ingr.getKey() - 1));
			grid.appendTag(entry);
		}
		entry = new NBTTagCompound();
		entry.setTag(JEI_DATA, grid);
		entry.setByte(COUNT, maxTransfer ? (byte)64 : (byte)1);
		PacketBuffer packet = BlockGuiHandler.getPacketForItem(player.inventory.currentItem);
		packet.writeByte(4);
		packet.writeCompoundTag(entry);
		BlockGuiHandler.sendPacketToServer(packet);
		return null;
	}

}
