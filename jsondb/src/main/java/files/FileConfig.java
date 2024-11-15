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

package files;

import java.io.File;
import jsondb.Config;
import messages.Messages;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class FileConfig
{
   private static final String FILES = "files";

   private static final String CACHE = "cache";
   private static final String COMPRESS = "compress";
   private static final String MIMETYPES = "mimetypes";

   private static final String MINSIZE = "minsize";
   private static final String MAXSIZE = "maxsize";

   private static final String FILETYPE = "filetype";
   private static final String MIMETYPE = "mimetype";

   private static final ArrayList<FilePattern> cache =
      new ArrayList<FilePattern>();

   private static final ArrayList<FilePattern> compress =
      new ArrayList<FilePattern>();

   private static final HashMap<String,String> mimetypes =
      new HashMap<String,String>();


   public static void initialize() throws Exception
   {
      JSONObject files = Config.get(FILES);
      JSONArray cache = Config.get(files,CACHE);
      JSONArray compr = Config.get(files,COMPRESS);

      loadMimeTypes(files);
      loadCacheRules(cache);
      loadCompressionRules(compr);
   }

   public static String root()
   {
      return(Config.appl());
   }

   public static boolean cache(File file)
   {
      for (int i = 0; i < cache.size(); i++)
      {
         if (cache.get(i).matchSmallFile(file.getName(),file.length()))
            return(true);
      }

      return(false);
   }

   public static boolean compress(File file)
   {
      for (int i = 0; i < compress.size(); i++)
      {
         if (compress.get(i).matchLargeFile(file.getName(),file.length()))
            return(true);
      }

      return(false);
   }

   public static String getMimeType(String file)
   {
      String ext = file;

      int pos = ext.lastIndexOf('.');
      if (pos >= 0) ext = ext.substring(pos+1);

		String mime = mimetypes.get(ext);

		if (mime == null)
		{
			mime = "application/octet-stream";
			Config.logger().warning(Messages.get("UNKNOWN_MIME_TYPE",ext));
		}

      return(mime);
   }

   private static void loadMimeTypes(JSONObject def)
   {
      mimetypes.clear();
      JSONArray types = Config.get(def,MIMETYPES);

      for (int i = 0; i < types.length(); i++)
      {
         JSONObject mt = types.getJSONObject(i);
         mimetypes.put(mt.getString(FILETYPE).toLowerCase(),mt.getString(MIMETYPE));
      }
   }

   private static void loadCacheRules(JSONArray def)
   {
      cache.clear();

      for (int i = 0; i < def.length(); i++)
      {
         JSONObject rule = def.getJSONObject(i);
         cache.add(new FilePattern(rule.getString(FILETYPE),rule.getLong(MAXSIZE)));
      }
   }


   private static void loadCompressionRules(JSONArray def)
   {
      compress.clear();

      for (int i = 0; i < def.length(); i++)
      {
         JSONObject rule = def.getJSONObject(i);
         compress.add(new FilePattern(rule.getString(FILETYPE),rule.getLong(MINSIZE)));
      }
   }
}