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

import jsondb.Session;
import jsondb.Response;
import utils.JSONOObject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class Cursor
{
   private final String sessid;
   private final String cursid;
   private final JSONObject definition;

   private static final String FETCH = "fetch";
   private static final String CLOSE = "close";
   private static final String CURSOR = "cursor";
   private static final String SESSION = "session";
   private static final String PAGESIZE = "page-size";


   public Cursor(JSONObject definition)
   {
      this.definition = definition;
      cursid = definition.getString(CURSOR);
      sessid = definition.getString(SESSION);
   }


   public Response fetch() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,FETCH);
      if (session == null) return(new Response(response));

      Forward fw = Forward.redirect(session,"Cursor",definition);
      if (fw != null) return(new Response(fw.response()));

      try {return(fetch(session));}
        finally {session.down();}
   }


   public Response fetch(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();
      database.Cursor cursor = Utils.getCursor(response,session,cursid,"fetch()");
      if (cursor == null) return(new Response(response));
      return(fetch(session,cursor));
   }


   public Response fetch(Session session, database.Cursor cursor) throws Exception
   {
      JSONObject response = new JSONOObject();

      if (definition.has(FETCH))
      {
         JSONObject fetch = Utils.getMethod(definition,FETCH);
         if (fetch.has(PAGESIZE)) cursor.pagesize(fetch.getInt(PAGESIZE));
      }

      JSONArray rows = new JSONArray();
      ArrayList<Object[]> table = cursor.fetch();

      response.put("success",true);
      response.put("method","fetch()");
      response.put("more",cursor.next());

      if (cursor.primary())
         response.put("primary",true);

      response.put("rows",rows);
      for(Object[] row : table) rows.put(row);

      return(new Response(response));
   }


   public Response close() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,CLOSE);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Cursor",definition);
         if (fw != null) return(new Response(fw.response()));
         return(close(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response close(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();
      database.Cursor cursor = Utils.getCursor(response,session,cursid,"fetch()");
      if (cursor == null) return(new Response(response));
      return(close(session,cursor));
   }


   public Response close(Session session, database.Cursor cursor) throws Exception
   {
      JSONObject response = new JSONOObject();

      cursor.close();

      response.put("success",true);
      response.put("method","close()");

      return(new Response(response));
   }
}