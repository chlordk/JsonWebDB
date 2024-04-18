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
   private static final String STATE = "state";


   public static void initialize() throws Exception
   {
      FileOutputStream pf = null;

      StateHandler.inst = Config.inst();
      StateHandler.path = Config.path(STATE,inst);

      (new File(path)).mkdirs();
      (new StateHandler()).start();

      pid = ProcessHandle.current().pid();
      pf = new FileOutputStream(path+File.separator+PID);
      pf.write((pid+"").getBytes()); pf.close();

      Thread shutdown = new Thread(() -> StateHandler.removePidFile());
      Runtime.getRuntime().addShutdownHook(shutdown);
   }

   public static void removePidFile()
   {
      File pf = new File(path+File.separator+PID);
      pf.delete();
   }

   public static synchronized String getSession(String session) throws Exception
   {
      session = getLocal(session);

      File file = new File(sesspath(session));
      if (!file.exists()) return(null);

      file.setLastModified(System.currentTimeMillis());
      FileInputStream in = new FileInputStream(file);
      String username = new String(in.readAllBytes());
      in.close(); return(username);
   }


   public static synchronized String createSession(String username) throws Exception
   {
      boolean done = false;
      String session = null;

      while (!done)
      {
         FileOutputStream fout = null;
         session = UUID.randomUUID().toString();

         session = session.replaceAll("\\:","_");
         session = session.replaceAll("\\.","_");

         File sess = new File(sesspath(session));

         if (!sess.exists())
         {
            done = true;
            fout = new FileOutputStream(sess);
            fout.write(username.getBytes()); fout.close();
         }
      }

      return(inst+":"+session);
   }


   public static boolean removeSession(String session)
   {
      session = getLocal(session);

      File file = new File(sesspath(session));
      if (!file.exists()) return(false);

      file.delete();
      deletecursors(session);

      return(true);
   }


   public static String getLocal(String session)
   {
      int pos = session.indexOf(':');
      if (pos <= 0) return(null);
      return(session.substring(pos+1));
   }


   public static String getInstance(String session)
   {
      int pos = session.indexOf(':');
      if (pos <= 0) return(null);
      return(session.substring(0,pos));
   }


   private static String sesspath(String session)
   {
      return(StateHandler.path+File.separator+session+"."+SES);
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
            if (file.isDirectory()) continue;
            if (file.getName().equals(PID)) continue;

            if (curr - file.lastModified() > sttl)
            {
               file.delete();
               deletecursors(file.getName());
            }
         }
      }
   }


   private static void deletecursors(String sess)
   {
      System.out.println("deletecursors "+sess);

      File root = new File(StateHandler.path);

      if (root.exists())
      {
         for(File file : root.listFiles())
         {
            int pos = file.getName().lastIndexOf(".c");
            if (pos < 0) continue;

            String name = file.getName().substring(0,pos);
            if (sess.equals(name)) file.delete();
         }
      }
   }
}
