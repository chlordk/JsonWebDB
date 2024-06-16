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
import sources.TableSource;
import org.json.JSONObject;


public class Sql
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String INSERT = "insert";
   private static final String UPDATE = "update";
   private static final String DELETE = "delete";
   private static final String SELECT = "select";
   private static final String SESSION = "session";


   public Sql(JSONObject definition) throws Exception
   {
      this.definition = definition;
      source = definition.getString(SOURCE);
      sessid = definition.optString(SESSION);
   }


   public Response update() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,INSERT);
      if (session == null) return(new Response(response));

      try
      {
         Forward fw = Forward.redirect(session,"Table",definition);
         if (fw != null) return(new Response(fw.response()));
         return(update(session));
      }
      finally
      {
         session.down();
      }
   }


   public Response update(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();

      TableSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));

      return(update(session,source));
   }


   public Response update(Session session, TableSource source) throws Exception
   {
      return(null);
   }
}
