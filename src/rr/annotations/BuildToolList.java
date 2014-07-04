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

package rr.annotations;

import static com.sun.mirror.util.DeclarationVisitors.NO_OP;
import static com.sun.mirror.util.DeclarationVisitors.getDeclarationScanner;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.util.SimpleDeclarationVisitor;

/**
 * This class is used to run an annotation processor that lists class
 * names.  The build process uses it to generate the abbreviations that
 * can be used when specifying the tool chain.  See the Tool class for more
 * info.
 */
public class BuildToolList implements AnnotationProcessorFactory {
	// Process any set of annotations
	private static final Collection<String> supportedAnnotations = unmodifiableCollection(Arrays.asList("*"));

	// No supported options
	private static final Collection<String> supportedOptions = emptySet();

	public Collection<String> supportedAnnotationTypes() {
		return supportedAnnotations;
	}

	public Collection<String> supportedOptions() {
		return supportedOptions;
	}

	public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds, AnnotationProcessorEnvironment env) {
		return new ListClassAp(env);
	}

	private static class ListClassAp implements AnnotationProcessor {

		private final AnnotationProcessorEnvironment env;
		protected PrintWriter out;

		ListClassAp(AnnotationProcessorEnvironment env) {
			this.env = env;
			try {
				out = new PrintWriter(new FileWriter("rrtools.properties"));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		public void process() {
			for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations())
				typeDecl.accept(getDeclarationScanner(new ListClassVisitor(),
						NO_OP));
			out.close();
		}

		private class ListClassVisitor extends SimpleDeclarationVisitor {
			@Override
			public void visitClassDeclaration(ClassDeclaration d) {
				Abbrev a = d.getAnnotation(Abbrev.class);
//				File file = d.getPosition().file();
//				try {
//					final String command = "wcb " + file.getAbsolutePath();
//					Process p = Runtime.getRuntime().exec(command);
//					DataInputStream in = new DataInputStream(p.getInputStream());
//					System.out.println(d.getQualifiedName() + " " + in.readLine());
//				} catch (Exception e) {
//					Assert.fail(e);
//				}
				if (a != null) {
					out.println(a.value() + "=" + d.getQualifiedName());
				}
			}
		}
	}
}
