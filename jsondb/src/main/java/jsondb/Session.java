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

package jsondb;

import java.util.Date;
import java.util.ArrayList;
import jsondb.state.StateHandler;
import database.definitions.AdvancedPool;
import database.definitions.JdbcInterface;
import java.util.concurrent.ConcurrentHashMap;


public class Session
{
   private final String guid;
   private final String user;
   private final boolean dedicated;
   private final AdvancedPool pool;

   private Date touched = null;
   private JdbcInterface rconn = null;
   private JdbcInterface wconn = null;


   private final static ConcurrentHashMap<String,Session> sessions =
      new ConcurrentHashMap<String,Session>();


   public static ArrayList<Session> getAll()
   {
      ArrayList<Session> sessions = new ArrayList<Session>();
      sessions.addAll(Session.sessions.values());
      return(sessions);
   }

   public static Session get(String guid)
   {
      Session session = sessions.get(guid);
      return(session);
   }

   public static Session create(String user, boolean dedicated) throws Exception
   {
      String guid = StateHandler.createSession(user,dedicated);

      Session session = new Session(guid,user,dedicated);
      sessions.put(guid,session);

      return(session);
   }

   public static boolean remove(Session session)
   {
      return(remove(session.guid));
   }

   public static boolean remove(String guid)
   {
      return(sessions.remove(guid) != null);
   }

   private Session(String guid, String user, boolean dedicated) throws Exception
   {
      this.guid = guid;
      this.user = user;
      this.pool = Config.pool();
      this.touched = new Date();
      this.dedicated = dedicated;
   }

   public String getGuid()
   {
      return(guid);
   }

   public String getUser()
   {
      return(user);
   }

   public Date getTouched()
   {
      return(touched);
   }

   public boolean isDedicated()
   {
      return(dedicated);
   }

   public boolean touch() throws Exception
   {
      this.touched = new Date();
      boolean success = StateHandler.touchSession(guid);
      return(success);
   }

   public Session connect() throws Exception
   {
      touch();

      if (wconn == null)
         wconn = JdbcInterface.getInstance(true);

      if (pool.secondary())
      {
         if (rconn == null)
            rconn = JdbcInterface.getInstance(false);
      }

      return(this);
   }


   public boolean disconnect() throws Exception
   {
      if (wconn != null)
      {
         wconn.disconnect();
         wconn = null;
      }

      if (rconn != null)
      {
         rconn.disconnect();
         rconn = null;
      }

      boolean success = StateHandler.removeSession(guid);
      sessions.remove(guid);

      return(success);
   }

   public boolean authenticate(String username, String password) throws Exception
   {
      if (username == null) return(false);
      if (password == null) return(false);
      return(pool.authenticate(username,password));
   }

   public boolean commit() throws Exception
   {
      boolean success = StateHandler.touchSession(guid);

      if (wconn != null)
      {
         wconn.commit();
         return(success);
      }

      return(false);
   }

   public boolean rollback() throws Exception
   {
      boolean success = StateHandler.touchSession(guid);

      if (wconn != null)
      {
         wconn.rollback();
         return(success);
      }

      return(false);
   }

   public String toString()
   {
      boolean connected = false;

      if (wconn != null && wconn.isConnected()) connected = true;
      if (rconn != null && rconn.isConnected()) connected = true;

      int age = (int) ((new Date()).getTime() - touched.getTime())/1000;

      return(this.guid+", age: "+age+"secs, conn: "+connected);
   }
}
