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
package http;

import java.util.ArrayList;


public class AdminResponse
{
   public final int code;
   public final int size;
   public final String html;
   public final byte[] page;

   public final ArrayList<Header> headers =
      new ArrayList<Header>();


   public AdminResponse(String html)
   {
      this(200,html);
   }


   public AdminResponse(int code)
   {
      this(code,"");
   }


   public AdminResponse(int code, String html)
   {
      this.code = code;
      this.html = html;
      this.page = html.getBytes();
      this.size = page.length;
   }

   public AdminResponse setHeader(String name, String value)
   {
      headers.add(new Header(name,value));
      return(this);
   }


   public static class Header
   {
      public final String name;
      public final String value;

      public Header(String name, String value)
      {
         this.name = name;
         this.value = value;
      }
   }
}
