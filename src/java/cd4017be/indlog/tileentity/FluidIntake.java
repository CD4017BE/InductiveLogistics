package cd4017be.indlog.tileentity;

import cd4017be.lib.util.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * 
 * @author cd4017be
 */
public class FluidIntake extends FluidIO {

	private Fluid fluidId;

	@Override
	public void update() {
		if (world.isRemote || range == 0) return;
		for (int i = SPEED; tank.free() >= 1000 && i > 0; i--) {
			EnumFacing dir;
			if (dist < 0) {
				dir = getOrientation().front.getOpposite();
				if (range == 1) {
					dist = 0;
					moveBack(dir.getFrontOffsetX(), dir.getFrontOffsetY(), dir.getFrontOffsetZ());
					return;
				}
				BlockPos pos = this.pos.offset(dir);
				if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) return;
				FluidStack fluid = Utils.getFluid(world, pos, blockNotify);
				if (fluid != null && (tank.fluid == null || fluid.isFluidEqual(tank.fluid))) {
					fluidId = fluid.getFluid();
					goUp = fluidId.getDensity() > 0;
					lastStepDown = dist = 0;
					blocks[dist] = (dir.getFrontOffsetX() & 0xff) | (dir.getFrontOffsetY() & 0xff) << 8 | (dir.getFrontOffsetZ() & 0xff) << 16 | dir.ordinal() << 24;
				} else return;
			}
			super.update();
		}
	}

	@Override
	protected boolean canUse(BlockPos pos) {
		if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) return false;
		FluidStack fluid = Utils.getFluid(world, pos, blockNotify);
		return fluid != null && fluid.getFluid() == fluidId;
	}

	@Override
	protected void moveBack(int x, int y, int z) {
		dist--;
		if (dist < lastStepDown)
			findPrevStepDown();
		BlockPos pos = this.pos.add(x, y, z);
		if (pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) return;
		FluidStack fluid = Utils.getFluid(world, pos, blockNotify);
		if (fluid != null && tank.fill(fluid, false) == fluid.amount && world.setBlockState(pos, Blocks.AIR.getDefaultState(), blockNotify ? 3 : 2)) {
			tank.fill(fluid, true);
		}
	}

}
