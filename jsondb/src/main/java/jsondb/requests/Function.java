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
import jsondb.Session;
import jsondb.Response;
import database.SQLPart;
import utils.JSONOObject;
import java.util.HashMap;
import database.BindValue;
import org.json.JSONObject;


public class Function
{
   private final String sessid;
   private final String source;
   private final JSONObject definition;

   private static final String SOURCE = "source";
   private static final String SESSION = "session";
   private static final String EXECUTE = "execute";
   private static final String RETURNS = "returns";
   private static final String SAVEPOINT = "savepoint";


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
      sources.Function source = Utils.getSource(response,this.source);
      if (source == null) return(new Response(response));
      return(execute(session,source));
   }


   public Response execute(Session session, sources.Function source) throws Exception
   {
      JSONObject response = new JSONOObject();
      JSONObject args = Utils.getMethod(definition,"execute");
      HashMap<String,BindValue> values = Utils.getBindValues(args);

      boolean savepoint = Config.dbconfig().savepoint(source.update());
      if (args.has(SAVEPOINT)) savepoint = args.getBoolean(SAVEPOINT);

      SQLPart call = source.sql();

      for(BindValue bv : call.bindValues())
      {
         BindValue val = values.get(bv.name().toLowerCase());
         if (val != null) bv.value(val.value());
      }

      call.bindByValue();
      response = session.executeCall(call.snippet(),call.bindValues(),source.update(),savepoint);

      response.put("success",true);
      response.put("session",sessid);
      response.put("method","execute()");

      if (source.returns())
         response.put(RETURNS,call.bindValues().get(0).name());

      return(new Response(response));
   }
}