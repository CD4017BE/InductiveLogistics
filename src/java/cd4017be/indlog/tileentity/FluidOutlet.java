package cd4017be.indlog.tileentity;

import cd4017be.api.protect.PermissionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;

/**
 * 
 * @author cd4017be
 */
public class FluidOutlet extends FluidIO {

	private Block blockId;

	@Override
	public void update() {
		if (world.isRemote || blocks.length == 0 || tank.amount() < 1000) return;
		Fluid fluid = tank.fluid.getFluid();
		if (!fluid.canBePlacedInWorld()) return;
		goUp = fluid.getDensity() <= 0;
		blockId = fluid.getBlock();
		EnumFacing dir;
		if (dist < 0) {
			dir = getOrientation().front;
			if (canUse(pos.offset(dir))) {
				dist = 0;
				blocks[dist] = (dir.getFrontOffsetX() & 0xff) | (dir.getFrontOffsetY() & 0xff) << 8 | (dir.getFrontOffsetZ() & 0xff) << 16 | dir.ordinal() << 24;
			} else return;
		}
		super.update();
	}

	@Override
	protected boolean canUse(BlockPos pos) {
		if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) return false;
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block.isAir(state, world, pos)) return true;
		if (block == blockId) return state != block.getDefaultState();
		Material material = state.getMaterial();
		return material.isReplaceable() && !material.isLiquid();
	}

	@Override
	protected void moveBack(int x, int y, int z) {
		dist--;
		BlockPos pos = this.pos.add(x, y, z);
		if (canUse(pos))
			if (PermissionUtil.handler.canEdit(world, pos, lastUser)) {
				IBlockState state = world.getBlockState(pos);
				Block block = state.getBlock();
				block.dropBlockAsItem(world, pos, state, 0);
				if (world.setBlockState(pos, blockId.getDefaultState(), blockNotify ? 3 : 2)) tank.drain(1000, true);
			} else {
				//reduce range to get out of protected area
				int l = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) - 1;
				mode = (mode & 0xf00) | (l & 0xff);
			}
	}

}
