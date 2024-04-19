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

package http;

import java.util.Base64;
import java.util.logging.Level;

import jsondb.Config;
import jsondb.messages.Messages;


public class Admin
{
   public static AdminResponse noSSLMessage()
   {
      String message = null;
      AdminResponse response = null;

      message = Messages.get("SSL_REQUIRED");
      response = new AdminResponse(message);

      return(response);
   }


   public static AdminResponse getBasicAuthMessage()
   {
      AdminResponse response = new AdminResponse(401,"");
      response.setHeader("WWW-Authenticate","Basic realm='JsonWebDB'");
      return(response);
   }


   public static boolean isAdminUser(String header)
   {
      if (header == null) return(false);

      if (header.toLowerCase().startsWith("basic "))
         header = header.substring(6);

      String auth = new String(Base64.getDecoder().decode(header));
      int pos = auth.indexOf(':');

      String usr = auth.substring(0,pos);
      String pwd = auth.substring(pos+1);

      if (usr.equals(Options.username()) && pwd.equals(Options.password()))
         return(true);

      return(false);
   }


   public static AdminResponse process(String path)
   {
      AdminResponse response = null;

      if (path.equals(Options.admin()+"/stop"))
      {
         response = new AdminResponse(200,"stopping");
         (new DelayedStop()).start();
         return(response);
      }

      response = new AdminResponse(200,"status");
      return(response);
   }


   private static class DelayedStop extends Thread
   {
      DelayedStop()
      {
         this.setDaemon(true);
         this.setName("Stopping");
      }

      public void run()
      {
         try
         {
            Thread.sleep(5000);
            Config.logger().info("Shutting down");
            System.exit(0);
         }
         catch(Exception e)
         {
            Config.logger().log(Level.SEVERE,"Unable to stop process",e);
         }
      }
   }
}
