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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


public class Cursor
{
   private final String sessid;
   private final String cursid;
   private final JSONObject definition;

   private static final String CURSOR = "cursor";
   private static final String SESSION = "session";


   public Cursor(JSONObject definition)
   {
      this.definition = definition;
      cursid = definition.getString(CURSOR);
      sessid = definition.getString(SESSION);
   }


   public Response fetch() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid);
      if (session == null) return(new Response(response));

      database.Cursor cursor = Utils.getCursor(response,session,cursid);
      if (cursor == null) return(new Response(response));

      JSONArray rows = new JSONArray();
      ArrayList<Object[]> table = cursor.fetch();

      response.put("success",true);

      if (cursor.next())
         response.put("more",true);

      response.put("rows",rows);
      for(Object[] row : table) rows.put(row);

      return(new Response(response));
   }


   public Response close() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid);
      if (session == null) return(new Response(response));

      database.Cursor cursor = Utils.getCursor(response,session,cursid);
      if (cursor == null) return(new Response(response));

      cursor.close();
      response.put("success",true);

      return(new Response(response));
   }
}
