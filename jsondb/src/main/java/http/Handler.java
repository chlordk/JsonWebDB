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

package http;

import jsondb.JsonDB;
import jsondb.Config;
import java.util.List;
import jsondb.Response;
import multipart.Multipart;
import files.FileResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import application.Application;
import java.util.logging.Level;
import http.AdminResponse.Header;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;


public class Handler implements HttpHandler
{
   private static String JSONType = getJsonType();

   private static String getJsonType()
   {
      String ct = Config.getMimeType(".json");
      if (ct == null) ct = "application/json";
      return(ct);
   }

   @Override
   public void handle(HttpExchange exchange) throws IOException
   {
      Application appl = null;

      try {appl = Config.application();}
      catch (Exception e) {throw new IOException(e);}

      try
      {
         if (appl != null && appl.intercept(exchange))
            return;
      }
      catch (Exception e)
      {
         throw new IOException(e);
      }

      String meth = exchange.getRequestMethod();

      if (meth.equals("GET"))
      {
         doGet(exchange);
         return;
      }

      if (meth.equals("POST"))
      {
         doPost(appl,exchange);
         return;
      }
   }


   public void doGet(HttpExchange exchange) throws IOException
   {
      JsonDB jsondb = new JsonDB();

      String path = exchange.getRequestURI().getPath();
      if (path.length() <= 2) path = HTTPConfig.index();

      if (path.startsWith(HTTPConfig.admin()))
      {
         admin(path,exchange);
         return;
      }

      String vpath = HTTPConfig.getVirtual(path);
      if (vpath != null) path = vpath;

      try
      {
         String lastmod = getRequestHeader(exchange,"If-modified-since");

         if (!jsondb.modified(path,lastmod))
         {
            exchange.sendResponseHeaders(304,-1);
            return;
         }

         FileResponse file = jsondb.get(path);

         if (!file.exists())
         {
            exchange.sendResponseHeaders(404,-1);
            return;
         }

         OutputStream out = exchange.getResponseBody();
         exchange.getResponseHeaders().set("Content-Type",file.mimetype);
         if (file.gzip) exchange.getResponseHeaders().set("Content-Encoding","gzip");
         exchange.getResponseHeaders().set("Last-Modified",file.gmt());
         exchange.sendResponseHeaders(200,file.size);
         out.write(file.content);
         out.close();
      }
      catch (Throwable t)
      {
         Config.logger().log(Level.SEVERE,t.toString(),t);
         throw new IOException(t);
      }
   }


   public void doPost(Application appl, HttpExchange exchange) throws IOException
   {
      JsonDB jsondb = new JsonDB();

      InputStream in = exchange.getRequestBody();
      OutputStream out = exchange.getResponseBody();

      String ctype = exchange.getRequestHeaders().getFirst("Content-Type");

      if (ctype.startsWith("multipart/form-data"))
      {
         if (appl != null)
         {
            byte[] content = in.readAllBytes(); in.close();
            Multipart upload = new Multipart(ctype,content);

            try {appl.upload(exchange,upload);}
               catch (Exception e) {throw new IOException(e);}
         }

         return;
      }

      String request = new String(in.readAllBytes());

      try
      {
         Response response = jsondb.execute(appl,request);
         byte[] content = response.toString(2).getBytes();

         exchange.getResponseHeaders().set("Content-Type",Handler.JSONType);
         exchange.sendResponseHeaders(200,content.length);

         out.write(content);

         in.close();
         out.flush();
         out.close();
      }
      catch (Throwable t)
      {
         Config.logger().log(Level.SEVERE,t.toString(),t);
         throw new IOException(t);
      }
   }


   private void admin(String path, HttpExchange exchange) throws IOException
   {
      try
      {
         OutputStream out = exchange.getResponseBody();

         if (HTTPConfig.adminSSLRequired() && !(exchange instanceof HttpsExchange))
         {
            AdminResponse response = Admin.noSSLMessage();
            exchange.sendResponseHeaders(response.code,response.size);
            out.write(response.page);
            out.close();
            return;
         }

         boolean auth = Admin.isAdminUser(getRequestHeader(exchange,"Authorization"));

         if (!auth)
         {
            AdminResponse response = Admin.getBasicAuthMessage();

            for (int i = 0; i < response.headers.size(); i++)
            {
               Header header = response.headers.get(i);
               exchange.getResponseHeaders().set(header.name,header.value);
            }

            exchange.sendResponseHeaders(response.code,response.size);
            out.close();

            return;
         }

         AdminResponse response = Admin.process(path);
         exchange.sendResponseHeaders(response.code,response.size);

         for (int i = 0; i < response.headers.size(); i++)
         {
            Header header = response.headers.get(i);
            exchange.getResponseHeaders().set(header.name,header.value);
         }

         out.write(response.page);
         out.close();
      }
      catch (Throwable t)
      {
         Config.logger().log(Level.SEVERE,t.toString(),t);
         throw new IOException(t);
      }
   }


   private String getRequestHeader(HttpExchange exchange, String name)
   {
      String value = null;
      List<String> values = exchange.getRequestHeaders().get(name);

      if (values != null)
      {
         for(String part : values)
         {
            if (value == null) value = part;
            else value += " " + part;
         }
      }

      return(value);
   }
}