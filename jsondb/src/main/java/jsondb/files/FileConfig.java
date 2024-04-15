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
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class FileConfig
{
   private static final String FILES = "files";

   private static final String CACHE = "cache";
   private static final String SMALL = "small";
   private static final String LARGE = "large";
   private static final String MIMETYPES = "mimetypes";

   private static final String PATTERN = "pattern";
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
      JSONObject cache = Config.get(files,CACHE);

      loadMimeTypes(files);
      loadCacheRules(cache);
      loadCompressionRules(cache);
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

      return(mimetypes.get(ext));
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

   private static void loadCacheRules(JSONObject def)
   {
      cache.clear();
      JSONArray rules = Config.get(def,SMALL);

      for (int i = 0; i < rules.length(); i++)
      {
         JSONObject rule = rules.getJSONObject(i);
         cache.add(new FilePattern(rule.getString(PATTERN),rule.getLong(MAXSIZE)));
      }
   }

   private static void loadCompressionRules(JSONObject def)
   {
      compress.clear();
      JSONArray rules = Config.get(def,LARGE);

      for (int i = 0; i < rules.length(); i++)
      {
         JSONObject rule = rules.getJSONObject(i);
         compress.add(new FilePattern(rule.getString(PATTERN),rule.getLong(MINSIZE)));
      }
   }
}
