/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package misc;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextFinder
{
   private static Pattern pattern = Pattern.compile("\".{15,}\"");

   public static void main(String[] args) throws Exception
   {
     next(new File("/Users/alhof/Repository/JsonWebDB/jsondb/src"));
   }


   private static void next(File folder) throws Exception
   {
      File[] ls = folder.listFiles();

      for(File file : ls)
      {
         if (file.isDirectory()) next(file);
         else
         {
            FileInputStream in = new FileInputStream(file);
            String content = new String(in.readAllBytes());
            in.close();

            String fname = file.getName();
            Matcher matcher = pattern.matcher(content);

            while(matcher.find())
               System.out.println(fname+": "+content.substring(matcher.start(),matcher.end()));
         }
      }
   }
}