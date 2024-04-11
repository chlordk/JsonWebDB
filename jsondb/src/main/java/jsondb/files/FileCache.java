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
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileCache extends Thread
{
   private static FileCache instance = null;

   private final int check;
   private final int grace;
   private final String appl;
   private final Logger logger;
   private final ArrayList<String> ignore;


   public static FileCache observe(Config config) throws Exception
   {
      if (instance != null)
         return(instance);

      instance = new FileCache(config);

      instance.deploy();
      instance.start();

      return(instance);
   }

   private FileCache(Config config)
   {
      this.setDaemon(true);
      this.setName("deployment");

      this.appl = config.appl();
      this.logger = config.logger();
      this.check = FileConfig.check();
      this.grace = FileConfig.grace();
      this.ignore = FileConfig.ignore();
   }

   @Override
   public void run()
   {
      try {Thread.sleep(check);}
      catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}

      while (true)
      {
         try
         {
            deploy();
            Thread.sleep(check);
         }
         catch (Throwable e)
         {
            logger.log(Level.SEVERE,e.getMessage(),e);
         }
      }
   }

   private boolean deploy() throws Exception
   {
      ArrayList<File> files = list(new File(appl));
      for(File file : files) logger.info(file.getName());
      return(true);
   }

   private ArrayList<File> list(File root)
   {
      ArrayList<File> files = new ArrayList<File>();

      for(File file : root.listFiles())
      {
         if (file.isFile())
         {
            boolean skip = false;

            for (int i = 0; i < ignore.size(); i++)
            {
               if (file.getName().matches(ignore.get(i)))
               {
                  skip = true;
                  break;
               }
            }

            if (!skip) files.add(file);
         }

         if (file.isDirectory() && !file.isHidden())
            files.addAll(list(file));
      }

      return(files);
   }
}
