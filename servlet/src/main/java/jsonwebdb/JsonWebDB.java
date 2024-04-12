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

package jsonwebdb;

import jsondb.Config;
import jsondb.JsonDB;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import jsondb.files.FileResponse;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;


public class JsonWebDB extends HttpServlet
{
  private Config config = null;
  private Logger logger = null;

  public void init() throws ServletException
  {
    try {start();}
    catch (Exception e) {throw new ServletException(e);}
  }

  public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    try {jsonwebdb(request,response);}
    catch (Exception e) {throw new ServletException(e);}
  }


  private void start() throws Exception
  {
    String home = System.getenv("JsonWebDB_Home");
    String inst = System.getenv("JsonWebDB_Inst");

    config = Config.load(home,inst);
    logger = config.logger();

    Pool pool = new Pool(config);

    JsonDB.register(pool);
    JsonDB.register(config);

    JsonDB.start();
  }


  private void jsonwebdb(HttpServletRequest request, HttpServletResponse response) throws Exception
  {
    JsonDB jsondb = new JsonDB();
    String meth = request.getMethod();

    if (meth.equals("GET"))
    {
      FileResponse file = null;
      String path = getPath(request);

      file = jsondb.get(path);
      logger.info(file.toString());

      response.setContentType(file.mimetype);
      if (file.gzip) response.setHeader("Content-Encoding","gzip");

      OutputStream out = response.getOutputStream();
      out.write(file.content);
      out.close();
      return;
    }

    if (meth.equals("POST"))
    {
      String body = getBody(request);
      JSONObject json = jsondb.execute(body);
      logger.info(json.toString());

      response.setContentType(jsondb.mimetype("json"));
      OutputStream out = response.getOutputStream();
      out.write(json.toString().getBytes());
      out.close();
      return;
    }

    throw new ServletException("Method '"+meth+"'' not supported");
  }


  private String getPath(HttpServletRequest request)
  {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.length() == 1) path += "index.html";
    return(path);
  }


  private String getBody(HttpServletRequest request) throws IOException
  {
    int read = 0;
    byte[] buf = new byte[4196];

    InputStream in = request.getInputStream();
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    while (read >= 0)
    {
      read = in.read(buf);
      if (read > 0) out.write(buf,0,read);
    }

    in.close();
    out.close();

    return(out.toString());
  }
}