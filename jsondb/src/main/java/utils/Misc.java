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

package utils;

public class Misc
{
   public static String url(String ...parts)
   {
      String path = "";
      boolean url = parts[0].indexOf("://") > 0;

      for (int i = 0; i < parts.length; i++)
      {
         if (!url || i > 0)
         {
            if (parts[i].startsWith("/"))
               parts[i] = parts[i].substring(1);
         }

         if (parts[i].endsWith("/"))
            parts[i] = parts[i].substring(0,parts[i].length()-1);

         if (url && i == 0) path = parts[i];
         else path += "/" + parts[i];
      }
      return(path);
   }
}
