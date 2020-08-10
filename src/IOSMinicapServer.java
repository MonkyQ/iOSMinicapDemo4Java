import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class IOSMinicapServer {
    public static String initIOSMinicapRun() throws IOException {
        Process ps=Runtime.getRuntime().exec("lsof -i tcp:12345");
        List<String> mPortProcessStatic=loadStreamList(ps.getInputStream());
        String mPortProcess=null;
        for(int i=0;i<mPortProcessStatic.size();i++){
            if(mPortProcessStatic.get(i).contains(":italk")){// && mPortProcessStatic.get(i).contains("LISTEN")
                String mPortColInfo=mPortProcessStatic.get(i);
                //正则表达式 以非数字划分字符串
                String[] mPortColTmp=mPortColInfo.split("\\D+");
                mPortProcess= mPortColTmp[mPortColTmp.length-1];
                break;
            }
        }
        return mPortProcess;
    }

    public static List<String> loadStreamList(InputStream in) throws IOException {
        int ptr = 0;
        in = new BufferedInputStream(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        List<String> buffer = new ArrayList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            buffer.add( line);
        }
        return buffer;
    }
    public static void main(String[] args) {
//        initIOSMinicapRun();
    }
}
