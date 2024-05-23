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

package jsondb.requests;

import http.Client;
import jsondb.Config;
import jsondb.Session;
import utils.JSONOObject;
import org.json.JSONObject;
import state.StatePersistency;
import java.util.logging.Level;
import state.StatePersistency.ServerInfo;


public class Forward
{
   private final JSONObject response;

   public static Forward redirect(Session session, String object, JSONObject definition) throws Exception
   {
      if (session.forward())
      {
         JSONObject response = invoke(session,object,definition);
         if (response != null) return(new Forward(response));
      }

      return(null);
   }


   private static JSONObject invoke(Session session, String object, JSONObject definition) throws Exception
   {
      String request = "{\""+object+"\": "+definition.toString()+"}";
      ServerInfo info = StatePersistency.getServerInfo(session.inst());

      try
      {
         Client client = new Client(info.endp);
         byte[] bytes = client.post(request);

         session.removeForeign();
         return(new JSONOObject(new String(bytes)));
      }
      catch (Throwable t)
      {
         Config.logger().log(Level.WARNING,t.toString(),t);
         session.transfer();
      }

      return(null);
   }


   private Forward(JSONObject response)
   {
      this.response = response;
   }


   public JSONObject response()
   {
      return(response);
   }
}