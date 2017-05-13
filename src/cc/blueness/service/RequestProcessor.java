package cc.blueness.service;

import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.BufferedReader;
import java.io.DataInputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;
import java.io.IOException;  
import java.io.InputStreamReader;  
import java.io.OutputStream;  
import java.io.OutputStreamWriter;  
import java.io.Reader;  
import java.io.Writer;  
import java.net.Socket;  
import java.util.Date;  
import java.util.List;  
import java.util.LinkedList;  
import java.util.StringTokenizer;  
  
public class RequestProcessor implements Runnable {  
  
    private static List pool=new LinkedList();  
    private File documentRootDirectory;  
    private String indexFileName="index.html";  
    private int contentLength = 0;
    private String boundary = null; 
    private String requestPath;
    
    public RequestProcessor(File documentRootDirectory,String indexFileName) {  
        if (documentRootDirectory.isFile()) {  
            throw new IllegalArgumentException();  
        }  
        this.documentRootDirectory=documentRootDirectory;  
        try {  
            this.documentRootDirectory=documentRootDirectory.getCanonicalFile();  
        } catch (IOException e) {  
        }  
          
        if (indexFileName!=null) {  
            this.indexFileName=indexFileName;  
        }
    }  
      
    public static void processRequest(Socket request) {  
        synchronized (pool) {  
            pool.add(pool.size(),request);  
            pool.notifyAll();  
        }  
    }  
      
    @Override  
    public void run() {
        //��ȫ�Լ��  
    	String root;
    	if(documentRootDirectory!=null){
    		root=documentRootDirectory.getPath();  
    	}
          
        while (true) {  
            Socket connection;  
            synchronized (pool) {  
                while (pool.isEmpty()) {  
                    try {  
                        pool.wait();  
                    } catch (InterruptedException e) {  
                    }  
                      
                }  
                connection=(Socket)pool.remove(0);  
            }  
              
            try {
            	DataInputStream reader = new DataInputStream((connection.getInputStream()));  
                String line = reader.readLine();  
                String method = line.substring(0, 4).trim();  
                OutputStream out = connection.getOutputStream();  
                this.requestPath = line.split(" ")[1];  
                System.out.println(method);  
                if ("GET".equalsIgnoreCase(method)) {  
                    System.out.println("do get......");  
                    this.doGet(reader, out ,requestPath);  
                } else if ("POST".equalsIgnoreCase(method)) {  
                    System.out.println("do post......");  
                    this.doPost(reader, out);  
                }  
                  
            } catch (Exception e) {
				e.printStackTrace();
			}finally{  
                try {  
                    connection.close();  
                } catch (IOException e2) {  
                }  
                  
            }  
        }  
    }  
    
  //����GET����  
    private void doGet(DataInputStream reader, OutputStream out,String requestPath) throws Exception {  
    	BufferedReader bd=new BufferedReader(new InputStreamReader(reader));
    	 /**
         * ����HTTP����
         */
        String requestHeader;
        while((requestHeader=bd.readLine())!=null&&!requestHeader.isEmpty()){
            System.out.println(requestHeader);
            /**
             * ���GET����
             */
                
        }
        int begin = requestPath.indexOf("/?")+2;
        String condition=requestPath.substring(begin, requestPath.length());
        System.out.println("GET�����ǣ�"+condition);
        
      //���ͻ������������  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        //response += "/n";  
        //String body = "<html><head><title>test server</title></head><body><p>post ok:</p>" + new String(buf, 0, size) + "</body></html>";  
        //System.out.println(body);  
        out.write(response.getBytes());  
        //out.write(body.getBytes());  
        out.flush();  
        reader.close();  
        out.close();  
        System.out.println("request complete.");  
    }  
  //����post����  
    private void doPost(DataInputStream reader, OutputStream out) throws Exception {
        String line = reader.readLine();  
        while (line != null) {
            System.out.println(line);  
            line = reader.readLine();  
            if ("".equals(line)) {  
                break;  
            } else if (line.indexOf("Content-Length") != -1) {  
                this.contentLength = Integer.parseInt(line.substring(line.indexOf("Content-Length") + 16));  
            }  
            //����Ҫ�ϴ������� ��ת��doMultiPart������  
            else if(line.indexOf("multipart/form-data")!= -1){  
                //��multiltipart�ķָ���  
                this.boundary = line.substring(line.indexOf("boundary") + 9);  
                this.doMultiPart(reader, out);  
                return;  
            }  
        }  
        //������ȡ��ͨpost��û�и������ύ������  
        System.out.println("begin reading posted data......");  
        String dataLine = null;  
        //�û����͵�post��������  
        byte[] buf = {};  
        int size = 0;  
        if (this.contentLength != 0) {
            buf = new byte[this.contentLength];  
            while(size<this.contentLength){  
                int c = reader.read();  
                buf[size++] = (byte)c;  
                  
            }  
            System.out.println("The data user posted: " + new String(buf, 0, size));  
        }  
        //���ͻ������������  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        response += "/n";  
        String body = "<html><head><title>test server</title></head><body><p>post ok:</p>" + new String(buf, 0, size) + "</body></html>";  
        System.out.println(body);  
        out.write(response.getBytes());  
        out.write(body.getBytes()); 
        out.flush();  
        reader.close();  
        out.close();  
        System.out.println("request complete.");  
    }  
    //������  
    private void doMultiPart(DataInputStream reader, OutputStream out) throws Exception {  
        System.out.println("doMultiPart ......");  
        String line = reader.readLine();  
        while (line != null) {  
            System.out.println(line);  
            line = reader.readLine();  
            if ("".equals(line)) {  
                break;  
            } else if (line.indexOf("Content-Length") != -1) {  
                this.contentLength = Integer.parseInt(line.substring(line.indexOf("Content-Length") + 16));  
                System.out.println("contentLength: " + this.contentLength);  
            } else if (line.indexOf("boundary") != -1) {  
                //��ȡmultipart�ָ���  
                this.boundary = line.substring(line.indexOf("boundary") + 9);  
            }  
        }  
        System.out.println("begin get data......");  
        /*�����ע����һ����������ʹ������������ȫ�ģ��������Ķ���˵���Ե�����***** 
        <HTTPͷ��������> 
        ............ 
        Cache-Control: no-cache 
        <������һ�����У����������������ݶ���Ҫ�ύ������> 
        -----------------------------7d925134501f6<����multipart�ָ���> 
        Content-Disposition: form-data; name="myfile"; filename="mywork.doc" 
        Content-Type: text/plain 
         
        <��������>........................................ 
        ................................................. 
         
        -----------------------------7d925134501f6<����multipart�ָ���> 
        Content-Disposition: form-data; name="myname"<�����ֶλ򸽼�> 
        <������һ������> 
        <�����ֶλ򸽼�������> 
        -----------------------------7d925134501f6--<����multipart�ָ��������һ���ָ���������-> 
        ****************************************************************/  
        /** 
         * �����ע����һ����������multipart���͵�POST��ȫ��ģ�ͣ� 
         * Ҫ�Ѹ���ȥ����������Ҫ�ҵ��������ĵ���ʼλ�úͽ���λ�� 
         * **/  
        if (this.contentLength != 0) {  
            //�����е��ύ�����ģ����������������ֶζ��ȶ���buf.  
            byte[] buf = new byte[this.contentLength];  
            int totalRead = 0;  
            int size = 0;  
            while (totalRead < this.contentLength) {  
                size = reader.read(buf, totalRead, this.contentLength - totalRead);  
                totalRead += size;  
            }  
            //��buf����һ���ַ������������ַ�������ļ�����������ڵ�λ��  
            String dataString = new String(buf, 0, totalRead);  
            System.out.println("the data user posted:/n" + dataString);  
            int pos = dataString.indexOf(boundary);  
            //�����Թ�4�о��ǵ�һ��������λ��  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            //������ʼλ��  
            int start = dataString.substring(0, pos).getBytes().length;  
            pos = dataString.indexOf(boundary, pos) - 4;  
            //��������λ��  
            int end = dataString.substring(0, pos).getBytes().length;  
            //�����ҳ�filename  
            int fileNameBegin = dataString.indexOf("filename") + 10;  
            int fileNameEnd = dataString.indexOf("/n", fileNameBegin);  
            String fileName = dataString.substring(fileNameBegin, fileNameEnd);  
            /** 
             * ��ʱ���ϴ����ļ���ʾ�������ļ���·��,����c:/my file/somedir/project.doc 
             * ����ʱ��ֻ��ʾ�ļ������֣�����myphoto.jpg. 
             * ������Ҫ��һ���жϡ� 
            */  
            if(fileName.lastIndexOf("//")!=-1){  
                fileName = fileName.substring(fileName.lastIndexOf("//") + 1);  
            }  
            fileName = fileName.substring(0, fileName.length()-2);  
            OutputStream fileOut = new FileOutputStream("c://" + fileName);  
            fileOut.write(buf, start, end-start);  
            fileOut.close();  
            fileOut.close();  
        }  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        response += "/n";  
        out.write("<html><head><title>test server</title></head><body><p>Post is ok</p></body></html>".getBytes());  
        out.flush();  
        reader.close();  
        System.out.println("request complete.");  
    }  
      
    public static String guessContentTypeFromName(String name) {  
        if (name.endsWith(".html")||name.endsWith(".htm")) {  
            return "text/html";  
        }else if (name.endsWith(".txt")||name.endsWith(".java")) {  
            return "text/plain";  
        }else if (name.endsWith(".gif")) {  
            return "image/gif";  
        }else if (name.endsWith(".class")) {  
            return "application/octet-stream";  
        }else if (name.endsWith(".jpg")||name.endsWith(".jpeg")) {  
            return "image/jpeg";  
        }else {  
            return "text/plain";  
        }  
    }  
  
}  
