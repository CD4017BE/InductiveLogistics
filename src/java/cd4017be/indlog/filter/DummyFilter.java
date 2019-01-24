package cd4017be.indlog.filter;

import cd4017be.api.indlog.filter.FilterBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 *
 * @param <Obj>
 * @param <Inv>
 */
@SideOnly(Side.CLIENT)
public class DummyFilter<Obj, Inv> extends FilterBase<Obj, Inv> {

	public DummyFilter(byte mode) {
		this.mode = mode;
	}

	@Override
	public boolean matches(Obj obj) {
		return true;
	}

	@Override
	public boolean noEffect() {
		return true;
	}

	@Override
	public Item item() {
		return ItemStack.EMPTY.getItem();
	}

	@Override
	public int insertAmount(Obj obj, Inv inv) {
		return 0;
	}

	@Override
	public Obj getExtract(Obj obj, Inv inv) {
		return null;
	}

	@Override
	public boolean transfer(Obj obj) {
		return true;
	}

}
