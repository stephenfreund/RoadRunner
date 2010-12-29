package rr.loader;

import rr.meta.ClassInfo;

public class MethodResolutionException extends Exception {

	protected final ClassInfo rrClass;
	protected final String methodName;
	protected final String desc;
	
	public MethodResolutionException(ClassInfo classInfo, String methodName, String desc) {
		super();
		this.rrClass = classInfo;
		this.methodName = methodName;
		this.desc = desc;
	}

	@Override
	public String getMessage() {
		return "Cannot find method: " + rr.meta.MetaDataInfoKeys.getMethodKey(rrClass, methodName, desc);
	}
	
	
	
	
}
