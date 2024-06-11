/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package jsondb;

import java.util.Date;
import java.util.ArrayList;
import state.StatePersistency;
import java.util.logging.Level;


public class Monitor extends Thread
{
   private static int contmout = 0;
   private static int trxtmout = 0;
   private static int sestmout = 0;

   private static final int SLACK = 500;
   private static final int MAXINT = 60000;


   public static void monitor() throws Exception
   {
      (new Monitor()).start();
   }

   private Monitor()
   {
      contmout = Config.conTimeout() * 1000;
      trxtmout = Config.trxTimeout() * 1000;
      sestmout = Config.sesTimeout() * 1000;

      this.setDaemon(true);
      this.setName(this.getClass().getName());
   }

   public void run()
   {
      int interval = sestmout;

      if (contmout > 0 && contmout < interval) interval = contmout;
      if (trxtmout > 0 && trxtmout < interval) interval = trxtmout;

      interval = interval > MAXINT*2 ? MAXINT : (int) (3.0/4*interval);

      Config.logger().info(this.getClass().getSimpleName()+" running every "+interval/1000+" secs");

      while (true)
      {
         try
         {
            long now = (new Date()).getTime();
            StatePersistency.cleanout(now,sestmout);

            Thread.sleep(interval);
            cleanout(now,sestmout,contmout,trxtmout);
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.toString(),t);
         }
      }
   }

   private void cleanout(long now, int sestmout, int contmout, int trxtmout) throws Exception
   {
      for(Session session : state.State.sessions())
      {
         Date lastUsed = session.lastUsed();
         Date lastTrxUsed = session.lastUsedTrx();
         Date lastConnUsed = session.lastUsedConn();

         boolean trx = lastTrxUsed != null && trxtmout > 0;
         boolean con = session.isConnected() && contmout > 0;

         if (trx && (now - lastTrxUsed.getTime() > trxtmout))
         {
            Config.logger().info("rollback "+session.guid());
            session = Session.get(session.guid(),true);

            if (session != null)
            {
               try {session.rollback();}
               finally {session.down();}
            }
         }

         if (con && (now - lastConnUsed.getTime() > contmout))
         {
            Config.logger().info("release "+session.guid());
            session = Session.get(session.guid(),true);

            if (session != null)
            {
               try {session.release(contmout);}
                 finally {session.down();}
            }
         }

         if (now - lastUsed.getTime() > sestmout)
         {
            Config.logger().info("remove "+session.guid());
            session = Session.get(session.guid(),true);

            if (session != null)
            {
               try {session.disconnect();}
               finally {session.down();}
            }
         }
      }
   }


   public static class CloseAsap extends Thread
   {
      private static CloseAsap running = null;
      private static final int latency = Config.dbconfig().latency();

      private final static ArrayList<Session> sessions =
         new ArrayList<Session>();


      public static synchronized void done()
      {
         running = null;

         if (sessions.size() > 0)
            running = new CloseAsap();
      }


      public static synchronized void add(Session session)
      {
         if (contmout <= 0) return;
         if (session.isStateful()) return;

         sessions.add(session);
         if (running == null) running = new CloseAsap();
      }


      public static synchronized void remove(Session session)
      {
         sessions.remove(session);
      }


      @SuppressWarnings("unchecked")
      private static synchronized ArrayList<Session> getSessions()
      {
         return((ArrayList<Session>) sessions.clone());
      }


      private CloseAsap()
      {
         this.setName(this.getClass().getName());
         this.setDaemon(true); this.start();
      }


      public void run()
      {
         try
         {
            while (true)
            {
               sleep(latency+SLACK);
               ArrayList<Session> sessions = getSessions();

               if (sessions.size() == 0)
                  break;

               for(Session session : sessions)
                  session.releaseWrite();
            }
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.toString(),t);
         }

         done();
      }
   }
}