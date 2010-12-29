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

package acme.util;



/**
 * Various routines for processing and formatting Strings, borrowed from Apache.
 * This file was modified in various ways for the Acme package.
 */


/*
* Copyright (c) 2003, Henri Yandell
* All rights reserved.  
* 
* Redistribution and use in source and binary forms, with or 
* without modification, are permitted provided that the 
* following conditions are met:
* 
* + Redistributions of source code must retain the above copyright notice, 
*   this list of conditions and the following disclaimer.
* 
* + Redistributions in binary form must reproduce the above copyright notice, 
*   this list of conditions and the following disclaimer in the documentation 
*   and/or other materials provided with the distribution.
* 
* + Neither the name of Genjava-Core nor the names of its contributors 
*   may be used to endorse or promote products derived from this software 
*   without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
* ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
* POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
* A set of String library static methods. While extending String or 
* StringBuffer would have been the nicest solution, that is not 
* possible, so a simple set of static methods seems the most workable.
*
* Most methods have now gone to Commons Lang StringUtils.
*/
final public class Strings {

   /**
    * Create a word-wrapped version of a String. Wrap at 80 characters and 
    * use newlines as the delimiter. If a word is over 80 characters long 
    * use a - sign to split it.
    */
   static public String wordWrap(String str) {
       return wordWrap(str, 80, "\n", "-", "");
   }
   /**
    * Create a word-wrapped version of a String. Wrap at a specified width and 
    * use newlines as the delimiter. If a word is over the width in lenght 
    * use a - sign to split it.
    */
   static public String wordWrap(String str, int width) {
       return wordWrap(str, width, "\n", "-", "");
   }

   /**
    * Word-wrap a string.
    *
    * @param str   String to word-wrap
    * @param width int to wrap at
    * @param delim String to use to separate lines
    * @param split String to use to split a word greater than width long
    * @param leftIndent amount to indent lines after first one
    *
    * @return String that has been word wrapped
    */
   static public String wordWrap(String str, int width, String delim, String split, String leftIndent) {
       int sz = str.length();

       /// shift width up one. mainly as it makes the logic easier
       width++;

       // our best guess as to an initial size
       StringBuffer buffer = new StringBuffer(sz/width*delim.length()+sz);

       // every line will include a delim on the end
       width = width - delim.length();

       int idx = -1;
       String substr = null;

       // beware: i is rolled-back inside the loop
       for(int i=0; i<sz; i+=width) {
    	   
           // on the last line
           if(i > sz - width) {
               buffer.append(str.substring(i));
               break;
           }

           // the current line
           substr = str.substring(i, i+width);

           // is the delim already on the line
           idx = substr.indexOf(delim);
           if(idx != -1) {
               buffer.append(substr.substring(0,idx));
               buffer.append(delim);
               i -= width-idx-delim.length();
               
               // Erase a space after a delim. Is this too obscure?
               if(substr.charAt(idx+1) != '\n') {
                   if(Character.isWhitespace(substr.charAt(idx+1))) {
                       i++;
                   }
               }
               continue;
           }

           idx = -1;

           // figure out where the last space is
           char[] chrs = substr.toCharArray();
           for(int j=width; j>0; j--) {
               if(Character.isWhitespace(chrs[j-1])) {
                   idx = j;
                   break;
               }
           }

           // idx is the last whitespace on the line.
           if(idx == -1) {
               for(int j=width; j>0; j--) {
                   if(chrs[j-1] == '-') {
                       idx = j;
                       break;
                   }
               }
               if(idx == -1) {
                   buffer.append(substr);
                   buffer.append(delim);
               } else {
                   if(idx != width) {
                       idx++;
                   }
                   buffer.append(substr.substring(0,idx));
                   buffer.append(delim);
                   i -= width-idx;
               }
           } else {
               /*
               if(force) {
                   if(idx == width-1) {
                       buffer.append(substr);
                       buffer.append(delim);
                   } else {
                       // stick a split in.
                       int splitsz = split.length();
                       buffer.append(substr.substring(0,width-splitsz));
                       buffer.append(split);
                       buffer.append(delim);
                       i -= splitsz;
                   }
               } else {
               */
                   // insert spaces
                   buffer.append(substr.substring(0,idx));
                   buffer.append(repeat(" ",width-idx));
                   buffer.append(delim);
                   i -= width-idx;
//               }
           }
       }
       return buffer.toString().replaceAll("\n", "\n" + leftIndent);
   }
   

   // Padding
   //-----------------------------------------------------------------------
   /**
    * <p>Repeat a String <code>repeat</code> times to form a
    * new String.</p>
    *
    * <pre>
    * StringUtils.repeat(null, 2) = null
    * StringUtils.repeat("", 0)   = ""
    * StringUtils.repeat("", 2)   = ""
    * StringUtils.repeat("a", 3)  = "aaa"
    * StringUtils.repeat("ab", 2) = "abab"
    * StringUtils.repeat("a", -2) = ""
    * </pre>
    *
    * @param str  the String to repeat, may be null
    * @param repeat  number of times to repeat str, negative treated as zero
    * @return a new String consisting of the original String repeated,
    *  <code>null</code> if null String input
    */
   public static String repeat(String str, int repeat) {
       // Performance tuned for 2.0 (JDK1.4)

       if (str == null) {
           return null;
       }
       if (repeat <= 0) {
           return "";
       }
       int inputLength = str.length();
       if (repeat == 1 || inputLength == 0) {
           return str;
       }

       int outputLength = inputLength * repeat;
       switch (inputLength) {
           case 1 :
               char ch = str.charAt(0);
               char[] output1 = new char[outputLength];
               for (int i = repeat - 1; i >= 0; i--) {
                   output1[i] = ch;
               }
               return new String(output1);
           case 2 :
               char ch0 = str.charAt(0);
               char ch1 = str.charAt(1);
               char[] output2 = new char[outputLength];
               for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                   output2[i] = ch0;
                   output2[i + 1] = ch1;
               }
               return new String(output2);
           default :
               StringBuffer buf = new StringBuffer(outputLength);
               for (int i = 0; i < repeat; i++) {
                   buf.append(str);
               }
               return buf.toString();
       }
   }

   /**
    * <p>Returns padding using the specified delimiter repeated
    * to a given length.</p>
    *
    * <pre>
    * StringUtils.padding(0, 'e')  = ""
    * StringUtils.padding(3, 'e')  = "eee"
    * StringUtils.padding(-2, 'e') = IndexOutOfBoundsException
    * </pre>
    *
    * <p>Note: this method doesn't not support padding with
    * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
    * as they require a pair of <code>char</code>s to be represented.
    * If you are needing to support full I18N of your applications
    * consider using {@link #repeat(String, int)} instead.
    * </p>
    *
    * @param repeat  number of times to repeat delim
    * @param padChar  character to repeat
    * @return String with repeated character
    * @throws IndexOutOfBoundsException if <code>repeat &lt; 0</code>
    * @see #repeat(String, int)
    */
   private static String padding(int repeat, char padChar) throws IndexOutOfBoundsException {
       if (repeat < 0) {
           throw new IndexOutOfBoundsException("Cannot pad a negative amount: " + repeat);
       }
       final char[] buf = new char[repeat];
       for (int i = 0; i < buf.length; i++) {
           buf[i] = padChar;
       }
       return new String(buf);
   }


   /**
    * <p>Right pad a String with a specified character.</p>
    *
    * <p>The String is padded to the size of <code>size</code>.</p>
    *
    * <pre>
    * StringUtils.rightPad(null, *, *)     = null
    * StringUtils.rightPad("", 3, 'z')     = "zzz"
    * StringUtils.rightPad("bat", 3, 'z')  = "bat"
    * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
    * StringUtils.rightPad("bat", 1, 'z')  = "bat"
    * StringUtils.rightPad("bat", -1, 'z') = "bat"
    * </pre>
    *
    * @param str  the String to pad out
    * @param size  the size to pad to
    * @param padChar  the character to pad with
    * @return right padded String or original String if no padding is necessary,
    *  <code>null</code> if null String input
    * @since 2.0
    */
   public static String pad(String str, int size, char padChar) {
       int pads = size - str.length();
       if (pads <= 0) {
           return str; // returns original String when possible
       }
       return str.concat(padding(pads, padChar));
   }

   /**
    * <p>Right pad a String with a specified String.</p>
    *
    * <p>The String is padded to the size of <code>size</code>.</p>
    *
    * <pre>
    * StringUtils.rightPad("", 3, "z")      = "zzz"
    * StringUtils.rightPad("bat", 3, "yz")  = "bat"
    * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
    * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
    * StringUtils.rightPad("bat", 1, "yz")  = "bat"
    * StringUtils.rightPad("bat", -1, "yz") = "bat"
    * StringUtils.rightPad("bat", 5, "")    = "bat  "
    * </pre>
    *
    * @param str  the String to pad out
    * @param size  the size to pad to
    * @param padStr  the String to pad with
    * @return right padded String or original String if no padding is necessary,
    *  <code>null</code> if null String input
    */
   public static String pad(String str, int size, String padStr) {

       int padLen = padStr.length();
       int strLen = str.length();
       int pads = size - strLen;
       if (pads <= 0) {
           return str; // returns original String when possible
       }

       if (pads == padLen) {
           return str.concat(padStr);
       } else if (pads < padLen) {
           return str.concat(padStr.substring(0, pads));
       } else {
           char[] padding = new char[pads];
           char[] padChars = padStr.toCharArray();
           for (int i = 0; i < pads; i++) {
               padding[i] = padChars[i % padLen];
           }
           return str.concat(new String(padding));
       }
   }


}

