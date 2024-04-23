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
import utils.JSONOObject;
import org.json.JSONArray;
import org.json.JSONObject;

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


   public static JSONObject list(String path) throws Exception
   {
      File root = new File(path);
      JSONOObject response = new JSONOObject();

      if (root.exists())
      {
         File[] state = root.listFiles();

         JSONArray processes = new JSONArray();
         response.put("processes",processes);

         for(File file : state)
         {
            if (file.getName().endsWith("."+PID))
            {
               String inst = file.getName();
               inst = inst.substring(0,inst.length()-PID.length()-1);

               FileInputStream in = new FileInputStream(file);
               String pid = new String(in.readAllBytes()); in.close();

               JSONOObject entry = new JSONOObject();
               entry.put("instance",inst);
               entry.put("process#",pid);
               processes.put(entry);
            }
         }

         JSONArray sessions = new JSONArray();
         response.put("sessions",sessions);

         for(File file : state)
         {
            if (!file.isDirectory())
               continue;

            JSONOObject entry = new JSONOObject();
            sessions.put(entry);

            File session = sesFile(root,file.getName());
            SessionInfo info = new SessionInfo(session);

            entry.put("session",info.guid);
            entry.put("accessed",info.age+" secs");
            entry.put("instance",info.inst);
            entry.put("username",info.user);

            /* Add cursors and transcactions
            File[] content = session.getParentFile().listFiles();

            for(File child : content)
            {
            }
            */
         }
      }

      return(response);
   }

   public static SessionInfo getSession(String session) throws Exception
   {
      File file = sesFile(session);
      if (!file.exists()) return(null);
      SessionInfo info = new SessionInfo(file);
      file.setLastModified(System.currentTimeMillis());
      return(info);
   }


   public static String createSession(String username, boolean dedicated) throws Exception
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
            out.write((username+" "+inst+" "+dedicated).getBytes()); out.close();
         }
      }

      return(session);
   }


   public static boolean removeSession(String session)
   {
      File file = sesFile(session);
      if (!file.exists()) return(false);

      file = file.getParentFile();

      File[] content = file.listFiles();
      for(File child : content) child.delete();

      file.delete();
      return(true);
   }


   public static boolean touchSession(String session)
   {
      File file = sesFile(session);
      if (!file.exists()) return(false);

      file.setLastModified((new Date()).getTime());
      return(true);
   }


   public static boolean createTransaction(String session, String inst) throws Exception
   {
      File file = trxFile(session);
      if (file.exists()) return(false);

      FileOutputStream out = new FileOutputStream(file);
      out.write((inst+" "+StateHandler.pid).getBytes()); out.close();

      return(true);
   }


   public static TransactionInfo touchTransaction(String session, Date start) throws Exception
   {
      TransactionInfo info = null;
      File file = trxFile(session);

      if (file.exists())
      {
         info = new TransactionInfo(file);
         file.setLastModified(start.getTime());
      }

      return(info);
   }


   public static TransactionInfo getTransaction(String session) throws Exception
   {
      TransactionInfo info = null;
      File file = trxFile(session);

      if (file.exists())
         info = new TransactionInfo(file);

      return(info);
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


   private static File sesFile(File parent, String session)
   {
      String path = parent.getPath()+File.separator+session;
      return(new File(path+File.separator+session+"."+SES));
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
      int timeout = Config.timeout() * 1000;
      int interval = timeout > MAXINT*2 ? MAXINT : (int) (3.0/4*timeout);
      Config.logger().info(this.getClass().getSimpleName()+" running every "+interval/1000+" secs");

      while (true)
      {
         try
         {
            cleanout(timeout);
            Thread.sleep(interval);
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.getMessage(),t);
         }
      }
   }


   private void cleanout(int timeout)
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

            if (curr - session.lastModified() > timeout)
            {
               File folder = session.getParentFile();

               File[] content = folder.listFiles();
               for(File child : content) child.delete();

               session.getParentFile().delete();
            }
         }
      }
   }


   public static class SessionInfo
   {
      public final long age;
      public final String guid;
      public final String user;
      public final String inst;
      public final boolean dedicated;

      private SessionInfo(File file) throws Exception
      {
         String guid = file.getName();
         long now = (new Date()).getTime();

         this.age = (int) (now - file.lastModified())/1000;
         this.guid = guid.substring(0,guid.length()-SES.length()-1);

         FileInputStream in = new FileInputStream(file);
         String content = new String(in.readAllBytes());
         in.close();

         String[] args = content.split(" ");

         this.user = args[0];
         this.inst = args[1];

         this.dedicated = Boolean.parseBoolean(args[2]);
      }
   }


   public static class TransactionInfo
   {
      public final long age;
      public final long pid;
      public final String guid;
      public final String user;
      public final String inst;

      private TransactionInfo(File file) throws Exception
      {
         String guid = file.getName();
         long now = (new Date()).getTime();

         this.age = (int) (now - file.lastModified())/1000;
         this.guid = guid.substring(0,guid.length()-SES.length()-1);

         FileInputStream in = new FileInputStream(file);
         String content = new String(in.readAllBytes());
         in.close();

         String[] args = content.split(" ");

         this.user = args[0];
         this.inst = args[1];

         this.pid = Long.parseLong(args[2]);
      }
   }
}