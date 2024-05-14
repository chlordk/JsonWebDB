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

import state.State;
import java.util.Date;
import database.Cursor;
import messages.Messages;
import database.BindValue;
import java.util.ArrayList;
import database.JdbcInterface;
import state.StatePersistency;
import java.util.logging.Level;
import database.definitions.AdvancedPool;
import state.StatePersistency.SessionInfo;
import state.StatePersistency.TransactionInfo;


public class Session
{
   private String inst;

   private final String guid;
   private final String user;

   private final boolean forward;
   private final boolean stateful;

   private final AdvancedPool pool;
   private final Object SYNC = new Object();

   private int clients = 0;
   private Date used = null;
   private Date trxused = null;
   private Date connused = null;

   private JdbcInterface rconn = null;
   private JdbcInterface wconn = null;


   public static Session get(String guid) throws Exception
   {
      Session session = State.getSession(guid);
      SessionInfo info = StatePersistency.getSession(guid);

      if (info != null && session == null)
      {
         session = new Session(info);

         if (info.online && !info.owner)
            Config.logger().info(Messages.get("NOT_SESSION_OWNER",guid,info.inst));

         if (!info.online)
         {
            Config.logger().info(Messages.get("SESSION_REINSTATED",guid));
            session.transfer();
         }

         State.addSession(session);
      }

      return(session);
   }


   public static Session create(String user, boolean stateful) throws Exception
   {
      String guid = StatePersistency.createSession(user,stateful);
      Session session = new Session(guid,user,stateful);
      State.addSession(session);
      return(session);
   }


   public static boolean remove(Session session)
   {
      if (State.removeSession(session.guid))
      {
         StatePersistency.removeSession(session.guid);
         return(true);
      }

      return(false);
   }


   private Session(SessionInfo info) throws Exception
   {
      this.guid = info.guid;
      this.user = info.user;
      this.inst = info.inst;

      this.stateful = info.stateful;

      this.used = new Date();
      this.pool = Config.pool();

      this.forward = info.online && !info.owner;
   }


   private Session(String guid, String user, boolean stateful) throws Exception
   {
      this.guid = guid;
      this.user = user;

      this.forward = false;
      this.stateful = stateful;

      this.used = new Date();
      this.inst = Config.inst();
      this.pool = Config.pool();
   }

   public Session up()
   {
      synchronized(SYNC)
      { clients++; }
      return(this);
   }

   public int clients()
   {
      synchronized(SYNC)
      {return(clients);}
   }

   public Session down()
   {
      synchronized(SYNC)
      { clients--; }
      return(this);
   }

   public String inst()
   {
      return(inst);
   }

   public String guid()
   {
      return(guid);
   }

   public String user()
   {
      return(user);
   }

   public boolean forward()
   {
      return(forward);
   }

   public boolean isStateful()
   {
      return(stateful);
   }


   public Date lastUsed()
   {
      synchronized(SYNC)
       {return(used);}
   }

   public Date lastUsedTrx()
   {
      synchronized(SYNC)
      {return(trxused);}
   }

   public Date lastUsedConn()
   {
      synchronized(SYNC)
      {return(connused);}
   }

   public synchronized void transfer() throws Exception
   {
      boolean move = this.inst.equals(Config.inst());

      if (!move) Config.logger().info(Messages.get("SESSION_REINSTATED",guid));
      else Config.logger().info(Messages.get("TRANSFER_SESSION",guid,this.inst));

      StatePersistency.transferSession(guid,this.user,this.stateful);
      this.inst = Config.inst();
   }

   public synchronized boolean touch() throws Exception
   {
      synchronized(SYNC) {this.used = new Date();}
      boolean success = StatePersistency.touchSession(guid);
      return(success);
   }


   public synchronized TransactionInfo touchTrx() throws Exception
   {
      this.trxused = new Date();
      TransactionInfo info = StatePersistency.touchTransaction(guid,trxused);
      return(info);
   }


   public synchronized Cursor getCursor(String cursid) throws Exception
   {
      Cursor cursor = State.getCursor(this,cursid);

      if (cursor == null)
      {
         cursor = Cursor.load(this,cursid);
         if (cursor == null) return(null);

         JdbcInterface read = ensure(false);
         read.executeQuery(cursor,false);

         State.addCursor(cursor);
         cursor.position();
      }

      synchronized(SYNC)
      {
         used = new Date();
         connused = new Date();
      }

      return(cursor);
   }


   public boolean isConnected()
   {
      synchronized(SYNC)
      {
         if (wconn != null && wconn.isConnected()) return(true);
         if (rconn != null && rconn.isConnected()) return(true);
         return(false);
      }
   }


   public synchronized boolean release(int idle) throws Exception
   {
      ArrayList<Cursor> cursors = State.getAllCursors(guid);

      if (!State.removeSession(guid))
      {
         Config.logger().warning(Messages.get("DISC_WITH_CLIENTS",guid,clients));
         return(false);
      }

      long used = lastUsed().getTime();
      long curr = (new Date()).getTime();

      if (this.trxused != null) return(false);
      if (curr - used < idle*1000) return(false);

      for (Cursor cursor : cursors)
         cursor.offline();

      if (wconn != null && wconn.isConnected())
      {
         try {wconn.disconnect();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }

      if (rconn != null && rconn.isConnected())
      {
         try {rconn.disconnect();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
      }

      StatePersistency.releaseSession(this.guid,this.user,this.stateful);
      return(true);
   }


   public synchronized boolean disconnect()
   {
      ArrayList<Cursor> cursors = State.getAllCursors(guid);

      if (!State.removeSession(guid))
      {
         Config.logger().warning(Messages.get("DISC_WITH_CLIENTS",guid,clients));
         return(false);
      }

      for(Cursor curs : cursors)
         curs.release();

      if (wconn != null)
      {
         try {wconn.disconnect();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
         wconn = null;
      }

      if (rconn != null)
      {
         try {rconn.disconnect();} catch (Exception e)
         {Config.logger().log(Level.SEVERE,e.toString(),e);}
         rconn = null;
      }

      boolean success = StatePersistency.removeSession(guid);

      return(success);
   }


   public synchronized boolean commit() throws Exception
   {
      StatePersistency.removeTransaction(this.guid);
      boolean success = StatePersistency.touchSession(guid);

      synchronized(SYNC)
      {
         trxused = null;
         used = new Date();
         connused = new Date();
      }

      if (wconn != null)
      {
         wconn.commit();
         return(success);
      }

      return(true);
   }


   public synchronized boolean rollback() throws Exception
   {
      StatePersistency.removeTransaction(this.guid);
      boolean success = StatePersistency.touchSession(guid);

      synchronized(SYNC)
      {
         trxused = null;
         used = new Date();
         connused = new Date();
      }

      if (wconn != null)
      {
         wconn.rollback();
         return(success);
      }

      return(true);
   }


   public synchronized Cursor executeQuery(String sql, ArrayList<BindValue> bindvalues, boolean savepoint, int pagesize) throws Exception
   {
      JdbcInterface read = ensure(false);

      for(BindValue bv : bindvalues)
         bv.validate();

      Cursor cursor = Cursor.create(this,sql,bindvalues,pagesize);

      long time = System.nanoTime();
      read.executeQuery(cursor,savepoint);
      cursor.excost(System.nanoTime()-time);

      synchronized(SYNC)
      {
         used = new Date();
         connused = new Date();
      }

      State.addCursor(cursor);
      return(cursor);
   }


   public boolean authenticate(String username, String password) throws Exception
   {
      if (username == null) return(false);
      if (password == null) return(false);
      return(pool.authenticate(username,password));
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

      if (write)  return(wconn.connect(this.user,write,stateful));
      else        return(rconn.connect(this.user,write,stateful));
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