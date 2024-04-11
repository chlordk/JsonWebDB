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
import java.util.ArrayList;
import java.util.logging.Logger;


public class FileCache
{
   private static FileCache instance = null;

   private final String appl;
   private final Logger logger;

   public static void main(String[] args) throws Exception
   {
      Config config = Config.load(args[0],args[1]);
      FileCache cache = new FileCache(config);
      cache.deploy();
   }

   public static FileCache load(Config config) throws Exception
   {
      if (instance != null)
         return(instance);

      instance = new FileCache(config);
      return(instance);
   }

   private FileCache(Config config)
   {
      this.appl = config.appl();
      this.logger = config.logger();
   }

   private boolean deploy() throws Exception
   {
      ArrayList<File> files = list(new File(appl));

      for(File file : files)
      {
         if (FileConfig.compress(file.getName(),file.length()))
            logger.info("compress "+file.getName());

         if (FileConfig.cache(file.getName(),file.length()))
            logger.info("cache "+file.getName());
      }

      return(true);
   }

   private ArrayList<File> list(File root)
   {
      ArrayList<File> files = new ArrayList<File>();

      for(File file : root.listFiles())
      {
         if (file.isDirectory() && !file.isHidden())
            files.addAll(list(file));
      }

      return(files);
   }
}
