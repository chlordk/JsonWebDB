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
import jsondb.Session;
import database.Cursor;
import org.json.JSONObject;
import java.util.Collection;
import java.util.LinkedHashMap;


public class State
{
   private static LinkedHashMap<String,Cursor> cursors =
      new LinkedHashMap<String,Cursor>();

   private static LinkedHashMap<String,Session> sessions =
      new LinkedHashMap<String,Session>();


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
      }
   }


   public static Session getSession(String guid)
   {
      synchronized(sessions)
      {
         Session session = sessions.remove(guid);

         if (session != null)
         {
            session.inUse(true);
            sessions.put(guid,session);
         }
         return(session);
      }
   }


   public static void removeSession(String guid)
   {
      synchronized(sessions)
      {
         Session ses = sessions.get(guid);
         if (!ses.inUse()) sessions.remove(ses.guid());
      }
   }


   public static void addCursor(Cursor cursor)
   {
      synchronized(cursors)
      {
         cursor.inUse(true);
      }
   }


   public static Collection<Session> sessions()
   {
      synchronized(sessions)
      {
        return(sessions.values());
      }
   }
}