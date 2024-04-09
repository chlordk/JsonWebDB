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


public class FileConfig
{
   private static final String FEXT = "ext";
   private static final String TYPE = "type";
   private static final String FILES = "files";
   private static final String MIMETYPES = "mimetypes";

   private final Config config;

   private final HashMap<String,String> mimetypes =
      new HashMap<String,String>();

   private static FileConfig instance = null;


   public static FileConfig load(Config config)
   {
      if (instance != null)
         return(instance);

      FileHandler.setConfig(config);
      instance = new FileConfig(config);
      
      return(instance);
   }


   public String getMimeType(String file)
   {
      String ext = file;

      int pos = file.lastIndexOf("/");
      if (pos >= 0) file = file.substring(pos+1);

      pos = file.lastIndexOf('.');
      if (pos >= 0) ext = file.substring(pos+1);

      return(mimetypes.get(ext));
   }

   private FileConfig(Config config)
   {
      this.config = config;
      JSONObject def = config.get(FILES);
      loadMimeTypes(def);
   }

   private void loadMimeTypes(JSONObject def)
   {
      JSONArray types = config.get(def,MIMETYPES);

      for (int i = 0; i < types.length(); i++)
      {
         JSONObject mt = types.getJSONObject(i);
         mimetypes.put(mt.getString(FEXT).toLowerCase(),mt.getString(TYPE));
      }
   }
}
