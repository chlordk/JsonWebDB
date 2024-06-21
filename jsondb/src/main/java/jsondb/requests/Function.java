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
import sources.SQLSource;
import jsondb.Response;
import java.util.HashMap;
import utils.JSONOObject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import database.Variable;


public class Function
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String SESSION = "session";
   private static final String EXECUTE = "execute";


   public Function(JSONObject definition) throws Exception
   {
      this.definition = definition;
      this.source = definition.getString(SOURCE);
      this.sessid = definition.getString(SESSION);
   }

   public Response execute() throws Exception
   {
      JSONObject response = new JSONOObject();

      Session session = Utils.getSession(response,sessid,EXECUTE);
      if (session == null) return(new Response(response));

      Forward fw = Forward.redirect(session,"Call",definition);
      if (fw != null) return(new Response(fw.response()));

      try {return(execute(session));}
        finally {session.down();}
   }


   public Response execute(Session session) throws Exception
   {
      JSONObject response = new JSONOObject();
      SQLSource source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));
      return(execute(session,source));
   }


   public Response execute(Session session, SQLSource source) throws Exception
   {
      JSONObject response = new JSONOObject();

      return(new Response(response));
   }
}