package rr.instrument.pragma;

public abstract class PragmaHandler {

	final protected String type;
	final protected String name;
	final protected String desc;
	
	public PragmaHandler(String type, String name, String desc) {
		super();
		this.type = type;
		this.name = name;
		this.desc = desc;
	}

	public boolean process(String t, String n, String d) {
		if (t.equals(type) && n.equals(name) && d.equals(desc)) {
			pragma();
			return true;
		} else {
			return false;
		}
	}
	
	public abstract void pragma();
	
}
