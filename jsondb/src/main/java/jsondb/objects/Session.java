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
import jsondb.Trusted;
import jsondb.Response;
import org.json.JSONObject;
import jsondb.messages.Messages;


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

      boolean authenticated = false;
      jsondb.Session session = jsondb.Session.create(username,dedicated);

      if (signature != null)
      {
         if (Trusted.getEntity(signature) != null)
         {
            authenticated = true;
            session.connect(false);
         }
      }

      else

      if (session.authenticate(username,password))
      {
         authenticated = true;
         session.connect(false);
      }

      if (authenticated)
      {
         response.put("success",true);
         response.put("session",session.getGuid());
      }
      else
      {
         response.put("success",false);
         response.put("message",Messages.get("AUTHENTICATION_FAILED",username));

         jsondb.Session.remove(session);
         return(new Response(response));
      }

      return(new Response(response));
   }


   public Response disconnect() throws Exception
   {
      JSONObject response = new JSONObject();
      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      response.put("session",sessid);
      response.put("method","disconnect");

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
         return(new Response(response));
      }

      if (!session.isDedicated())
      {
         response.put("success",false);
         response.put("message",Messages.get("SESSION_NOT_DEDICATED","disconnect",sessid));
         return(new Response(response));
      }

      if (!session.disconnect())
      {
         response.put("success",false);
         response.put("message",Messages.get("DISCONNECT_FAILED",sessid));
         return(new Response(response));
      }

      response.put("success",true);
      return(new Response(response));
   }


   public Response keepalive() throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("method","keepalive");

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      if (session != null && session.touch())
      {
         response.put("success",true);
         response.put("session",session.getGuid());
      }
      else
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
      }

      return(new Response(response));
   }


   public Response commit() throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("method","commit");

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      if (session != null && session.touch())
      {
         response.put("success",false);
         response.put("session",session);
         response.put("message",Messages.get("NO_SUCH_SESSION",session));
         return(new Response(response));
      }

      return(new Response(response));
   }


   public Response rollback() throws Exception
   {
      JSONObject response = new JSONObject();
      response.put("method","rollback");

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      if (session == null || !session.touch())
      {
         response.put("success",false);
         response.put("session",session);
         response.put("message",Messages.get("NO_SUCH_SESSION",session));
         return(new Response(response));
      }

      return(new Response(response));
   }
}
