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

import jsondb.Config;
import jsondb.Admins;
import jsondb.Response;
import messages.Messages;
import utils.JSONOObject;
import org.json.JSONObject;


public class Session
{
   private final JSONObject definition;

   private static final String SESSION = "session";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String STATEFUL = "stateful";
   private static final String SIGNATURE = "signature";
   private static final String PARAMETERS = "connect()";


   public Session(JSONObject definition) throws Exception
   {
      this.definition = definition;
   }


   public Response connect() throws Exception
   {
      String password = null;
      String signature = null;
      boolean stateful = false;

      JSONObject response = new JSONOObject();

      String username = Config.pool().defaultuser();
      JSONObject data = definition.getJSONObject(PARAMETERS);

      if (data.has(USERNAME))
         username = data.getString(USERNAME);

      if (data.has(PASSWORD))
         password = data.getString(PASSWORD);

      if (data.has(STATEFUL))
         stateful = data.getBoolean(STATEFUL);

      if (data.has(SIGNATURE))
         signature = data.getString(SIGNATURE);

      if (password != null) data.put(PASSWORD,"********");
      if (signature != null) data.put(SIGNATURE,"********");

      boolean authenticated = false;
      jsondb.Session session = jsondb.Session.create(username,stateful);

      response.put("success",true);
      response.put("method","connect()");
      response.put("session",session.guid());

      if (signature != null)
      {
         if (Admins.getAdmin(signature) != null)
         {
            authenticated = true;
            session.connect();
         }
      }

      else

      if (session.authenticate(username,password))
      {
         authenticated = true;
         session.connect();
      }

      if (!authenticated)
      {
         jsondb.Session.remove(session);

         response.put("success",false);
         response.put("message",Messages.get("AUTHENTICATION_FAILED",username));
      }

      return(new Response(response));
   }


   public Response disconnect() throws Exception
   {
      JSONObject response = new JSONOObject();
      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      response.put("session",sessid);
      response.put("method","disconnect()");

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
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
      JSONObject response = new JSONOObject();

      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      response.put("success",true);
      response.put("session",sessid);
      response.put("method","keepalive()");

      if (session == null || !session.touch())
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
      }

      return(new Response(response));
   }


   public Response commit() throws Exception
   {
      JSONObject response = new JSONOObject();
      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      response.put("success",true);
      response.put("session",sessid);
      response.put("method","commit()");

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
         return(new Response(response));
      }

      if (!session.isStateful())
      {
         response.put("success",false);
         response.put("message",Messages.get("SESSION_NOT_STATEFUL","commit",sessid));
         return(new Response(response));
      }

      session.commit();
      return(new Response(response));
   }


   public Response rollback() throws Exception
   {
      JSONObject response = new JSONOObject();
      String sessid = definition.optString(SESSION);
      jsondb.Session session = jsondb.Session.get(sessid);

      response.put("success",true);
      response.put("session",sessid);
      response.put("method","rollback()");

      if (session == null)
      {
         response.put("success",false);
         response.put("message",Messages.get("NO_SUCH_SESSION",sessid));
         return(new Response(response));
      }

      if (!session.isStateful())
      {
         response.put("success",false);
         response.put("message",Messages.get("SESSION_NOT_STATEFUL","rollback",sessid));
         return(new Response(response));
      }

      session.rollback();
      return(new Response(response));
   }
}