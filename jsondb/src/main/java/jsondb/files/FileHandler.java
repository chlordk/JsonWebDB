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
import jsondb.Response;
import jsondb.files.FileCache.CacheEntry;

import java.io.OutputStream;
import java.io.FileInputStream;


public class FileHandler
{
   private static Config config = null;


   public static void setConfig(Config config)
   {
      FileHandler.config = config;
   }


   public Response load(String path, OutputStream out) throws Exception
   {
      String mime = config.getMimeType(path);
      CacheEntry centry = FileCache.get(path);

      path = config.appl() + path;
      long bytes = readFile(path,out);

      Response response = new Response(path,out,bytes,mime);
      return(response);
   }


   private long readFile(String path, OutputStream out) throws Exception
   {
      int read = 0;
      long bytes = 0;

      File file = new File(path);
      byte[] buf = new byte[8192];

      if (file.exists())
      {
         FileInputStream in = new FileInputStream(file);

         while((read = in.read(buf)) >= 0)
         {
            bytes += read;
            out.write(buf,0,read);
         }

         in.close();
      }

      return(bytes);
   }
}
