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
import java.io.FileInputStream;

import jsondb.Config;
import java.util.UUID;
import java.io.FileOutputStream;


public class StateHandler
{
   private static String inst = null;
   private static String conn = null;
   private static String curs = null;

   private static final String STATE = "state";
   private static final String CURSORS = "cursors";
   private static final String CONNECTIONS = "connections";

   public static void main(String[] args) throws Exception
   {
      Config config = Config.load(args[0],args[1]);
      prepare(config); /* GGG */
      String conn = createConnection("hr");
      getConnection(conn);
   }


   public static void prepare(Config config) throws Exception
   {
      StateHandler.inst = config.inst();
      StateHandler.curs = Config.path(STATE,inst,CURSORS);
      StateHandler.conn = Config.path(STATE,inst,CONNECTIONS);

      clearAll(conn);
      clearAll(curs);

      (new File(curs)).mkdirs();
      (new File(conn)).mkdirs();
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


   private static void clearAll(String path)
   {
      File root = new File(path);

      if (root.exists())
      {
         for(File file : root.listFiles())
            file.delete();
      }
   }


   private static String connpath(String guid)
   {
      return(StateHandler.conn+File.separator+guid);
   }


   private static String curspath(String guid)
   {
      return(StateHandler.curs+File.separator+guid);
   }
}
