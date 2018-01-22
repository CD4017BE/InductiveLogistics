package cd4017be.indlog.tileentity;

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
		if (world.isRemote || range == 0) return;
		for (int i = SPEED; tank.amount() >= 1000 && i > 0; i--) {
			Fluid fluid = tank.fluid.getFluid();
			if (!fluid.canBePlacedInWorld()) return;
			goUp = fluid.getDensity() <= 0;
			blockId = fluid.getBlock();
			if (dist < 0) {
				EnumFacing dir = getOrientation().front.getOpposite();
				if (range == 1) {
					dist = 0;
					moveBack(dir.getFrontOffsetX(), dir.getFrontOffsetY(), dir.getFrontOffsetZ());
					return;
				}
				if (canUse(pos.offset(dir))) {
					dist = 0;
					blocks[dist] = (dir.getFrontOffsetX() & 0xff) | (dir.getFrontOffsetY() & 0xff) << 8 | (dir.getFrontOffsetZ() & 0xff) << 16 | dir.ordinal() << 24;
				} else return;
			}
			super.update();
		}
	}

	@Override
	protected boolean canUse(BlockPos pos) {
		if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) return false;
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block.isAir(state, world, pos)) return true;
		if (block == blockId) return state != block.getDefaultState();
		Material material = state.getMaterial();
		return material.isReplaceable() && !(material.isLiquid() && state == block.getDefaultState());
	}

	@Override
	protected void moveBack(int x, int y, int z) {
		dist--;
		BlockPos pos = this.pos.add(x, y, z);
		if (canUse(pos)) {
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			block.dropBlockAsItem(world, pos, state, 0);
			if (world.setBlockState(pos, blockId.getDefaultState(), blockNotify ? 3 : 2)) tank.drain(1000, true);
		}
	}

}
