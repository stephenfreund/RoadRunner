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

package rr.loader;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassWriter;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;

import acme.util.Assert;
import acme.util.Util;

public class ClassLoaderTransformer extends ClassWriter {
 
	private int count = 0;

	public ClassLoaderTransformer(ClassReader classReader) {
		super(classReader, 0);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if (name.equals("findLoadedClass")) {
			Util.log("   ---> Making findLoadedClass(" + desc + ") public");
			access = ((access & ~Opcodes.ACC_PRIVATE) & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
			count++;
		} else if (name.equals("defineClass")) {
			Util.log("   ---> Making defineClass(" + desc + ") public");
			access = ((access & ~Opcodes.ACC_PRIVATE) & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
			count++;
		} 
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		Assert.assertTrue(count == 5, "Expected to modify 5 methods but found " + count + ".  You may want to send email to Steve...");
		super.visitEnd();
	}

	public static void main(String args[]) throws IOException {
		InputStream in = ClassLoader.getSystemResourceAsStream("java/lang/ClassLoader.class");
		ClassReader reader = new ClassReader(in);
		ClassLoaderTransformer t = new ClassLoaderTransformer(reader);
		reader.accept(t, 0);
		FileOutputStream out = new FileOutputStream(new File("/tmp/RR_ClassLoader.class"));
		out.write(t.toByteArray());
		out.close();
	}
}
