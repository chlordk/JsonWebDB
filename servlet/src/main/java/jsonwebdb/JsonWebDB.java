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

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class JsonWebDB extends HttpServlet
{
  private Pool pool = null;
  private String config = null;


  public void init() throws ServletException
  {
    pool = new Pool();
    config = getInitParameter("JsonWebDB");
  }


  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String user = null;
    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();
    try {
      user = pool.authenticate("pvuser","pvuser");
    } catch (Exception e) {
      throw new AnyException(e);
    }
    pw.println("<html><body>");
    pw.println("<b>JsonWebDB 4 "+user+"</b>");
    pw.println("</body></html>");
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