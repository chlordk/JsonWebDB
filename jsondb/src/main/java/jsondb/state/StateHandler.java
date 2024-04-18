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

   public static final String SHARED = "shared";


   public static void initialize() throws Exception
   {
      String path = null;
      FileOutputStream pf = null;

      StateHandler.inst = Config.inst();
      StateHandler.path = Config.path(STATE);

      path = Config.path(STATE,inst);

      (new File(path)).mkdirs();
      (new StateHandler()).start();

      pid = ProcessHandle.current().pid();

      pf = new FileOutputStream(pidFile(inst));
      pf.write((pid+"").getBytes()); pf.close();

      Thread shutdown = new Thread(() -> StateHandler.pidFile(inst).delete());
      Runtime.getRuntime().addShutdownHook(shutdown);
   }

   public static synchronized String getSession(String session) throws Exception
   {
      File file = new File(sesspath(session));
      if (!file.exists()) return(null);

      file.setLastModified(System.currentTimeMillis());
      FileInputStream in = new FileInputStream(file);
      String username = new String(in.readAllBytes());
      in.close(); return(username);
   }


   public static synchronized String createSession(String username) throws Exception
   {
      String guid = null;
      boolean done = false;
      String session = null;

      while (!done)
      {
         FileOutputStream fout = null;
         guid = UUID.randomUUID().toString();

         guid = guid.replaceAll("\\:","_");
         guid = guid.replaceAll("\\.","_");

         session = inst+":"+guid;
         File file = new File(sesspath(session));

         if (!file.exists())
         {
            done = true;
            fout = new FileOutputStream(file);
            fout.write(username.getBytes()); fout.close();
         }
      }

      return(session);
   }


   public static boolean removeSession(String session)
   {
      File file = new File(sesspath(session));
      if (!file.exists()) return(false);

      file.delete();
      deletestate(session);

      return(true);
   }


   public static boolean touchSession(String session)
   {
      File file = new File(sesspath(session));
      if (!file.exists()) return(false);

      file.setLastModified((new Date()).getTime());
      return(true);
   }


   private static File pidFile(String inst)
   {
      return(new File(Config.path(STATE,inst+"."+PID)));
   }


   private static String sesspath(String session)
   {
      int pos = session.indexOf(':');
      String inst = session.substring(0,pos);
      String name = session.substring(pos+1);
      return(Config.path(STATE,inst,name+"."+SES));
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

            if (file.getName().equals(SHARED))
               continue;

            File[] sessions = file.listFiles();

            for(File session : sessions)
            {
               if (curr - file.lastModified() > sttl)
               {
                  String guid = session.getName();
                  guid = guid.substring(0,guid.length()-4);

                  session.delete();
                  deletestate(file.getName()+":"+guid);
               }
            }
         }
      }
   }


   private static void deletestate(String session)
   {
      int pos = session.indexOf(':');
      String inst = session.substring(0,pos);
      String guid = session.substring(pos+1);
      String path = Config.path(STATE,SHARED,inst+"."+guid);

      File state = new File(path);
      System.out.println("deletecursors "+path);

      if (state.exists())
      {
         for(File file : state.listFiles())
         {
            if (file.getName().endsWith("."+TRX))
               continue;

            file.delete();
         }
      }
   }
}
