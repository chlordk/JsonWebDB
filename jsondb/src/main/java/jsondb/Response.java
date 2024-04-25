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

package jsondb;

import org.json.JSONObject;
import jsondb.messages.Messages;


public class Response
{
   private Throwable error;
   private JSONObject payload;

   public Response()
   {
   }

   public Response(JSONObject response)
   {
      this.payload = response;
   }

   public Object get(String name)
   {
      ensure();
      return(payload.get(name));
   }

   public JSONObject put(String name, Object value)
   {
      ensure();
      return(payload.put(name,value));
   }

   public JSONObject payload()
   {
      ensure();
      return(payload);
   }

   public Response payload(JSONObject payload)
   {
      this.payload = payload;
      return(this);
   }

   public Exception exception()
   {
      return((Exception) error);
   }

   public Response exception(Exception stacktrc)
   {
      this.error = stacktrc;
      return(this);
   }

   public Response exception(Throwable stacktrc)
   {
      this.error = stacktrc;
      return(this);
   }

   public String toString()
   {
      ensure();
      return(payload.toString());
   }

   public String toString(int indent)
   {
      ensure();
      return(payload.toString(indent));
   }

   private void ensure()
   {
      if (payload == null)
         payload = new JSONObject();

      if (error != null)
      {
         String message = error.toString();

         if (message == null || message.trim().length() == 0)
            message = Messages.get("EXCEPTION_WITHOUT_MESSAGE");

         payload.put("success",false);
         payload.put("message",message);
      }
   }
}