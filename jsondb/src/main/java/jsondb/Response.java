package jsondb;

import org.json.JSONObject;

import jsondb.messages.Messages;


public class Response
{
   private Exception error;
   private JSONObject payload;

   public Response()
   {
   }

   public Response(JSONObject response)
   {
      this.payload = response;
   }

   public JSONObject payload()
   {
      return(payload);
   }

   public Response payload(JSONObject payload)
   {
      this.payload = payload;
      return(this);
   }

   public Exception exception()
   {
      return(error);
   }

   public Response exception(Exception stacktrc)
   {
      this.error = stacktrc;
      return(this);
   }

   public String toString()
   {
      if (payload != null)
      return(payload.toString());
      else return(Messages.get("EMPTY_RESPONSE"));
   }

   public String toString(int indent)
   {
      return(payload.toString(indent));
   }
}
