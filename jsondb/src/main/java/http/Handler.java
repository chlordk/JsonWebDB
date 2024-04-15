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
import jsondb.files.FileResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;


public class Handler implements HttpHandler
{
   @Override
   public void handle(HttpExchange exchange) throws IOException
   {
      String meth = exchange.getRequestMethod();

      if (meth.equals("GET"))
      {
         doGet(exchange);
         return;
      }

      if (meth.equals("POST"))
      {
         doPost(exchange);
         return;
      }

      /*
      InputStream in = exchange.getRequestBody();
      OutputStream out = exchange.getResponseBody();

      exchange.sendResponseHeaders(200,5);
      out.write("Hello".getBytes());
      out.flush();
      out.close();
      */
   }


   public void doPost(HttpExchange exchange) throws IOException
   {
      JsonDB jsondb = new JsonDB();

      InputStream in = exchange.getRequestBody();
      OutputStream out = exchange.getResponseBody();
      String request = new String(in.readAllBytes());

      try
      {
         JSONObject response = jsondb.execute(request);
         byte[] content = response.toString(2).getBytes();

         exchange.sendResponseHeaders(200,content.length);
         out.write(content);

         out.flush();
         out.close();
      }
      catch (Throwable t)
      {
        throw new IOException(t);
      }
   }


   public void doGet(HttpExchange exchange) throws IOException
   {
      JsonDB jsondb = new JsonDB();
      OutputStream out = exchange.getResponseBody();

      String path = exchange.getRequestURI().getPath();
      if (path.length() <= 2) path = "/index.html";

      try
      {
         FileResponse file = jsondb.get(path);

         if (file.gzip)
            exchange.getResponseHeaders().set("Content-Encoding","gzip");

         exchange.sendResponseHeaders(200,file.size);
         out.write(file.content);

         out.flush();
         out.close();
      }
      catch (Throwable t)
      {
        throw new IOException(t);
      }
   }
}