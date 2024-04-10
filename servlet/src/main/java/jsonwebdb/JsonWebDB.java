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
import jsondb.Response;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class JsonWebDB extends HttpServlet
{
  private Config config = null;
  private Logger logger = null;


  public void init() throws ServletException
  {
    String home = System.getenv("JsonWebDB_Home");
    String inst = System.getenv("JsonWebDB_Inst");

    try {config = Config.load(home,inst);}
    catch (Exception e) {throw new AnyException(e);}

    logger = config.logger();
    Pool pool = new Pool(config);

    JsonDB.register(pool);
    JsonDB.register(config);
  }

  public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String meth = request.getMethod();

    if (meth.equals("GET"))
    {
      Response file = null;
      JsonDB jsondb = new JsonDB();
      String path = getPath(request);
      OutputStream out = response.getOutputStream();

      try {file = jsondb.getFile(path,out);}
      catch (Exception e) {throw new AnyException(e);}

      logger.info(file.toString());
      response.setContentType(file.mimetype);
      out.close();

      return;
    }

    if (meth.equals("POST"))
    {
      String body = getBody(request);
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

  public static class AnyException extends ServletException
  {
    public AnyException(Exception e)
    {
      super(e.getMessage());
      this.setStackTrace(e.getStackTrace());
    }
  }
}