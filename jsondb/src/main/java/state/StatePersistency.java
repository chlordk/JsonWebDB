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

package state;

import utils.Guid;
import utils.Bytes;
import java.io.File;
import jsondb.Config;
import java.util.Date;
import utils.JSONOObject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;


public class StatePersistency
{
   private static long pid;

   private static String inst = null;
   private static String path = null;

   private static final String PID = "pid";
   private static final String SES = "ses";
   private static final String TRX = "trx";
   private static final String CUR = "cur";
   private static final String STATE = "state";
   private static final String INSTANCES = "instances";


   public static void initialize() throws Exception
   {
      FileOutputStream pf = null;

      StatePersistency.inst = Config.inst();
      StatePersistency.path = Config.path(STATE);
      StatePersistency.pid = ProcessHandle.current().pid();

      File folders = new File(Config.path(STATE,INSTANCES));
      if (!folders.exists()) folders.mkdirs();

      pf = new FileOutputStream(pidFile(inst));
      pf.write((pid+"").getBytes()); pf.close();

      Thread shutdown = new Thread(() -> StatePersistency.pidFile(inst).delete());
      Runtime.getRuntime().addShutdownHook(shutdown);
   }


   public static JSONObject list() throws Exception
   {
      JSONOObject response = new JSONOObject();

      File root = new File(Config.path(STATE));
      File proc = new File(Config.path(STATE,INSTANCES));

      if (root.exists())
      {
         File[] procs = proc.listFiles();
         File[] state = root.listFiles();

         JSONArray processes = new JSONArray();
         response.put("processes",processes);

         for(File file : procs)
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

            if (file.getName().equals(INSTANCES))
               continue;

            JSONOObject entry = new JSONOObject();
            sessions.put(entry);

            File session = sesFile(root,file.getName());
            SessionInfo info = new SessionInfo(session,inst);

            entry.put("pid",info.pid);
            entry.put("session",info.guid);
            entry.put("accessed",info.age+" secs");
            entry.put("instance",info.inst);
            entry.put("username",info.user);
            entry.put("online",info.online);

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


   public static long getPid(String inst) throws Exception
   {
      File file = pidFile(inst);
      if (!file.exists()) return(-1);

      FileInputStream in = new FileInputStream(file);
      String pid = new String(in.readAllBytes()); in.close();

      return(Long.parseLong(pid));
   }


   public static String createSession(String user, boolean stateful) throws Exception
   {
      String guid = null;
      boolean done = false;

      while (!done)
      {
         guid = Guid.generate();
         File file = sesFile(guid);

         if (!file.exists())
         {
            done = true;
            sesPath(guid).mkdirs();

            SessionInfo info = new SessionInfo(pid,guid,inst,user,stateful);
            info.save(file);
         }
      }

      return(guid);
   }


   public static SessionInfo getSession(String session) throws Exception
   {
      File file = sesFile(session);
      if (!file.exists()) return(null);
      SessionInfo info = new SessionInfo(file,inst);
      file.setLastModified(System.currentTimeMillis());
      return(info);
   }


   public static boolean touchSession(String session)
   {
      File file = sesFile(session);
      if (!file.exists()) return(false);

      file.setLastModified((new Date()).getTime());
      return(true);
   }


   public static void transferSession(String session, String user, boolean stateful) throws Exception
   {
      SessionInfo info = new SessionInfo(pid,session,inst,user,stateful);
      info.save(sesFile(session));
   }


   public static void releaseSession(String session) throws Exception
   {
      SessionInfo info = new SessionInfo(-1,session,"","",false);
      info.save(sesFile(session));
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


   public static boolean createTransaction(String session, String inst) throws Exception
   {
      File file = trxFile(session);
      if (file.exists()) return(false);

      FileOutputStream out = new FileOutputStream(file);
      out.write((inst+" "+StatePersistency.pid).getBytes()); out.close();

      return(true);
   }


   public static TransactionInfo getTransaction(String session) throws Exception
   {
      TransactionInfo info = null;
      File file = trxFile(session);

      if (file.exists())
         info = new TransactionInfo(file);

      return(info);
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


   public static boolean removeTransaction(String session) throws Exception
   {
      File file = trxFile(session);
      if (file.exists()) return(false);
      file.delete();
      return(true);
   }


   public static String createCursor(String sessid, long pos, int pagesize, JSONObject def) throws Exception
   {
      String guid = null;
      boolean done = false;

      while (!done)
      {
         guid = Guid.generate();
         File file = curFile(sessid,guid);

         if (!file.exists())
         {
            done = true;
            CursorInfo info = new CursorInfo(guid,pos,pagesize,def);
            info.save(file);
         }
      }

      return(guid);
   }


   public static CursorInfo getCursor(String sessid, String cursid) throws Exception
   {
      File file = curFile(sessid,cursid);
      if (!file.exists()) return(null);
      return(new CursorInfo(file));
   }


   public static boolean removeCursor(String sessid, String cursid) throws Exception
   {
      File file = curFile(sessid,cursid);
      if (!file.exists()) return(false);
      file.delete();
      return(true);
   }


   public static boolean updateCursor(String sessid, String cursid, long pos, int pgz) throws Exception
   {
      File file = curFile(sessid,cursid);
      if (!file.exists()) return(false);
      CursorInfo.update(file,pos,pgz);
      return(true);
   }


   private static File pidFile(String inst)
   {
      return(new File(Config.path(STATE,INSTANCES,inst+"."+PID)));
   }


   private static File curFile(String session, String cursor)
   {
      return(new File(Config.path(STATE,session,cursor+"."+CUR)));
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


   public static void cleanout(long now, int timeout)
   {
      File root = new File(StatePersistency.path);

      if (root.exists())
      {
         for(File file : root.listFiles())
         {
            if (!file.isDirectory())
               continue;

            if (file.getName().equals(INSTANCES))
               continue;

            if (State.hasSession(file.getName()))
               continue;

            File session = sesFile(file.getName());

            if (now - session.lastModified() > timeout)
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
      public final long pid;
      public final String guid;
      public final String inst;
      public final String user;
      public final boolean owner;
      public final boolean online;
      public final boolean stateful;

      private SessionInfo(long pid, String guid, String inst, String user, boolean stateful)
      {
         this.age = 0;
         this.pid = pid;
         this.guid = guid;
         this.inst = inst;
         this.user = user;
         this.owner = true;
         this.online = true;
         this.stateful = stateful;
      }

      private SessionInfo(File file, String inst) throws Exception
      {
         String guid = file.getName();
         long now = (new Date()).getTime();

         this.age = (int) (now - file.lastModified())/1000;
         this.guid = guid.substring(0,guid.length()-SES.length()-1);

         FileInputStream in = new FileInputStream(file);
         byte[] bytes = in.readAllBytes();
         in.close();

         int ilen = bytes[0];
         this.stateful = bytes[1] == '1';
         int ulen = bytes.length - 2 - 8 - ilen;

         this.pid = Bytes.getLong(bytes,2);
         this.inst = new String(bytes,2+8,ilen);
         this.user = new String(bytes,2+8+ilen,ulen);

         long pid = StatePersistency.getPid(this.inst);

         if (inst == null) this.owner = true;
         else this.owner = inst.equals(this.inst);
         this.online = pid >= 0 && pid == this.pid;
      }


      public void save(File file) throws Exception
      {
         byte[] user = this.user.getBytes();
         byte[] inst = this.inst.getBytes();
         byte[] bpid = Bytes.getBytes(this.pid);

         byte ilen = (byte) inst.length;
         byte ulen = (byte) user.length;

         byte[] bytes = new byte[2+bpid.length+inst.length+user.length];

         bytes[0] = ilen;
         bytes[1] = (byte) (this.stateful ? '1' : '0');

         System.arraycopy(bpid,0,bytes,2,bpid.length);
         System.arraycopy(inst,0,bytes,2+bpid.length,ilen);
         System.arraycopy(user,0,bytes,2+bpid.length+ilen,ulen);

         FileOutputStream out = new FileOutputStream(file);
         out.write(bytes);
         out.close();
      }
   }


   public static class CursorInfo
   {
      public final int pgz;
      public final long pos;
      public final String guid;
      public final JSONObject json;

      private static void update(File file, long pos, int pgz) throws Exception
      {
         byte[] bpos = Bytes.getBytes(pos);
         byte[] bpgz = Bytes.getBytes(pgz);

         byte[] bytes = new byte[bpos.length+bpgz.length];

         System.arraycopy(bpos,0,bytes,0,bpos.length);
         System.arraycopy(bpgz,0,bytes,8,bpgz.length);

         RandomAccessFile raf = new RandomAccessFile(file,"rw");
         raf.write(bytes); raf.close();
      }

      private CursorInfo(String guid, long pos, int pgz, JSONObject json)
      {
         this.pos = pos;
         this.pgz = pgz;
         this.json = json;
         this.guid = guid;
      }

      private CursorInfo(File file) throws Exception
      {
         String guid = file.getName();
         this.guid = guid.substring(0,guid.length()-CUR.length()-1);

         FileInputStream in = new FileInputStream(file);
         byte[] bytes = in.readAllBytes();
         in.close();

         this.pos = Bytes.getLong(bytes,0);
         this.pgz = Bytes.getInt(bytes,8);

         String json = new String(bytes,12,bytes.length-12);
         this.json = new JSONObject(json);
      }

      public void save(File file) throws Exception
      {
         byte[] pos = Bytes.getBytes(this.pos);
         byte[] pgz = Bytes.getBytes(this.pgz);
         byte[] def = this.json.toString().getBytes();

         byte[] bytes = new byte[8+4+def.length];

         System.arraycopy(pos,0,bytes,0,pos.length);
         System.arraycopy(pgz,0,bytes,pos.length,pgz.length);
         System.arraycopy(def,0,bytes,pos.length+pgz.length,def.length);

         FileOutputStream out = new FileOutputStream(file);
         out.write(bytes);
         out.close();
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