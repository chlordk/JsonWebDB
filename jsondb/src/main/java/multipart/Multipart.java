/*
MIT License

Copyright (c) 2024 Alex Høffner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package multipart;

import java.util.ArrayList;


public class Multipart
{
   private byte[] content;
   private byte[] boundary;

   private final ArrayList<Field> entries =
      new ArrayList<Field>();


   public Multipart(String ctype, byte[] content)
   {
      this.content = content;
      int pos = ctype.indexOf("boundary=");
      this.boundary = ("--"+ctype.substring(pos+9)).getBytes();
      parse();
   }


   public ArrayList<Field> fields()
   {
      return(entries);
   }


   void parse()
   {
      int next = 0;
      byte[] eoh = "\r\n\r\n".getBytes();

      if (content == null || boundary == null)
         return;

      while (true)
      {
         int last = next + 1;
         next = indexOf(content,boundary,next);

         if (next > last)
         {
            // newline is \r\n
            // 2 newlines after header
            // 1 newline after content

            int head = indexOf(content,eoh,last+boundary.length);
            String header = new String(content,last,head-last);

            head += 4;
            byte[] entry = new byte[next-head-2];
            System.arraycopy(content,head,entry,0,entry.length);

            Field field = new Field(header,entry);
            entries.add(field);
         }

         if (next == -1 || next + boundary.length + 4 == content.length)
            break;

         next += boundary.length + 1;
       }

       this.content = null;
       this.boundary = null;
   }


   private int indexOf(byte[] body, byte[] boundary, int start)
   {
      for(int i = start; i < body.length - boundary.length + 1; i++)
      {
         boolean found = true;

         for(int j = 0; j < boundary.length; ++j)
         {
            if (body[i+j] != boundary[j])
            {
               found = false;
               break;
            }
         }

         if (found) return(i);
      }

      return(-1);
   }
}
