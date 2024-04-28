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
import database.Cursor;
import state.StateHandler;
import database.BindValue;
import java.util.ArrayList;
import state.StateHandler.SessionInfo;
import database.definitions.AdvancedPool;
import database.definitions.JdbcInterface;
import state.StateHandler.TransactionInfo;
import java.util.concurrent.ConcurrentHashMap;


public class Session
{
   private final int idle;
   private final String guid;
   private final String user;
   private final boolean dedicated;
   private final AdvancedPool pool;

   private final ConcurrentHashMap<String,Cursor> cursors =
      new ConcurrentHashMap<String,Cursor>();

   private Date used = null;
   private Date trxused = null;
   private Date connused = null;

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

   public static Session get(String guid) throws Exception
   {
      Session session = sessions.get(guid);
      SessionInfo info = StateHandler.getSession(guid);

      if (info != null && session == null)
      {
         session = new Session(info.guid,info.user,info.dedicated);
         Config.logger().info("Move "+guid+" from "+info.inst+" to "+Config.inst());
      }

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
      this.used = new Date();
      this.pool = Config.pool();
      this.dedicated = dedicated;
      this.idle = Config.conTimeout();
   }

   public String getGuid()
   {
      return(guid);
   }

   public String getUser()
   {
      return(user);
   }

   public boolean isDedicated()
   {
      return(dedicated);
   }

   public synchronized Date lastUsed()
   {
      return(used);
   }

   public synchronized Date lastUsedTrx()
   {
      return(trxused);
   }

   public synchronized Date lastUsedConn()
   {
      return(connused);
   }

   public synchronized boolean touch() throws Exception
   {
      this.used = new Date();
      boolean success = StateHandler.touchSession(guid);
      return(success);
   }

   public synchronized boolean touchConn() throws Exception
   {
      this.connused = new Date();
      return(true);
   }

   public synchronized TransactionInfo touchTrx() throws Exception
   {
      this.trxused = new Date();
      TransactionInfo info = StateHandler.touchTransaction(guid,trxused);
      return(info);
   }

   public Cursor getCursor(String cursid) throws Exception
   {
      Cursor cursor = cursors.get(cursid);

      if (cursor != null) cursor.loadState();
      else cursor = Cursor.create(this,cursid);

      return(cursor);
  }

  public void removeCursor(Cursor cursor) throws Exception
  {
     cursors.remove(cursor.name());
  }

   public synchronized boolean isConnected()
   {
      if (wconn != null && wconn.isConnected()) return(true);
      if (rconn != null && rconn.isConnected()) return(true);
      return(false);
   }

   public synchronized boolean release() throws Exception
   {
      long used = lastUsed().getTime();
      long curr = (new Date()).getTime();

      if (this.trxused != null) return(false);
      if (curr - used < idle*1000) return(false);

      if (wconn != null && wconn.isConnected())
         wconn.disconnect();

      if (rconn != null && rconn.isConnected())
         rconn.disconnect();

      for(Cursor cursor : cursors.values())
         cursor.close();

      trxused = null;
      connused = null;

      return(true);
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

      cursors.clear();
      sessions.remove(guid);

      trxused = null;
      connused = null;

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
      StateHandler.removeTransaction(this.guid);
      boolean success = StateHandler.touchSession(guid);

      trxused = null;

      if (wconn != null)
      {
         wconn.commit();
         return(success);
      }

      return(true);
   }

   public boolean rollback() throws Exception
   {
      StateHandler.removeTransaction(this.guid);
      boolean success = StateHandler.touchSession(guid);

      trxused = null;

      if (wconn != null)
      {
         wconn.rollback();
         return(success);
      }

      return(true);
   }

   public Cursor executeQuery(String sql, ArrayList<BindValue> bindvalues, boolean savepoint) throws Exception
   {
      JdbcInterface read = ensure(false);
      Cursor cursor = new Cursor(this,sql,bindvalues);

      read.executeQuery(cursor,savepoint);
      cursors.put(cursor.name(),cursor);

      return(cursor);
   }

   private synchronized JdbcInterface ensure(boolean write) throws Exception
   {
      if (!write && !pool.secondary())
         write = true;

      if (write && wconn == null)
         wconn = JdbcInterface.getInstance(true);

      if (!write && rconn == null)
         rconn = JdbcInterface.getInstance(true);

      connused = new Date();

      if (write && wconn.isConnected())
         return(wconn);

      if (!write && rconn.isConnected())
         return(rconn);

      if (write)  return(wconn.connect(this.user,write,dedicated));
      else        return(rconn.connect(this.user,write,dedicated));
   }

   public String toString()
   {
      boolean connected = false;

      if (wconn != null && wconn.isConnected()) connected = true;
      if (rconn != null && rconn.isConnected()) connected = true;

      int age = (int) ((new Date()).getTime() - used.getTime())/1000;

      return(this.guid+", age: "+age+"secs, conn: "+connected);
   }
}