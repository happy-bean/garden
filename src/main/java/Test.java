import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author wgt
 * @date 2018-04-18
 * @description
 **/
public class Test {
    private static String url = "http://127.0.0.1:9030/test/index.html";

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 10000; i++) {
            Te te = new Te(i);
            te.start();
        }
    }

    static class Te implements Runnable {

        private Thread t;
        private String threadName;

        Te(int name) {
            threadName = String.valueOf(name);
        }

        @Override
        public void run() {
            BufferedReader in = null;
            try {
                URL realUrl = new URL(url);
                // 打开和URL之间的连接
                URLConnection connection = realUrl.openConnection();
                // 设置通用的请求属性
                connection.setRequestProperty("accept", "*/*");
                connection.setRequestProperty("connection", "Keep-Alive");
                connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                // 建立实际的连接
                connection.connect();
                // 定义 BufferedReader输入流来读取URL的响应
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                return;
            } catch (Exception e) {
            }
            // 使用finally块来关闭输入流
            finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        public void start() {
            System.out.println("Starting " + threadName);
            if (t == null) {
                t = new Thread(this, threadName);
                t.start();
            }
        }
    }
}
