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

import utils.GMTDate;
import jsondb.files.FileCache.CacheEntry;


public class FileResponse
{
   public final long size;
   public final String path;
   public final boolean gzip;
   public final long modified;
   public final byte[] content;
   public final String mimetype;

   public FileResponse(String path, byte[] content, String mimetype, long modified)
   {
      this.path = path;
      this.gzip = false;
      this.content = content;
      this.mimetype = mimetype;
      this.modified = modified;
      this.size = content == null ? 0 : content.length;
   }

   public FileResponse(CacheEntry entry, String mimetype) throws Exception
   {
      this.mimetype = mimetype;
      this.path = entry.path();
      this.size = entry.bytes();
      this.gzip = entry.gzipped();
      this.content = entry.content();
      this.modified = entry.modified();
   }

   public boolean exists()
   {
      return(modified != 0);
   }

   public String gmt()
   {
      return(GMTDate.format(modified));
   }

   @Override
   public String toString()
   {
      int len = path.length();
      String path = this.path;
      if (len > 40) path = "..."+path.substring(len-37);

      String mime = mimetype;
      mime = mime != null ? mime : "";

      len = mime.length();
      if (len > 15) mime = mime.substring(len-15);

      String desc = null;

      if (!exists())
      {
         desc = String.format("%-40s %s",path,"Page Not Found");
      }
      else
      {
         desc = String.format("%-40s %-15s %6dk",path,mime,size/1024);
         if (gzip) desc += " (gz)";
      }

      return(desc);
   }
}