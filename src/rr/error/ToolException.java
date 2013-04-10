package rr.error;

/**
 * Base class for exceptions that tools throw into target programs.  
 * Unlike all other exceptions thrown by tools, ToolExceptions are not
 * caught by RoadRunner.  They propagate up to the target program.
 * 
 * Programming model exceptions (e.g. a DataRaceException) should subclass
 * ToolException.
 * 
 * @author bpw
 *
 */
public abstract class ToolException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ToolException(final String message) {
		super(message);
	}
}
