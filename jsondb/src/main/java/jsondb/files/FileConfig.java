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

import jsondb.Config;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class FileConfig
{
   private static final String FILES = "files";

   private static final String CACHE = "cache";
   private static final String IGNORE = "ignore";
   private static final String MIMETYPES = "mimetypes";
   private static final String DEPLOYMENT = "deployment";
   private static final String COMPRESSION = "compression";

   private static final String FEXT = "filetype";
   private static final String TYPE = "mimetype";

   private static final String GRACE = "grace-period";
   private static final String CHECK = "check-for-changes";

   private static final String PATTERN = "pattern";
   private static final String MINSIZE = "minsize";
   private static final String MAXSIZE = "maxsize";

   private final int check;
   private final int grace;
   private final Config config;

   private final ArrayList<String> ignore =
      new ArrayList<String>();

   private final ArrayList<FileSpec> cache =
      new ArrayList<FileSpec>();

   private final ArrayList<FileSpec> compression =
      new ArrayList<FileSpec>();

   private final HashMap<String,String> mimetypes =
      new HashMap<String,String>();

   private static FileConfig instance = null;


   public static FileConfig load(Config config)
   {
      if (instance != null)
         return(instance);

      FileHandler.setConfig(config);
      instance = new FileConfig(config);

      Deployment.observe(config);
      return(instance);
   }

   private FileConfig(Config config)
   {
      this.config = config;

      JSONObject files = config.get(FILES);
      JSONObject deploy = config.get(files,DEPLOYMENT);

      loadIgnore(deploy);
      loadMimeTypes(files);
      loadCacheRules(files);
      loadCompressionRules(files);

      this.check = deploy.getInt(CHECK) * 1000;
      this.grace = deploy.getInt(GRACE) * 1000;
   }

   public static int check()
   {
      return(instance.check);
   }

   public static int grace()
   {
      return(instance.grace);
   }

   public static ArrayList<String> ignore()
   {
      return(instance.ignore);
   }

   public static String getMimeType(String file)
   {
      String ext = file;

      int pos = file.lastIndexOf("/");
      if (pos >= 0) file = file.substring(pos+1);

      pos = file.lastIndexOf('.');
      if (pos >= 0) ext = file.substring(pos+1);

      return(instance.mimetypes.get(ext));
   }

   private void loadIgnore(JSONObject def)
   {
      ignore.clear();

      if (def.has(IGNORE))
      {
         JSONArray list = def.optJSONArray(IGNORE);

         for (int i = 0; i < list.length(); i++)
         {
            String pattern = list.getString(i);

            pattern = pattern.replace(".","\\.");
            pattern = pattern.replace("*",".*");

            ignore.add(pattern);
         }
      }
   }

   private void loadMimeTypes(JSONObject def)
   {
      mimetypes.clear();
      JSONArray types = config.get(def,MIMETYPES);

      for (int i = 0; i < types.length(); i++)
      {
         JSONObject mt = types.getJSONObject(i);
         mimetypes.put(mt.getString(FEXT).toLowerCase(),mt.getString(TYPE));
      }
   }

   private void loadCacheRules(JSONObject def)
   {
      cache.clear();
      JSONArray rules = config.get(def,CACHE);

      for (int i = 0; i < rules.length(); i++)
      {
         JSONObject rule = rules.getJSONObject(i);
         cache.add(new FileSpec(rule.getString(PATTERN),rule.getLong(MAXSIZE)));
      }
   }

   private void loadCompressionRules(JSONObject def)
   {
      compression.clear();
      JSONArray rules = config.get(def,COMPRESSION);

      for (int i = 0; i < rules.length(); i++)
      {
         JSONObject rule = rules.getJSONObject(i);
         compression.add(new FileSpec(rule.getString(PATTERN),rule.getLong(MINSIZE)));
      }
   }
}
