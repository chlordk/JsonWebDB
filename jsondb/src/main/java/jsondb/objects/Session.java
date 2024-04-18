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

package jsondb.objects;

import jsondb.Config;
import jsondb.Response;
import jsondb.Trusted;

import org.json.JSONObject;
import jsondb.messages.Messages;
import database.definitions.JdbcInterface;


public class Session implements DatabaseRequest
{
   private final JSONObject definition;

   private static final String SESSION = "session";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String SIGNATURE = "signature";
   private static final String DEDICATED = "dedicated";
   private static final String DATASECTION = "connection-data";


   public Session(JSONObject definition) throws Exception
   {
      this.definition = definition;
   }


   public Response connect() throws Exception
   {
      String password = null;
      String signature = null;
      boolean dedicated = false;

      JSONObject response = new JSONObject();
      response.put("method","connect");

      String username = Config.pool().defaultuser();

      JSONObject data = definition.getJSONObject(DATASECTION);

      if (data.has(USERNAME))
         username = data.getString(USERNAME);

      if (data.has(PASSWORD))
         password = data.getString(PASSWORD);

      if (data.has(DEDICATED))
         dedicated = data.getBoolean(DEDICATED);

      if (data.has(SIGNATURE))
         signature = data.getString(SIGNATURE);

      if (password != null) data.put(PASSWORD,"********");
      if (signature != null) data.put(SIGNATURE,"********");

      try
      {
         boolean authenticated = false;
         JdbcInterface db = JdbcInterface.getInstance(false);

         if (Trusted.getEntity(signature) != null) authenticated = true;
         else if (db.authenticate(username,password)) authenticated = true;

         if (authenticated)
         {
            String session = jsondb.Session.create(username,dedicated).getGuid();

            response.put("success",true);
            response.put("session",session);

            return(new Response(response));
         }
         else
         {
            response.put("success",false);
            response.put("message",Messages.get("AUTHENTICATION_FAILED",username));
            return(new Response(response));
         }
      }
      catch (Exception e)
      {
         response.put("success",false);
         return(new Response(response).exception(e));
      }
   }


   public Response disconnect() throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("method","disconnect");

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      if (session.remove())
      {
         response.put("success",true);
         response.put("session",session);
      }
      else
      {
         response.put("success",false);
         response.put("message",Messages.get("DISCONNECT_FAILED",session));
      }

      return(new Response(response));
   }


   public Response keepalive() throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("method","keepalive");

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      if (session.touch())
      {
         response.put("success",true);
         response.put("session",session);
      }
      else
      {
         response.put("success",false);
         response.put("message",Messages.get("TOUCH_FAILED",session));
      }

      return(new Response(response));
   }
}
