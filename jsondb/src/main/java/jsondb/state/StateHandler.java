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

package jsondb.state;

import java.io.File;
import jsondb.Config;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class StateHandler extends Thread
{
   private static long pid;

   private static String inst = null;
   private static String path = null;

   private static final int MAXINT = 60000;

   private static final String PID = "pid";
   private static final String SES = "ses";
   private static final String TRX = "trx";
   private static final String STATE = "state";


   public static void initialize() throws Exception
   {
      FileOutputStream pf = null;

      StateHandler.inst = Config.inst();
      StateHandler.path = Config.path(STATE);

      (new File(path)).mkdirs();
      (new StateHandler()).start();

      StateHandler.pid = ProcessHandle.current().pid();

      pf = new FileOutputStream(pidFile(inst));
      pf.write((pid+"").getBytes()); pf.close();

      Thread shutdown = new Thread(() -> StateHandler.pidFile(inst).delete());
      Runtime.getRuntime().addShutdownHook(shutdown);
   }

   public static String getSession(String session) throws Exception
   {
      File file = sesFile(session);
      if (!file.exists()) return(null);

      file.setLastModified(System.currentTimeMillis());
      FileInputStream in = new FileInputStream(file);
      String username = new String(in.readAllBytes());
      in.close(); return(username);
   }


   public static String createSession(String username) throws Exception
   {
      boolean done = false;
      String session = null;

      while (!done)
      {
         FileOutputStream out = null;
         session = UUID.randomUUID().toString();

         File file = sesFile(session);

         if (!file.exists())
         {
            done = true;
            sesPath(session).mkdirs();
            out = new FileOutputStream(file);
            out.write(username.getBytes()); out.close();
         }
      }

      return(session);
   }


   public static boolean removeSession(String session)
   {
      File file = sesFile(session);
      if (!file.exists()) return(false);

      file.getParentFile().delete();
      return(true);
   }


   public static boolean touchSession(String session)
   {
      File file = sesFile(session);
      if (!file.exists()) return(false);

      file.setLastModified((new Date()).getTime());
      return(true);
   }


   public static String createTransaction(String session, String inst) throws Exception
   {
      String running = null;
      File file = trxFile(session);

      if (file.exists())
      {
         FileInputStream in = new FileInputStream(file);
         running = new String(in.readAllBytes()); in.close();
      }

      FileOutputStream out = new FileOutputStream(file);
      out.write(inst.getBytes()); out.close();

      return(running);
   }


   public static String getTransaction(String session) throws Exception
   {
      String running = null;
      File file = trxFile(session);

      if (file.exists())
      {
         FileInputStream in = new FileInputStream(file);
         running = new String(in.readAllBytes()); in.close();
      }

      return(running);
   }


   public static boolean removeTransaction(String session) throws Exception
   {
      File file = trxFile(session);
      if (file.exists()) return(false);
      return(true);
   }


   private static File pidFile(String inst)
   {
      return(new File(Config.path(STATE,inst+"."+PID)));
   }


   private static File trxFile(String session)
   {
      return(new File(Config.path(STATE,session+"."+TRX)));
   }


   private static File sesFile(String session)
   {
      return(new File(Config.path(STATE,session,session+"."+SES)));
   }


   private static File sesPath(String session)
   {
      return(new File(Config.path(STATE,session)));
   }


   private StateHandler()
   {
      this.setDaemon(true);
      this.setName(this.getClass().getName());
   }


   @Override
   public void run()
   {
      int sttl = Config.sesttl() * 1000;
      int wait = sttl > MAXINT*2 ? MAXINT : (int) (3.0/4*sttl);

      while (true)
      {
         try
         {
            cleanout(sttl);
            Thread.sleep(wait);
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.getMessage(),t);
         }
      }
   }


   private void cleanout(int sttl)
   {
      long curr = System.currentTimeMillis();
      File root = new File(StateHandler.path);

      if (root.exists())
      {
         for(File file : root.listFiles())
         {
            if (!file.isDirectory())
               continue;

            File session = sesFile(file.getName());

            if (curr - session.lastModified() > sttl)
            {
               File folder = session.getParentFile();

               File[] content = folder.listFiles();
               for(File child : content) child.delete();
               
               session.getParentFile().delete();
            }
         }
      }
   }
}
