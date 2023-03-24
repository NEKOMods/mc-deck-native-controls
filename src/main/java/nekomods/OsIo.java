package nekomods;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

public class OsIo {
    private static OsIoInterface INSTANCE = Native.load("c", OsIoInterface.class);

    public interface OsIoInterface extends Library {
        String getenv(String env);

        int open(String pathname, int flags);
        int read(int fd, byte[] data, NativeLong len);
        // TODO: we will probably need this later
        int ioctl(int fd, int cmd, byte[] p);
    }

    public static int open(String pathname, int flags) {
        return INSTANCE.open(pathname, flags);
    }

    public static int read(int fd, byte[] data, long len) {
        return INSTANCE.read(fd, data, new NativeLong(len));
    }
}
