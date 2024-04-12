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

package jsondb.files;

import java.io.File;
import jsondb.Config;
import java.io.FileInputStream;
import jsondb.files.FileCache.CacheEntry;


public class FileHandler
{
   private static Config config = null;


   public static void setConfig(Config config)
   {
      FileHandler.config = config;
   }


   public Response get(String path) throws Exception
   {
      Response response = null;

      String mime = config.getMimeType(path);
      CacheEntry centry = FileCache.get(path);

      if (centry == null)
      {
         byte[] content = readFile(path);
         response = new Response(path,content,mime);
      }
      else
      {
         response = new Response(centry,mime);
      }

      return(response);
   }


   private byte[] readFile(String path) throws Exception
   {
      byte[] content = null;
      File file = new File(config.appl() + path);


      if (file.exists())
      {
         FileInputStream in = new FileInputStream(file);
         content = in.readAllBytes();
         in.close();
      }

      return(content);
   }
}
