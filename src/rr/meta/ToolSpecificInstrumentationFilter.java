package rr.meta;

/*
 * Provides a way for a tool to perform specific filtering.
 * Use in conjunction with 
 * 
 * InstrumentationFilter.addToolSpecificInstrumentationFilter
 * 
 * Note: Only one specific filter is permitted.
 */
public interface ToolSpecificInstrumentationFilter {

	public  boolean shouldInstrument(ClassInfo rrClass);

	public  boolean shouldInstrument(FieldInfo field);

	public  boolean shouldInstrument(MethodInfo rrMethod);

	public  boolean shouldInstrument(OperationInfo rrOp);

}
