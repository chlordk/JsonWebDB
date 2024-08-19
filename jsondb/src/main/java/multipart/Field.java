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

public class Field
{
   private final String name;
   private final byte[] content;
   private final String filename;


   Field(String header, byte[] content)
   {
      int pos1 = 0;
      int pos2 = 0;
      String name = null;
      String filename = null;

      pos1 = header.indexOf("name=");

      if (pos1 >= 0)
      {
        pos1 += 6;
        pos2 = header.indexOf('"',pos1);
        if (pos2 >= 0) name = header.substring(pos1,pos2);
      }

      pos1 = header.indexOf("filename=");

      if (pos1 >= 0)
      {
        pos1 += 10;
        pos2 = header.indexOf('"',pos1);
        if (pos2 >= 0) filename = header.substring(pos1,pos2);
      }

      this.name = name;
      this.content = content;
      this.filename = filename;
   }


   public String name()
   {
      return(name);
   }


   public String filename()
   {
      return(filename);
   }


   public byte[] content()
   {
      return(content);
   }
}
