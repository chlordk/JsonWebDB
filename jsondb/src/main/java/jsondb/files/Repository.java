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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.channels.FileChannel;


public class Repository
{
   private static File repos = null;
   private static Logger logger = null;


   public static void init(Config config)
   {
      Repository.logger = config.logger();
      String path = Config.path("deployment");

      Repository.repos = new File(path);
      if (!repos.exists()) repos.mkdirs();

      logger.info(path);
   }


   public static boolean owner()
   {
      try
      {
         //FileChannel
      }
      catch (Throwable e)
      {
         logger.log(Level.SEVERE,e.getMessage(),e);
      }

      return(true);
   }
}
