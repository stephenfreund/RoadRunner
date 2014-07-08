package rr.meta;

public interface ToolSpecificInstrumentationFilter {

	public  boolean shouldInstrument(ClassInfo rrClass);

	public  boolean shouldInstrument(FieldInfo field);

	public  boolean shouldInstrument(MethodInfo rrMethod);

	public  boolean shouldInstrument(OperationInfo rrOp);

}
