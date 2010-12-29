package rr.state.agent;

public interface DefineClassListener {

	public byte[] define(ClassLoader loader, String name, byte[] bytes);
		
}
