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

import http.Server;
import jsondb.Config;
import jsondb.Session;
import database.Cursor;
import messages.Messages;
import java.util.HashSet;
import database.SQLTypes;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


public class State
{
   private static Object[] LOCKS;

   private static ConcurrentHashMap<String,Cursor> cursors =
      new ConcurrentHashMap<String,Cursor>();

   private static ConcurrentHashMap<String,Session> sessions =
      new ConcurrentHashMap<String,Session>();

   private static ConcurrentHashMap<String,HashSet<String>> cursesmap =
      new ConcurrentHashMap<String,HashSet<String>>();


   public static void main(String[] args) throws Exception
   {
      SQLTypes.initialize();
      Config.root(Server.findAppHome());
      JSONObject list = StatePersistency.list();
      System.out.println(list.toString(2));
   }


   public static void initialize()
   {
      int locks = 1024;
      LOCKS = new Object[locks];

      for (int i = 0; i < LOCKS.length; i++)
         LOCKS[i] = new Object();
   }


   public static void addSession(Session session)
   {
      synchronized(getLock(session))
      {sessions.put(session.guid(),session);}
   }

   public static boolean hasSession(String guid)
   {
      synchronized(getLock(guid))
      {return(sessions.get(guid) != null);}
   }

   public static Session getSession(String guid)
   {
      synchronized(getLock(guid))
      {
         Session session = sessions.get(guid);
         if (session != null) session.up();
         return(session);
      }
   }


   // Cannot remove session if open-cursors
   public static boolean removeSession(String guid)
   {
      synchronized(getLock(guid))
      {
         Session session = sessions.get(guid);
         if (session == null) return(true);

         synchronized(session)
         {
            HashSet<String> sescurs = cursesmap.get(guid);
            if (sescurs != null) return(false);
            sessions.remove(guid);
            return(true);
         }
      }
   }


   public static void addCursor(Cursor cursor)
   {
      synchronized(cursor.session())
      {
         cursors.put(cursor.guid(),cursor);

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


   public static Cursor getCursor(Session session, String guid)
   {
      synchronized(session)
      {return(cursors.get(guid));}
   }


   public static boolean removeCursor(Cursor cursor) throws Exception
   {
      synchronized(cursor.session())
      {
         cursors.remove(cursor.guid());

         String sessid = cursor.session().guid();
         HashSet<String> sescurs = cursesmap.get(sessid);

         if (sescurs == null)
            throw new Exception(Messages.get("CANNOT_REMOVE_CURSOR",cursor.guid()));

         sescurs.remove(cursor.guid());

         if (sescurs.size() == 0)
            cursesmap.remove(sessid);

         return(true);
      }
   }


   public static ArrayList<Cursor> getAllCursors(String sessid)
   {
      Session session = null;
      ArrayList<Cursor> cursors = new ArrayList<Cursor>();

      synchronized(getLock(sessid))
      {
         session = sessions.get(sessid);
         if (session == null) return(cursors);
      }

      synchronized(session)
      {
         HashSet<String> sescurs = cursesmap.get(sessid);

         for(String cursid : sescurs)
         {
            Cursor cursor = State.cursors.get(cursid);
            cursors.add(cursor);
         }
      }

      return(cursors);
   }


   public static Collection<Cursor> cursors()
   {
      return(cursors.values());
   }


   public static Collection<Session> sessions()
   {
      return(sessions.values());
   }


   private static Object getLock(Session session)
   {
      return(getSession(session.guid()));
   }


   private static Object getLock(String guid)
   {
      System.out.println(guid.hashCode() % LOCKS.length);
      return(LOCKS[guid.hashCode() % LOCKS.length]);
   }
}