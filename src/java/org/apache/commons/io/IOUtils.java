/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * General IO Stream manipulation.
 * <p>
 * This class provides static utility methods for input/output operations, particularly buffered
 * copying between sources (<code>InputStream</code>, <code>Reader</code>, <code>String</code> and
 * <code>byte[]</code>) and destinations (<code>OutputStream</code>, <code>Writer</code>,
 * <code>String</code> and <code>byte[]</code>).
 * </p>
 *
 * <p>Unless otherwise noted, these <code>copy</code> methods do <em>not</em> flush or close the
 * streams. Often, doing so would require making non-portable assumptions about the streams' origin
 * and further use. This means that both streams' <code>close()</code> methods must be called after
 * copying. if one omits this step, then the stream resources (sockets, file descriptors) are
 * released when the associated Stream is garbage-collected. It is not a good idea to rely on this
 * mechanism. For a good overview of the distinction between "memory management" and "resource
 * management", see <a href="http://www.unixreview.com/articles/1998/9804/9804ja/ja.htm">this
 * UnixReview article</a></p>
 *
 * <p>For each <code>copy</code> method, a variant is provided that allows the caller to specify the
 * buffer size (the default is 4k). As the buffer size can have a fairly large impact on speed, this
 * may be worth tweaking. Often "large buffer -&gt; faster" does not hold, even for large data
 * transfers.</p>
 *
 * <p>For byte-to-char methods, a <code>copy</code> variant allows the encoding to be selected
 * (otherwise the platform default is used).</p>
 *
 * <p>The <code>copy</code> methods use an internal buffer when copying. It is therefore advisable
 * <em>not</em> to deliberately wrap the stream arguments to the <code>copy</code> methods in
 * <code>Buffered*</code> streams. For example, don't do the
 * following:</p>
 *
 * <code>copy( new BufferedInputStream( in ), new BufferedOutputStream( out ) );</code>
 *
 * <p>The rationale is as follows:</p>
 *
 * <p>Imagine that an InputStream's read() is a very expensive operation, which would usually suggest
 * wrapping in a BufferedInputStream. The BufferedInputStream works by issuing infrequent
 * {@link java.io.InputStream#read(byte[] b, int off, int len)} requests on the underlying InputStream, to
 * fill an internal buffer, from which further <code>read</code> requests can inexpensively get
 * their data (until the buffer runs out).</p>
 * <p>However, the <code>copy</code> methods do the same thing, keeping an internal buffer,
 * populated by {@link InputStream#read(byte[] b, int off, int len)} requests. Having two buffers
 * (or three if the destination stream is also buffered) is pointless, and the unnecessary buffer
 * management hurts performance slightly (about 3%, according to some simple experiments).</p>
 *
 * <p>Behold, intrepid explorers; a map of this class:</p>
 * <pre>
 *       Method      Input               Output          Dependency
 *       ------      -----               ------          -------
 * 1     copy        InputStream         OutputStream    (primitive)
 * 2     copy        Reader              Writer          (primitive)
 *
 * 3     copy        InputStream         Writer          2
 * 4     toString    InputStream         String          3
 * 5     toByteArray InputStream         byte[]          1
 *
 * 6     copy        Reader              OutputStream    2
 * 7     toString    Reader              String          2
 * 8     toByteArray Reader              byte[]          6
 *
 * 9     copy        String              OutputStream    2
 * 10    copy        String              Writer          (trivial)
 * 11    toByteArray String              byte[]          9
 *
 * 12    copy        byte[]              Writer          3
 * 13    toString    byte[]              String          12
 * 14    copy        byte[]              OutputStream    (trivial)
 * </pre>
 *
 * <p>Note that only the first two methods shuffle bytes; the rest use these
 * two, or (if possible) copy using native Java copy methods. As there are
 * method variants to specify buffer size and encoding, each row may
 * correspond to up to 4 methods.</p>
 *
 * <p>Origin of code: Apache Avalon (Excalibur)</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @version CVS $Revision: 1.9 $ $Date: 2003/12/30 06:50:16 $
 */
public final class IOUtils
{
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public IOUtils() {}

    /**
     * Unconditionally close an <code>Reader</code>.
     * Equivalent to {@link Reader#close()}, except any exceptions will be ignored.
     *
     * @param input A (possibly null) Reader
     */
    public static void closeQuietly( Reader input )
    {
        if( input == null )
        {
            return;
        }

        try
        {
            input.close();
        }
        catch( IOException ioe )
        {
        }
    }

    /**
     * Unconditionally close an <code>Writer</code>.
     * Equivalent to {@link Writer#close()}, except any exceptions will be ignored.
     *
     * @param output A (possibly null) Writer
     */
    public static void closeQuietly( Writer output )
    {
        if( output == null )
        {
            return;
        }

        try
        {
            output.close();
        }
        catch( IOException ioe )
        {
        }
    }

    /**
     * Unconditionally close an <code>OutputStream</code>.
     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
     * @param output A (possibly null) OutputStream
     */
    public static void closeQuietly( OutputStream output )
    {
        if( output == null )
        {
            return;
        }

        try
        {
            output.close();
        }
        catch( IOException ioe )
        {
        }
    }

    /**
     * Unconditionally close an <code>InputStream</code>.
     * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
     * @param input A (possibly null) InputStream
     */
    public static void closeQuietly( InputStream input )
    {
        if( input == null )
        {
            return;
        }

        try
        {
            input.close();
        }
        catch( IOException ioe )
        {
        }
    }

    ///////////////////////////////////////////////////////////////
    // Core copy methods
    ///////////////////////////////////////////////////////////////

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, OutputStream)}
     */
    public static int copy( InputStream input, OutputStream output )
        throws IOException
    {
        return copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, OutputStream, int)}
     */
    public static int copy( InputStream input,
                             OutputStream output,
                             int bufferSize )
        throws IOException
    {
        byte[] buffer = new byte[ bufferSize ];
        int count = 0;
        int n = 0;
        while( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
            count += n;
        }
        return count;
    }

    /**
     * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @return the number of characters copied
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(Reader, Writer)}
     */
    public static int copy( Reader input, Writer output )
        throws IOException
    {
        return copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @return the number of characters copied
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(Reader, Writer, int)}
     */
    public static int copy( Reader input, Writer output, int bufferSize )
        throws IOException
    {
        char[] buffer = new char[ bufferSize ];
        int count = 0;
        int n = 0;
        while( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
            count += n;
        }
        return count;
    }

    ///////////////////////////////////////////////////////////////
    // Derived copy methods
    // InputStream -> *
    ///////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////
    // InputStream -> Writer

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, Writer)}
     */
    public static void copy( InputStream input, Writer output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, Writer, int)}
     */
    public static void copy( InputStream input, Writer output, int bufferSize )
        throws IOException
    {
        InputStreamReader in = new InputStreamReader( input );
        copy( in, output, bufferSize );
    }

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     * <a href="http://www.iana.org/assignments/character-sets">IANA
     * Charset Registry</a> for a list of valid encoding types.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, Writer, String)}
     */
    public static void copy( InputStream input, Writer output, String encoding )
        throws IOException
    {
        InputStreamReader in = new InputStreamReader( input, encoding );
        copy( in, output );
    }

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     *        <a href="http://www.iana.org/assignments/character-sets">IANA
     *        Charset Registry</a> for a list of valid encoding types.
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(InputStream, Writer, String, int)}
     */
    public static void copy( InputStream input,
                             Writer output,
                             String encoding,
                             int bufferSize )
        throws IOException
    {
        InputStreamReader in = new InputStreamReader( input, encoding );
        copy( in, output, bufferSize );
    }


    ///////////////////////////////////////////////////////////////
    // InputStream -> String

    /**
     * Get the contents of an <code>InputStream</code> as a String.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the <code>InputStream</code> to read from
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( InputStream input )
        throws IOException
    {
        return toString( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of an <code>InputStream</code> as a String.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the <code>InputStream</code> to read from
     * @param bufferSize Size of internal buffer to use.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( InputStream input, int bufferSize )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        copy( input, sw, bufferSize );
        return sw.toString();
    }

    /**
     * Get the contents of an <code>InputStream</code> as a String.
     * @param input the <code>InputStream</code> to read from
     * @param encoding The name of a supported character encoding. See the
     *    <a href="http://www.iana.org/assignments/character-sets">IANA
     *    Charset Registry</a> for a list of valid encoding types.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( InputStream input, String encoding )
        throws IOException
    {
        return toString( input, encoding, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of an <code>InputStream</code> as a String.
     * @param input the <code>InputStream</code> to read from
     * @param encoding The name of a supported character encoding. See the
     *   <a href="http://www.iana.org/assignments/character-sets">IANA
     *   Charset Registry</a> for a list of valid encoding types.
     * @param bufferSize Size of internal buffer to use.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( InputStream input,
                                   String encoding,
                                   int bufferSize )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        copy( input, sw, encoding, bufferSize );
        return sw.toString();
    }

    ///////////////////////////////////////////////////////////////
    // InputStream -> byte[]

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( InputStream input )
        throws IOException
    {
        return toByteArray( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * @param input the <code>InputStream</code> to read from
     * @param bufferSize Size of internal buffer to use.
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( InputStream input, int bufferSize )
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy( input, output, bufferSize );
        return output.toByteArray();
    }


    ///////////////////////////////////////////////////////////////
    // Derived copy methods
    // Reader -> *
    ///////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////
    // Reader -> OutputStream
    /**
     * Serialize chars from a <code>Reader</code> to bytes on an <code>OutputStream</code>, and
     * flush the <code>OutputStream</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(Reader, OutputStream)}
     */
    public static void copy( Reader input, OutputStream output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Serialize chars from a <code>Reader</code> to bytes on an <code>OutputStream</code>, and
     * flush the <code>OutputStream</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(Reader, OutputStream, int)}
     */
    public static void copy( Reader input, OutputStream output, int bufferSize )
        throws IOException
    {
        OutputStreamWriter out = new OutputStreamWriter( output );
        copy( input, out, bufferSize );
        // NOTE: Unless anyone is planning on rewriting OutputStreamWriter, we have to flush
        // here.
        out.flush();
    }

    ///////////////////////////////////////////////////////////////
    // Reader -> String
    /**
     * Get the contents of a <code>Reader</code> as a String.
     * @param input the <code>Reader</code> to read from
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( Reader input )
        throws IOException
    {
        return toString( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of a <code>Reader</code> as a String.
     * @param input the <code>Reader</code> to read from
     * @param bufferSize Size of internal buffer to use.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( Reader input, int bufferSize )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        copy( input, sw, bufferSize );
        return sw.toString();
    }


    ///////////////////////////////////////////////////////////////
    // Reader -> byte[]
    /**
     * Get the contents of a <code>Reader</code> as a <code>byte[]</code>.
     * @param input the <code>Reader</code> to read from
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( Reader input )
        throws IOException
    {
        return toByteArray( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of a <code>Reader</code> as a <code>byte[]</code>.
     * @param input the <code>Reader</code> to read from
     * @param bufferSize Size of internal buffer to use.
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( Reader input, int bufferSize )
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy( input, output, bufferSize );
        return output.toByteArray();
    }


    ///////////////////////////////////////////////////////////////
    // Derived copy methods
    // String -> *
    ///////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////
    // String -> OutputStream

    /**
     * Serialize chars from a <code>String</code> to bytes on an <code>OutputStream</code>, and
     * flush the <code>OutputStream</code>.
     * @param input the <code>String</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(String, OutputStream)}
     */
    public static void copy( String input, OutputStream output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Serialize chars from a <code>String</code> to bytes on an <code>OutputStream</code>, and
     * flush the <code>OutputStream</code>.
     * @param input the <code>String</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(String, OutputStream, int)}
     */
    public static void copy( String input, OutputStream output, int bufferSize )
        throws IOException
    {
        StringReader in = new StringReader( input );
        OutputStreamWriter out = new OutputStreamWriter( output );
        copy( in, out, bufferSize );
        // NOTE: Unless anyone is planning on rewriting OutputStreamWriter, we have to flush
        // here.
        out.flush();
    }



    ///////////////////////////////////////////////////////////////
    // String -> Writer

    /**
     * Copy chars from a <code>String</code> to a <code>Writer</code>.
     * @param input the <code>String</code> to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(String, Writer)}
     */
    public static void copy( String input, Writer output )
        throws IOException
    {
        output.write( input );
    }

    ///////////////////////////////////////////////////////////////
    // String -> byte[]
    /**
     * Get the contents of a <code>String</code> as a <code>byte[]</code>.
     * @param input the <code>String</code> to convert
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( String input )
        throws IOException
    {
        return toByteArray( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of a <code>String</code> as a <code>byte[]</code>.
     * @param input the <code>String</code> to convert
     * @param bufferSize Size of internal buffer to use.
     * @return the requested byte array
     * @throws IOException In case of an I/O problem
     */
    public static byte[] toByteArray( String input, int bufferSize )
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy( input, output, bufferSize );
        return output.toByteArray();
    }



    ///////////////////////////////////////////////////////////////
    // Derived copy methods
    // byte[] -> *
    ///////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////
    // byte[] -> Writer

    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], Writer)}
     */
    public static void copy( byte[] input, Writer output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], Writer, int)}
     */
    public static void copy( byte[] input, Writer output, int bufferSize )
        throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream( input );
        copy( in, output, bufferSize );
    }

    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     * <a href="http://www.iana.org/assignments/character-sets">IANA
     * Charset Registry</a> for a list of valid encoding types.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], Writer, String)}
     */
    public static void copy( byte[] input, Writer output, String encoding )
        throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream( input );
        copy( in, output, encoding );
    }

    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     *        <a href="http://www.iana.org/assignments/character-sets">IANA
     *        Charset Registry</a> for a list of valid encoding types.
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], Writer, String, int)}
     */
    public static void copy( byte[] input,
                             Writer output,
                             String encoding,
                             int bufferSize )
        throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream( input );
        copy( in, output, encoding, bufferSize );
    }


    ///////////////////////////////////////////////////////////////
    // byte[] -> String

    /**
     * Get the contents of a <code>byte[]</code> as a String.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the byte array to read from
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( byte[] input )
        throws IOException
    {
        return toString( input, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of a <code>byte[]</code> as a String.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the byte array to read from
     * @param bufferSize Size of internal buffer to use.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( byte[] input, int bufferSize )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        copy( input, sw, bufferSize );
        return sw.toString();
    }

    /**
     * Get the contents of a <code>byte[]</code> as a String.
     * @param input the byte array to read from
     * @param encoding The name of a supported character encoding. See the
     *    <a href="http://www.iana.org/assignments/character-sets">IANA
     *    Charset Registry</a> for a list of valid encoding types.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( byte[] input, String encoding )
        throws IOException
    {
        return toString( input, encoding, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Get the contents of a <code>byte[]</code> as a String.
     * @param input the byte array to read from
     * @param encoding The name of a supported character encoding. See the
     *   <a href="http://www.iana.org/assignments/character-sets">IANA
     *   Charset Registry</a> for a list of valid encoding types.
     * @param bufferSize Size of internal buffer to use.
     * @return the requested <code>String</code>
     * @throws IOException In case of an I/O problem
     */
    public static String toString( byte[] input,
                                   String encoding,
                                   int bufferSize )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        copy( input, sw, encoding, bufferSize );
        return sw.toString();
    }


    ///////////////////////////////////////////////////////////////
    // byte[] -> OutputStream

    /**
     * Copy bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
     * @param input the byte array to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], OutputStream)}
     */
    public static void copy( byte[] input, OutputStream output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
     * @param input the byte array to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize Size of internal buffer to use.
     * @throws IOException In case of an I/O problem
     * @deprecated Replaced by {@link CopyUtils#copy(byte[], OutputStream, int)}
     */
    public static void copy( byte[] input,
                             OutputStream output,
                             int bufferSize )
        throws IOException
    {
        output.write( input );
    }

    /**
     * Compare the contents of two Streams to determine if they are equal or not.
     *
     * @param input1 the first stream
     * @param input2 the second stream
     * @return true if the content of the streams are equal or they both don't exist, false otherwise
     * @throws IOException In case of an I/O problem
     */
    public static boolean contentEquals( InputStream input1,
                                         InputStream input2 )
        throws IOException
    {
        InputStream bufferedInput1 = new BufferedInputStream( input1 );
        InputStream bufferedInput2 = new BufferedInputStream( input2 );

        int ch = bufferedInput1.read();
        while( -1 != ch )
        {
            int ch2 = bufferedInput2.read();
            if( ch != ch2 )
            {
                return false;
            }
            ch = bufferedInput1.read();
        }

        int ch2 = bufferedInput2.read();
        if( -1 != ch2 )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
