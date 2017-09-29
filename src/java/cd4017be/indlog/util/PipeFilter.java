package cd4017be.indlog.util;

import cd4017be.lib.util.IFilter;

/**
 * 
 * @author CD4017BE
 * @param <Obj> Thing to filter
 * @param <Inv> Inventory that provides Obj
 */
public interface PipeFilter<Obj, Inv> extends IFilter<Obj, Inv> {
	boolean active(boolean rs);
	boolean blocking();
}
