package cd4017be.indlog.item;

import java.util.List;

import cd4017be.indlog.tileentity.Tank;
import cd4017be.lib.templates.ItemVariantBlock;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;

public class ItemTank extends ItemVariantBlock {

	public ItemTank(Block id) {
		super(id);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		FluidStack fluid = FluidStack.loadFluidStackFromNBT(item.getTagCompound());
		if (fluid == null) list.add(TooltipUtil.format("tile.indlog.tank.empty", (double)Tank.CAP[item.getItemDamage()] / 1000D));
		else list.add(TooltipUtil.format("tile.indlog.tank.stor", fluid.getLocalizedName(), (double)fluid.amount / 1000D, (double)Tank.CAP[item.getItemDamage()] / 1000D));
		super.addInformation(item, player, list, b);
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
		for (int i = 0; i < Tank.CAP.length; i++)
			if (Tank.CAP[i] > 0)
				list.add(new ItemStack(item, 1, i));
	}

}
