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

import java.util.logging.Level;
import jsondb.state.StateHandler;


public class Monitor extends Thread
{
   private static final int MAXINT = 60000;


   public static void initialize() throws Exception
   {
      (new Monitor()).start();
      StateHandler.initialize();
   }

   private Monitor()
   {
      this.setDaemon(true);
      this.setName(this.getClass().getName());
   }

   public void run()
   {
      int idle = Config.idle() * 1000;
      int timeout = Config.timeout() * 1000;

      int interval = Math.min(idle,timeout);
      interval = timeout > MAXINT*2 ? MAXINT : (int) (3.0/4*timeout);

      Config.logger().info(this.getClass().getSimpleName()+" running every "+interval/1000+" secs");

      while (true)
      {
         try
         {
            Thread.sleep(interval);
            reap();
         }
         catch (Throwable t)
         {
            Config.logger().log(Level.SEVERE,t.toString(),t);
         }
      }
   }

   private void reap() throws Exception
   {
      for(Session session : Session.getAll())
      {
         Config.logger().info(session.toString());
      }
   }
}
