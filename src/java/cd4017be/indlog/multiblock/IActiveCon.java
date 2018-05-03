package cd4017be.indlog.multiblock;

import cd4017be.lib.TickRegistry.ITickReceiver;

/**
 * @author CD4017BE
 *
 */
public interface IActiveCon extends ITickReceiver {

	void enable();
	void disable();

}
