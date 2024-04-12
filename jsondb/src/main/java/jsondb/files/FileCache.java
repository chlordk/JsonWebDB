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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.ConcurrentHashMap;


public class FileCache
{
   private static ConcurrentHashMap<String,CacheEntry> cache =
      new ConcurrentHashMap<String,CacheEntry>();


   public static CacheEntry get(String path) throws Exception
   {
      CacheEntry centry = cache.get(path);

      if (centry != null)
      {
         if (!centry.ensure())
         {
            cache.remove(path);
            return(null);
         }
      }
      else
      {
         File file = open(path);

         if (FileConfig.cache(file))
         {
            centry = new CacheEntry(path,false);
            cache.put(path,centry);
         }
         else if (FileConfig.compress(file))
         {
            centry = new CacheEntry(path,true);
            cache.put(path,centry);
         }
      }

      return(centry);
   }

   private static File open(String path)
   {
      return(new File(FileConfig.root() + path));
   }

   public static class CacheEntry
   {
      private long size;
      private String path;
      private long modified;
      private byte[] content;
      private boolean gzipped;

      private CacheEntry(String path, boolean gzip) throws Exception
      {
         File file = open(path);

         this.path = path;
         this.gzipped = gzip;
         this.content = readFile(file);
         this.modified = file.lastModified();
      }

      public String path()
      {
         return(path);
      }

      public long bytes()
      {
         return(size);
      }

      public boolean gzipped()
      {
         return(gzipped);
      }

      public byte[] content()
      {
         return(content);
      }

      private boolean ensure() throws Exception
      {
         File file = open(path);
         if (!file.exists()) return(false);

         if (file.lastModified() != modified)
         {
            this.content = readFile();
            this.modified = file.lastModified();
         }

         return(true);
      }

      private byte[] readFile() throws Exception
      {
         return(readFile(open(path)));
      }

      private byte[] readFile(File file) throws Exception
      {
         FileInputStream in = new FileInputStream(file);
         byte[] content = in.readAllBytes(); in.close();

         this.size = content.length;

         if (this.gzipped)
         {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gout = new GZIPOutputStream(out);
            gout.write(content); gout.close(); out.close();
            content = out.toByteArray();
         }

         return(content);
      }

      private static File open(String path)
      {
         return(FileCache.open(path));
      }
   }
}
