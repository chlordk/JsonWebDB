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
import org.json.JSONObject;


public class Session implements DatabaseRequest
{
   private final JSONObject definition;

   private static final String SESSION = "session";
   private static final String USERNAME = "username";


   public Session(JSONObject definition) throws Exception
   {
      this.definition = definition;
   }


   public Response connect() throws Exception
   {
      JSONObject response = new JSONObject();
      String username = Config.pool().defaultuser();
      if (definition.has(USERNAME)) username = definition.getString(USERNAME);
      response.put("success",true);
      response.put("username",username);
      return(new Response(response));
   }


   public Response disconnect() throws Exception
   {
      JSONObject response = new JSONObject();
      String session = definition.optString(SESSION);
      response.put("success",true);
      response.put("session",session);
      return(new Response(response));
   }
}
