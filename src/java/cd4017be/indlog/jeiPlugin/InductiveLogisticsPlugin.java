package cd4017be.indlog.jeiPlugin;

import net.minecraft.item.ItemStack;
import cd4017be.indlog.Objects;
import cd4017be.indlog.render.gui.GuiPortableCrafting;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;

/**
 *
 * @author CD4017BE
 */
@JEIPlugin
public class InductiveLogisticsPlugin implements IModPlugin {

	@Override
	public void register(IModRegistry registry) {
		IRecipeTransferRegistry recipeTransferRegistry = registry.getRecipeTransferRegistry();
		
		registry.addRecipeCatalyst(new ItemStack(Objects.auto_craft), VanillaRecipeCategoryUid.CRAFTING);
		registry.addRecipeCatalyst(new ItemStack(Objects.portable_craft), VanillaRecipeCategoryUid.CRAFTING);
		
		registry.addRecipeClickArea(GuiPortableCrafting.class, 146, 19, 10, 10, VanillaRecipeCategoryUid.CRAFTING);
		
		recipeTransferRegistry.addRecipeTransferHandler(new PortableCraftingHandler(registry.getJeiHelpers().recipeTransferHandlerHelper()), VanillaRecipeCategoryUid.CRAFTING);
	}

}
