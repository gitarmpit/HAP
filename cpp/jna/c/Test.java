import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.FloatByReference;
 
public class Test {
 
    public interface DllInterface extends Library {
        DllInterface INSTANCE = (DllInterface) Native.loadLibrary("test", DllInterface.class);
 
        int multiply(float a, float b, FloatByReference result);
 
        int sumArray(float[] a, int length, FloatByReference result);
 
        String getVersion ();
 
        int addValue (float a [], int length, float value);
 
    }
 
    public float multiply(float a, float b) {
        FloatByReference r = new FloatByReference (0);
 
        int rc = DllInterface.INSTANCE.multiply (a, b, r);
 
        return r.getValue ();
    }
 
    public float sumArray(float[] a) {
        FloatByReference r = new FloatByReference (0);
        int l = a.length;
 
        int rc = DllInterface.INSTANCE.sumArray (a, l, r);
 
        return r.getValue ();
    }
 
    public String getVersion () {
        String v = DllInterface.INSTANCE.getVersion ();
        return v;
    }
 
    public void addValue (float[] a, float v) {
        int l = a.length;
        DllInterface.INSTANCE.addValue (a, l, v);
    }
 
 
    /**
     * A test method
     */
    public static void main(String[] args) throws Exception {
 
        Test t = new Test();
 
        System.out.println("SimpleDll calling multiply ()...");
        float r = t.multiply(3.1f, 3f);
        System.out.println("SimpleDll r: " + r);
 
        System.out.println("SimpleDll calling sumArray ()...");
        float[] a = new float[]{1.1f, 2.2f, 3.3f};
        r = t.sumArray(a);
        System.out.println("SimpleDll r: " + r);
 
        System.out.println("SimpleDll calling getVersion ()...");
        String s = t.getVersion ();
        System.out.println("SimpleDll r: " + s);
 
        System.out.println("SimpleDll calling addValue ()...");
        a = new float[]{1.1f, 2.2f, 3.3f};
        t.addValue(a, 5.5f);
        String b = "a[]: ";
        for (int i = 0; i < a.length; i++) {
            b += a[i];
            b+= " ";
        }
        System.out.println("SimpleDll r: " + b);
 
    }
 
}