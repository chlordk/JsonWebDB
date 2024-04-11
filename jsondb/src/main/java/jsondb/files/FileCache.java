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
import java.util.concurrent.ConcurrentHashMap;


public class FileCache
{
   private static ConcurrentHashMap<String,CacheEntry> cache =
      new ConcurrentHashMap<String,CacheEntry>();


   public static CacheEntry get(String path)
   {
      File file = new File(path);
      CacheEntry centry = cache.get(path);

      if (centry != null)
      {
         if (!file.exists())
         {
            cache.remove(path);
            return(null);
         }

         if (file.lastModified() != centry.modified)
         {
            System.out.println("reload "+path);
         }
      }
      else
      {
         if (FileConfig.cache(file))
         {
            System.out.println("cache "+path);
         }
         else if (FileConfig.compress(file))
         {
            System.out.println("compress "+path);
         }
      }

      return(centry);
   }


   public static class CacheEntry
   {
      public final boolean gzip;
      public final long modified;

      private CacheEntry(File file, boolean gzip)
      {
         this.gzip = gzip;
         this.modified = file.lastModified();
      }
   }
}
