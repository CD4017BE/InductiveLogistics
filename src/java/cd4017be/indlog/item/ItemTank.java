package cd4017be.indlog.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import java.util.List;

import cd4017be.indlog.render.tesr.FluidRenderer;
import cd4017be.indlog.tileentity.Tank;
import cd4017be.indlog.util.FluidHandlerDirectNBT;
import cd4017be.lib.item.ItemVariantBlock;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTank extends ItemVariantBlock {

	public ItemTank(Block id) {
		super(id);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		FluidStack fluid = FluidStack.loadFluidStackFromNBT(item.getTagCompound());
		if (fluid == null) list.add(TooltipUtil.format("tile.indlog.tank.empty", (double)Tank.CAP[item.getItemDamage()] / 1000D));
		else list.add(TooltipUtil.format("tile.indlog.tank.stor", fluid.getLocalizedName(), (double)fluid.amount / 1000D, (double)Tank.CAP[item.getItemDamage()] / 1000D));
		super.addInformation(item, player, list, b);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
		if (!isInCreativeTab(tab)) return;
		Item item = this;
		for (int i = 0; i < Tank.CAP.length; i++)
			if (Tank.CAP[i] > 0)
				list.add(new ItemStack(item, 1, i));
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return new FluidHandlerDirectNBT(stack, Tank.CAP[stack.getItemDamage()]);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return stack.hasTagCompound();
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		if (!stack.hasTagCompound()) return 1;
		return 1.0 - (double)stack.getTagCompound().getInteger("Amount") / (double)Tank.CAP[stack.getItemDamage()];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		if (!stack.hasTagCompound()) return 0;
		Fluid fluid = FluidRegistry.getFluid(stack.getTagCompound().getString("FluidName"));
		return fluid == null ? 0 : FluidRenderer.instance.fluidColor(fluid);
	}

}
