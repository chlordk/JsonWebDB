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

import utils.Misc;
import http.Server;
import jsondb.Config;
import jsondb.Session;
import database.Cursor;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONObject;
import java.util.Collection;
import java.util.logging.Level;


public class State
{
   private static HashMap<String,Cursor> cursors =
      new HashMap<String,Cursor>();

   private static HashMap<String,Session> sessions =
      new HashMap<String,Session>();

   private static HashMap<String,HashSet<String>> cursesmap =
      new HashMap<String,HashSet<String>>();


   public static void main(String[] args) throws Exception
   {
      String root = Misc.url(Server.findAppHome(),"state");
      JSONObject list = StatePersistency.list(root);
      System.out.println(list.toString(2));
   }


   public static void addSession(Session session)
   {
      synchronized(sessions)
      {
         session.inUse(true);
         sessions.put(session.guid(),session);
         System.out.println("addSession "+session.guid());
      }
   }

   public static Session getSession(String guid)
   {
      synchronized(sessions)
      {
         Session session = sessions.get(guid);
         if (session != null) session.inUse(true);
         System.out.println("getSession "+session.guid());
         return(session);
      }
   }

   public static boolean removeSession(String guid)
   {
      synchronized(sessions)
      {
         Session session = sessions.get(guid);
         System.out.println("removeSession inuse: "+session.inUse()+" "+session.guid());

         if (session == null || session.inUse())
            return(false);

         sessions.remove(guid);
         cursesmap.remove(guid);

         return(true);
      }
   }

   public static void addCursor(Cursor cursor)
   {
      synchronized(cursors)
      {
         cursor.inUse(true);
         cursors.put(cursor.guid(),cursor);

         System.out.println("addCursor "+cursor.guid());

         String sessid = cursor.session().guid();
         HashSet<String> sescurs = cursesmap.get(sessid);

         if (sescurs == null)
         {
            sescurs = new HashSet<String>();
            cursesmap.put(cursor.session().guid(),sescurs);
         }

         sescurs.add(cursor.guid());
      }
   }

   public static Cursor getCursor(String guid)
   {
      synchronized(cursors)
      {
         Cursor cursor = cursors.get(guid);
         if (cursor != null) cursor.inUse(true);
         System.out.println("getCursor "+cursor.guid());
         return(cursor);
      }
   }

   public static boolean removeCursor(String guid)
   {
      synchronized(cursors)
      {
         Cursor cursor = cursors.get(guid);

         if (cursor == null || cursor.inUse())
            return(false);

         cursors.remove(guid);
         System.out.println("removeCursor "+cursor.guid());

         String sessid = cursor.session().guid();
         HashSet<String> sescurs = cursesmap.get(sessid);
         if (sescurs != null) sescurs.remove(guid);

         return(true);
      }
   }

   public static Cursor[] removeAllCursors(String sessid)
   {
      String[] type = new String[0];
      System.out.println("removeAllCursors "+sessid);

      HashSet<String> sescurs = cursesmap.remove(sessid);
      if (sescurs == null) return(new Cursor[0]);

      String[] cursids = sescurs.toArray(type);
      Cursor[] cursors = new Cursor[cursids.length];

      for (int i = 0; i < cursors.length; i++)
         cursors[i] = State.cursors.remove(cursids[i]);

      return(cursors);
   }

   public static Collection<Session> sessions()
   {
      System.out.println("getAllSessions");
      synchronized(sessions)
      {
        return(sessions.values());
      }
   }
}