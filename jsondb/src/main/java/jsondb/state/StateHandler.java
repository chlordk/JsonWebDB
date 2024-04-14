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
   private static Config conf = null;
   private static String inst = null;
   private static String conn = null;
   private static String curs = null;

   private static final int MAXINT = 60000;
   private static final String STATE = "state";
   private static final String CURSORS = "cursors";
   private static final String CONNECTIONS = "connections";

   public static void main(String[] args) throws Exception
   {
      Config config = Config.load(args[0],args[1]);
      handle(config);
      Thread.sleep(MAXINT);
   }


   public static void handle(Config config) throws Exception
   {
      StateHandler.conf = config;
      StateHandler.inst = config.inst();
      StateHandler.curs = Config.path(STATE,inst,CURSORS);
      StateHandler.conn = Config.path(STATE,inst,CONNECTIONS);

      (new File(curs)).mkdirs();
      (new File(conn)).mkdirs();

      (new StateHandler()).start();
   }

   public static synchronized String getConnection(String guid) throws Exception
   {
      int pos = guid.indexOf(':');
      guid = guid.substring(pos+1);
      File file = new File(connpath(guid));
      file.setLastModified(System.currentTimeMillis());
      FileInputStream in = new FileInputStream(file);
      String username = new String(in.readAllBytes());
      in.close(); return(username);
   }


   public static synchronized String createConnection(String username) throws Exception
   {
      String guid = null;
      boolean done = false;

      while (!done)
      {
         FileOutputStream fout = null;
         guid = UUID.randomUUID().toString();
         File conn = new File(connpath(guid));

         if (!conn.exists())
         {
            done = true;
            fout = new FileOutputStream(conn);
            fout.write(username.getBytes()); fout.close();
         }
      }

      return(inst+":"+guid);
   }


   private static String connpath(String guid)
   {
      return(StateHandler.conn+File.separator+guid);
   }


   private StateHandler()
   {
      this.setDaemon(true);
      this.setName(this.getClass().getName());
   }


   @Override
   public void run()
   {
      int sttl = conf.sesttl() * 1000;
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
            conf.logger().log(Level.SEVERE,t.getMessage(),t);
         }
      }
   }


   private void cleanout(int sttl)
   {
      long curr = System.currentTimeMillis();
      File root = new File(StateHandler.conn);

      if (root.exists())
      {
         for(File file : root.listFiles())
         {
            if (curr - file.lastModified() > sttl)
            {
               file.delete();
               deletecursors(file.getName());
            }
         }
      }
   }


   private void deletecursors(String conn)
   {
      File root = new File(StateHandler.curs);

      if (root.exists())
      {
         for(File file : root.listFiles())
         {
            String name = "";
            int pos = file.getName().lastIndexOf('.');
            if (pos > 0) name = file.getName().substring(0,pos);
            if (conn.equals(name)) file.delete();
         }
      }
   }
}
