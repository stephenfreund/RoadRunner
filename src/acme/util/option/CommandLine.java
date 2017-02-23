/******************************************************************************

Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

All rights reserved.  

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

 * Neither the names of the University of California, Santa Cruz
      and Williams College nor the names of its contributors may be
      used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 ******************************************************************************/

package acme.util.option;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

import acme.util.Assert;
import acme.util.Debug;
import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.Strings;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.collections.IterableIterator;

/**
 * Abstraction for processing command line arguments.
 */
public class CommandLine {

	/** Set to all the args after the last option provided. */
	public static final Option<String> javaArgs = new Option<String>("javaArgs", "");

	private Vector<CommandLineOption<?>> flags = new Vector<CommandLineOption<?>>();
	private final String requiredPart; 
	private final String commandName;

	private ArrayList<String> usageInfo = new ArrayList<String>();
	private ArrayList<Runnable> postProcess = new ArrayList<Runnable>();

	protected CommandLineConstraints constraints = new CommandLineConstraints();

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */
	public static CommandLineOption<Boolean> makeBoolean(String id, final boolean dV, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<Boolean>(id,dV,false, k, usage) {
			@Override
			protected void apply(String arg) {
				this.set(!dV);
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param r   runs this Runnable when the flag is encountered
	 * @param usage  describes the option
	 */
	public static CommandLineOption<Boolean> makeBoolean(String id, final boolean dV, CommandLineOption.Kind k, String usage, final Runnable r) {
		return new CommandLineOption<Boolean>(id,dV,false,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(!dV);
				r.run();
			}

		};
	}


	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */
	public static CommandLineOption<Integer> makeInteger(String id, int dV, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<Integer>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(Integer.parseInt(arg));
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param r   runs this Runnable when the flag is encountered
	 * @param usage  describes the option
	 */
	public static CommandLineOption<Integer> makeInteger(String id, int dV, CommandLineOption.Kind k, String usage, final Runnable r) {
		return new CommandLineOption<Integer>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(Integer.parseInt(arg));
				r.run();
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */
	public static CommandLineOption<Long> makeLong(String id, long dV, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<Long>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(Long.parseLong(arg));
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param r   runs this Runnable when the flag is encountered
	 * @param usage  describes the option
	 */	
	public static CommandLineOption<Long> makeLong(String id, long dV, CommandLineOption.Kind k, String usage, final Runnable r) {
		return new CommandLineOption<Long>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(Long.parseLong(arg));
				r.run();
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */
	public static CommandLineOption<String> makeString(String id, String dV, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<String>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(arg);
			}

		};
	}

	/**
	 * Create a new command line option.  Concats together all string args, separating them with sep.
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param sep  separator character for concatenated string
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */
	public static CommandLineOption<String> makeAppendableString(String id, String dV, final String sep, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<String>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(this.get() + (this.get().length() > 0 ? sep : "") + arg);
			}

		};
	}

	/**
	 * Create a new command line option
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param dV  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param r   runs this Runnable when the flag is encountered
	 * @param usage  describes the option
	 */	
	public static CommandLineOption<String> makeString(String id, String dV, CommandLineOption.Kind k, String usage, final Runnable r) {
		return new CommandLineOption<String>(id,dV,true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.set(arg);
				r.run();
			}

		};
	}

	/**
	 * Create a new command line option that builds a list of Strings.
	 * @param id  keyword for the flag.  Set with -id=X
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 */	
	public static CommandLineOption<ArrayList<String>> makeStringList(String id, CommandLineOption.Kind k, String usage) {
		return new CommandLineOption<ArrayList<String>>(id,new ArrayList<String>(),true,k,usage) {
			@Override
			protected void apply(String arg) {
				this.get().add(arg);
			}		
			@Override
			protected String getType() {
				return "String";
			}

		};
	}

	/**
	 * Create a new command line option to choose from an enumerated type.
	 * @param <T>  The type of the enumeration.
	 * @param id   the key for the flag
	 * @param initial  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 * @param choices The meta object for the enum type
	 */
	public static <T extends Enum<T>> CommandLineOption<T> makeEnumChoice(final String id, T initial, CommandLineOption.Kind k, String usage, final Class<T> choices) {
		return new CommandLineOption<T>(id,initial,true,k,"One of " + Arrays.toString(choices.getEnumConstants()) + ".  " + usage) {
			@Override
			protected void apply(String arg) {
				try {
					set(Enum.valueOf(choices, arg));
				} catch (IllegalArgumentException e) {
					Assert.fail("Invalid option for " + id + ".  Must be one of " + Arrays.toString(choices.getEnumConstants()));
				}
			}

		};
	}


	public static <T extends Enum<T>> CommandLineOption<T> makeEnumChoice(final String id, T initial, CommandLineOption.Kind k, String usage, final Class<T> choices, final Runnable r) {
		return new CommandLineOption<T>(id,initial,true,k,"One of " + Arrays.toString(choices.getEnumConstants()) + ".  " + usage) {
			@Override
			protected void apply(String arg) {
				try {
					set(Enum.valueOf(choices, arg));
					r.run();
				} catch (IllegalArgumentException e) {
					Assert.fail("Invalid option for " + id + ".  Must be one of " + Arrays.toString(choices.getEnumConstants()));
				}
			}

		};
	}



	/**
	 * Create a new command line option to choose from a group of Strings.
	 * @param id   the key for the flag
	 * @param initial  initial value
	 * @param k   indicates whether the option is stable or not
	 * @param usage  describes the option
	 * @param choices The string to choose among
	 */
	public static CommandLineOption<Integer> 
	makeStringChoice(final String id, String initial, CommandLineOption.Kind k, String usage, final String... choices) {
		for (int i = 0; i < choices.length; i++) {
			if (initial.equals(choices[i])) {
				return new CommandLineOption<Integer>(id,i,true,k,usage + " (One of:" + Arrays.toString(choices) + ")") {
					@Override
					protected void apply(String arg) {
						for (int i = 0; i < choices.length; i++) {
							if (arg.equals(choices[i])) {
								this.set(i);
								return;
							}
						}
						Assert.fail("Invalid choice for " + id + ". Must be one of: " + Arrays.toString(choices));
					}	
					@Override
					protected String rep() {
						return choices[get()] + "("+get()+")";
					}
				};
			}
		}
		Assert.fail("Invalid choice for " + id + ". Must be one of: " + Arrays.toString(choices));
		return null;
	}


	/**
	 * Create a new command line option to describe a StringMatcher
	 * @param id
	 * @param defaultResult
	 * @param k
	 * @param usage
	 * @param initialArgs
	 */
	public static CommandLineOption<StringMatcher> makeStringMatcher(String id, final StringMatchResult defaultResult, 
			CommandLineOption.Kind k, String usage, final String... initialArgs) {
		CommandLineOption<StringMatcher> tmp = new CommandLineOption<StringMatcher>(id, new StringMatcher(defaultResult), true, k, usage) {
			private final int defaultLen = initialArgs.length; 
			@Override
			protected void apply(String arg) {
				char ch = arg.charAt(0);
				Assert.assertTrue(ch == '+' || ch == '-', "match item '" + arg + "' must start with +/-");
				// arg += ".*";
				this.get().addNFromEnd(defaultLen, arg);
			}

		};
		for (int i = 0; i < initialArgs.length; i++) {
			String arg = initialArgs[i];
			char ch = arg.charAt(0);
			Assert.assertTrue(ch == '+' || ch == '-', "match item '" + arg + "' must start with +/-");
			//	arg += ".*";
			tmp.get().add(arg);
		}
		return tmp;
	}	



	/**
	 * Add an option to this command line processor.
	 */
	public <T> void add(CommandLineOption<T> c) { 

		String flag = c.getId();
		for (CommandLineOption<?> clo : flags) {
			if (clo.getId().equals(flag)) {
				Assert.warn("Multiple Options with same flag: '%s'", flag);
				break;
			} 
		}

		flags.add(c);
		c.setCommandLine(this);
		usageInfo.add(c.getUsage());
	}

	/**
	 * Add a group marker to separate commands when printing help.
	 */
	public void addGroup(String name) {
		usageInfo.add(name);
		usageInfo.add(Strings.repeat("-", name.length()));
	}

	/**
	 * Create a new command line with the default set of options.
	 */
	public CommandLine(String command, String requiredPart, CommandLineOption<?>... clo) {
		this.requiredPart = requiredPart;
		this.commandName = command;
		for (CommandLineOption<?> c : clo) {
			add(c);
		}


		add(new CommandLineOption<ArrayList<String>>("args",new ArrayList<String>(),true, CommandLineOption.Kind.STABLE,
				"Read additional command-line options from the given file.  Can be used " +
		"multiple times.") {
			@Override
			protected void apply(String arg) {
				this.get().add(arg);
				readArgsFromFile(arg);
			}
			@Override
			protected String getType() {
				return "String";
			}
		});

		add(Debug.debugKeysOption);
		add(Util.quietOption);
		add(Util.logDepthOption);
		add(Util.outputPathOption); 
		add(Util.outputFileOption); 
		add(Util.errorFileOption); 
		add(Yikes.maxYikesOption);

	}


	private void readArgsFromFile(String inFile) {
		try {
			Vector<String> args = new Vector<String>();
			Scanner in = new Scanner(new BufferedReader(new FileReader(inFile)));
			while (in.hasNextLine()) {
				String line = in.nextLine();
				if (!line.startsWith("#")) {
					for (String s : new IterableIterator<String>(new Scanner(line))) {
						args.add(s);
					}
				}
			}
			in.close();
			String argsArray[] = new String[args.size()];
			args.copyInto(argsArray);
			int end = this.applyHelper(argsArray, 0);
			if (end < argsArray.length) {
				Assert.fail("Bad arg in file " + inFile + ": " + argsArray[end]);
			}
		} catch (IOException e) {
			Assert.fail(e);
		}

	}


	/**
	 * Print the usage information for the command line
	 */
	public void usage() {
		Util.error("\n");
		Util.error("Usage\n-----");
		Util.error("    " + this.commandName + "  <options>  " + this.requiredPart + "\n");
		Util.error("Standard Options");
		Util.error("----------------");
		for (String c : usageInfo) {
			Util.error(c);
		}
		Util.error("");
	}

	/**
	 * Add a routine to run after processing a list of options.
	 */
	public void addPostProcessor(Runnable r) {
		this.postProcess.add(r);
	}

	private void postProcess() {
		for (Runnable r : this.postProcess) {
			r.run();
		}
	}

	/**
	 * Apply the command line options to the array of arguments.
	 */
	public int apply(String args[]) {
		int n = applyHelper(args, 0);

		String s = "";
		for (int i = n; i < args.length; i++) {
			s += args[i] + " ";
		}
		javaArgs.set(s);

		Option.dumpOptions();
		return n;
	}

	private int applyHelper(String args[], int firstArg) { 
		Vector<CommandLineOption<?>> processed = new Vector<CommandLineOption<?>>();

		while (firstArg < args.length) {
			String flag = args[firstArg];
			String arg = null;
			int equals = flag.indexOf('=');
			if (equals > -1) {
				arg = flag.substring(equals+1);
				flag = flag.substring(0,equals);
			}

			if (flag.startsWith("-")) {
				boolean found = false;
				String flagNoDash = flag.substring(1);
				int n = flags.size();
				// don't use iterator, so we can add more options as we process...
				for (int i = 0; i < n; i++) {
					CommandLineOption<?> clo = flags.get(i);
					if (clo.getId().equals(flagNoDash)) {
						found = true;
						CommandLineOption failure = constraints.findOutOfOrder(processed, clo);
						if (failure != null) {
							Assert.fail("Option -%s cannot appear after -%s", clo.getId(), failure.getId());
						}
						processed.add(clo);
						clo.checkAndApply(arg);
					} 
				}
				if (!found) {
					Assert.fail("Unrecognized Option: %s.", flag);					
				}
			} else {
				break;
			}
			firstArg++;
		}

		return firstArg;
	}

	/**
	 * Specify that open option must appear on the command line before another.
	 */
	public void addOrderConstraint(CommandLineOption<?> before, CommandLineOption<?> o) {
		constraints.addConstraint(before, o);
	}

	private static enum Nums { ONE, TWO, THREE };

	public static void main(String s[]) {
		CommandLineOption<?>[] os = new CommandLineOption<?>[] {
				makeBoolean("f1", false, CommandLineOption.Kind.STABLE, "set f1"),
				makeBoolean("f2", true,  CommandLineOption.Kind.STABLE, "set f2"),
				makeInteger("i1", 32, CommandLineOption.Kind.STABLE, "set i1"),
				makeString("s1", "Cow", CommandLineOption.Kind.STABLE, "set s1"),
				makeStringList("l1", CommandLineOption.Kind.STABLE, "add to l"),
				makeEnumChoice("num", Nums.ONE, CommandLineOption.Kind.STABLE, "moo", Nums.class)
		};
		CommandLine cl = new CommandLine("mpoo", "file1 file2", os);
		cl.usage();
		Option.dumpOptions();
		cl.apply(new String[] { "-f1", "-l=moo", "-l=moo2", "-num=TWO"} );
		cl.apply(new String[] { "-f1", "-l=moo", "-l=moo2", "-num=THREE"} );
		cl.apply(new String[] { "-f1", "-l=moo", "-l=moo2", "-num=ONE"} );
		Option.dumpOptions();
	}

}



