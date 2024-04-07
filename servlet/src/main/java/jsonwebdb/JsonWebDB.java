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
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;


public class JsonWebDB extends HttpServlet
{
  public void init() throws ServletException
  {
    try {Config.load(System.getenv("JsonWebDB"));}
    catch (Exception e) {throw new AnyException(e);}
  }

  public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String vers = "4.0";

    String path = getPath(request);
    String meth = request.getMethod();

    if (meth.equals("GET") && path.length() <= 0)
    {
      showBanner(response,vers);
      return;
    }

    if (meth.equals("GET"))
    {
      byte[] cont = file(path);
    }

    if (meth.equals("POST"))
    {
      String body = getBody(request);
    }

    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();

    pw.println("<html>");
    pw.println(Config.path());
    pw.println("</html>");
  }

  private byte[] file(String path)
  {
    return(null);
  }

  private JSONObject jsondb(String request)
  {
    return(null);
  }

  private String getPath(HttpServletRequest request)
  {
    return(request.getRequestURI().substring(request.getContextPath().length()));
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


  private void showFile(HttpServletResponse response, byte[] cont) throws IOException
  {
    OutputStream out = response.getOutputStream();
    out.write(cont);
    out.close();
  }

  private void showBanner(HttpServletResponse response, String version) throws IOException
  {
    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();
    pw.println("<html>");
    pw.println("<head><title>JsonWebDB version "+version+"</title></head>");
    pw.println("<body><b>JsonWebDB is running</b></body>");
    pw.println("</html>");
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