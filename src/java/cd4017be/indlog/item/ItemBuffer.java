package cd4017be.indlog.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

import cd4017be.indlog.tileentity.Buffer;
import cd4017be.lib.item.ItemVariantBlock;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

public class ItemBuffer extends ItemVariantBlock {

	public ItemBuffer(Block id) {
		super(id);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		int n = item.hasTagCompound() ? item.getTagCompound().getTagList("Items", Constants.NBT.TAG_COMPOUND).tagCount() : 0;
		list.add(TooltipUtil.format("tile.indlog.buffer.stor", n, Buffer.SLOTS[item.getItemDamage()], Buffer.STACKS[item.getItemDamage()]));
		super.addInformation(item, player, list, b);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
		if (!isInCreativeTab(tab)) return;
		Item item = this;
		for (int i = 0; i < Buffer.SLOTS.length; i++)
			if (Buffer.SLOTS[i] > 0 && Buffer.STACKS[i] > 0)
				list.add(new ItemStack(item, 1, i));
	}

}
