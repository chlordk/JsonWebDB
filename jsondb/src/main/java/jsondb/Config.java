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

package jsondb;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
   private final JSONObject config;

   private static String root = null;
   private static Config instance = null;

   
   public static synchronized void load(String root) throws Exception
   {
      Config.root = root;
      FileInputStream in = new FileInputStream(path("config.json"));
      Config.instance  = new Config(new JSONObject(new JSONTokener(in)));
   }

   private Config(JSONObject config)
   {
      this.config = config;
   }

   public Config get()
   {
      return(instance);
   }

   @SuppressWarnings({ "unchecked" })
   public <T> T get(String section)
   {
      return((T) config.get(section));
   }

   @SuppressWarnings({ "unchecked" })
   public <T> T get(JSONObject section, String attr)
   {
      return((T) section.get(attr));
   }

   public static String path(String... parts)
   {
      String path = root;

      for (int i = 0; i < parts.length; i++)
         path += File.separator + parts[i];

      return(path);
   }
}
